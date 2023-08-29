package com.jaskarth.ecotones.world.worldgen.gen;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.*;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.util.math.noise.PerlinNoiseSampler;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.noise.NoiseConfig;
import com.jaskarth.ecotones.util.BiomeCache;
import com.jaskarth.ecotones.util.ImprovedChunkRandom;
import com.jaskarth.ecotones.util.noise.OctaveNoiseSampler;
import com.jaskarth.ecotones.util.noise.OpenSimplexNoise;
import com.jaskarth.ecotones.world.worldgen.river.PlateSet;
import com.jaskarth.ecotones.world.worldgen.river.RiverWorker;
import com.jaskarth.ecotones.world.worldgen.river.graph.RiverNode;
import com.jaskarth.ecotones.world.storage.ChunkDataStorage;
import com.jaskarth.ecotones.world.storage.ChunkStorageView;
import com.jaskarth.ecotones.world.storage.StorageKeys;
import com.jaskarth.ecotones.world.worldgen.river.RiverData;
import com.jaskarth.ecotones.world.worldgen.surface.SlopedSurfaceBuilder;
import com.jaskarth.ecotones.world.worldgen.surface.system.ConfiguredSurfaceBuilder;
import com.jaskarth.ecotones.world.worldgen.surface.system.SurfaceBuilder;
import com.jaskarth.ecotones.world.worldgen.surface.system.SurfaceConfig;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public abstract class BaseEcotonesChunkGenerator extends ChunkGenerator {
    private static final boolean GENERATE_384 = true;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private final int verticalNoiseResolution;
    private final int horizontalNoiseResolution;
    private final int noiseSizeX;
    private final int noiseSizeY;
    private final int noiseSizeZ;
    protected final ChunkRandom random;
    protected final Random juRandom;
    private final OctavePerlinNoiseSampler lowerInterpolatedNoise;
    private final OctavePerlinNoiseSampler upperInterpolatedNoise;
    private final OctavePerlinNoiseSampler interpolationNoise;
    private final OctaveSimplexNoiseSampler surfaceDepthNoise;
    protected final BlockState defaultBlock;
    protected final BlockState defaultFluid;
    protected final ThreadLocal<BiomeCache> biomeCache;
    private final ThreadLocal<NoiseCache> noiseCache;
    private final RiverWorker riverWorker;
    private final OctaveNoiseSampler<OpenSimplexNoise> riverNoiseNoise;
    private final long seed;

    public BaseEcotonesChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource);
        this.seed = seed;
        this.verticalNoiseResolution = 8;
        this.horizontalNoiseResolution = 4;
        this.defaultBlock = Blocks.STONE.getDefaultState();
        this.defaultFluid = Blocks.WATER.getDefaultState();
        this.noiseSizeX = 16 / this.horizontalNoiseResolution;
        this.noiseSizeY = 384 / this.verticalNoiseResolution;
        this.noiseSizeZ = 16 / this.horizontalNoiseResolution;
        this.random = new ChunkRandom(new CheckedRandom(seed));
        this.juRandom = new Random(seed);
        this.lowerInterpolatedNoise = OctavePerlinNoiseSampler.createLegacy(this.random, IntStream.rangeClosed(-15, 0));
        this.upperInterpolatedNoise = OctavePerlinNoiseSampler.createLegacy(this.random, IntStream.rangeClosed(-15, 0));
        this.interpolationNoise = OctavePerlinNoiseSampler.createLegacy(this.random, IntStream.rangeClosed(-7, 0));
        this.surfaceDepthNoise = new OctaveSimplexNoiseSampler(this.random, ImmutableList.of(-3, -2, -1, 0));

        this.biomeCache = ThreadLocal.withInitial(() -> new BiomeCache(1024, biomeSource));
        this.noiseCache = ThreadLocal.withInitial(() -> new NoiseCache(128, this.noiseSizeY + 1));

        this.riverWorker = new RiverWorker(seed);

        this.riverNoiseNoise = new OctaveNoiseSampler<>(OpenSimplexNoise.class, this.juRandom, 4, 128, 0.15, 0.15);
    }

    public double sampleTerrainNoise(int x, int y, int z, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch) {
        // To generate it's terrain, Minecraft uses two different perlin noises.
        // It interpolates these two noises to create the final sample at a position.
        // However, the interpolation noise is not all that good and spends most of it's time at > 1 or < 0, rendering
        // one of the noises completely unnecessary in the process.
        // By taking advantage of that, we can reduce the sampling needed per block through the interpolation noise.

        // This controls both the frequency and amplitude of the noise.
        double frequency = 1.0;
        double interpolationValue = 0.0;

        // Calculate interpolation data to decide what noise to sample.
        for (int octave = 0; octave < 8; octave++) {
            double scaledVerticalScale = verticalStretch * frequency;
            double scaledY = y * scaledVerticalScale;

            interpolationValue += sampleOctave(this.interpolationNoise.getOctave(octave),
                    OctavePerlinNoiseSampler.maintainPrecision(x * horizontalStretch * frequency),
                    OctavePerlinNoiseSampler.maintainPrecision(scaledY),
                    OctavePerlinNoiseSampler.maintainPrecision(z * horizontalStretch * frequency), scaledVerticalScale, scaledY, frequency);

            frequency /= 2.0;
        }

        // This uses 16.0 while vanilla uses 10.0, to get better looking terrain.
        double clampedInterpolation = (interpolationValue / 32.0 + 1.0) / 2.0;

        if (clampedInterpolation >= 1) {
            // Sample only upper noise, as the lower noise will be interpolated out.
            frequency = 1.0;
            double noise = 0.0;
            for (int octave = 0; octave < 16; octave++) {
                double scaledVerticalScale = verticalScale * frequency;
                double scaledY = y * scaledVerticalScale;

                noise += sampleOctave(this.upperInterpolatedNoise.getOctave(octave),
                        OctavePerlinNoiseSampler.maintainPrecision(x * horizontalScale * frequency),
                        OctavePerlinNoiseSampler.maintainPrecision(scaledY),
                        OctavePerlinNoiseSampler.maintainPrecision(z * horizontalScale * frequency), scaledVerticalScale, scaledY, frequency);

                frequency /= 2.0;
            }

            return noise / 512.0;
        } else if (clampedInterpolation <= 0) {
            // Sample only lower noise, as the upper noise will be interpolated out.
            frequency = 1.0;
            double noise = 0.0;
            for (int octave = 0; octave < 16; octave++) {
                double scaledVerticalScale = verticalScale * frequency;
                double scaledY = y * scaledVerticalScale;
                noise += sampleOctave(this.lowerInterpolatedNoise.getOctave(octave),
                        OctavePerlinNoiseSampler.maintainPrecision(x * horizontalScale * frequency),
                        OctavePerlinNoiseSampler.maintainPrecision(scaledY),
                        OctavePerlinNoiseSampler.maintainPrecision(z * horizontalScale * frequency), scaledVerticalScale, scaledY, frequency);

                frequency /= 2.0;
            }

            return noise / 512.0;
        } else {
            // Sample both and interpolate, as in vanilla.

            frequency = 1.0;
            double lowerNoise = 0.0;
            double upperNoise = 0.0;

            for (int octave = 0; octave < 16; octave++) {
                // Pre calculate these values to share them
                double scaledVerticalScale = verticalScale * frequency;
                double scaledY = y * scaledVerticalScale;
                double xVal = OctavePerlinNoiseSampler.maintainPrecision(x * horizontalScale * frequency);
                double yVal = OctavePerlinNoiseSampler.maintainPrecision(scaledY);
                double zVal = OctavePerlinNoiseSampler.maintainPrecision(z * horizontalScale * frequency);

                upperNoise += sampleOctave(this.upperInterpolatedNoise.getOctave(octave), xVal, yVal, zVal, scaledVerticalScale, scaledY, frequency);
                lowerNoise += sampleOctave(this.lowerInterpolatedNoise.getOctave(octave), xVal, yVal, zVal, scaledVerticalScale, scaledY, frequency);

                frequency /= 2.0;
            }

            // Vanilla behavior, return interpolated noise
            return MathHelper.lerp(clampedInterpolation, lowerNoise / 512.0, upperNoise / 512.0);
        }
    }

    private static double sampleOctave(PerlinNoiseSampler sampler, double x, double y, double z, double scaledVerticalScale, double scaledY, double frequency) {
        return sampler.sample(x, y, z, scaledVerticalScale, scaledY) / frequency;
    }

    protected double[] createNoiseColumn(int x, int z) {
        double[] noise = new double[this.noiseSizeY + 1];
        this.sampleNoiseColumn(noise, x, z);
        return noise;
    }

    protected abstract void sampleNoiseColumn(double[] buffer, int x, int z, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch, int interpolationSize, int interpolateTowards);

    protected abstract double[] computeNoiseData(int x, int z);

    protected abstract double computeNoiseFalloff(double depth, double scale, int y);

    protected double upperInterpolationStart() {
        return this.getNoiseSizeY() - 4;
    }

    protected double lowerInterpolationStart() {
        return -500.0D;
    }

    public int getHeight(int x, int z, Type heightmapType, HeightLimitView world, NoiseConfig noiseConfig) {
        return this.sampleHeightmap(x, z, null, heightmapType.getBlockPredicate());
    }

    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        BlockState[] states = new BlockState[this.noiseSizeY * this.verticalNoiseResolution];
        this.sampleHeightmap(x, z, states, null);
        return new VerticalBlockSample(getMinimumY(), states);
    }

    private int sampleHeightmap(int x, int z, @Nullable BlockState[] blockStates, @Nullable Predicate<BlockState> predicate) {
        // TODO: handle rivers and such
        // Get all of the coordinate starts and positions
        int xStart = Math.floorDiv(x, this.horizontalNoiseResolution);
        int zStart = Math.floorDiv(z, this.horizontalNoiseResolution);
        int xProgress = Math.floorMod(x, this.horizontalNoiseResolution);
        int zProgress = Math.floorMod(z, this.horizontalNoiseResolution);
        double xLerp = (double) xProgress / (double)this.horizontalNoiseResolution;
        double zLerp = (double) zProgress / (double)this.horizontalNoiseResolution;
        // Create the noise data in a 2 * 2 * 32 grid for interpolation.
        double[][] noiseData = new double[][]{this.createNoiseColumn(xStart, zStart), this.createNoiseColumn(xStart, zStart + 1), this.createNoiseColumn(xStart + 1, zStart), this.createNoiseColumn(xStart + 1, zStart + 1)};

        // [0, 40] -> noise chunks
        for(int noiseY = this.noiseSizeY - 1; noiseY >= 0; --noiseY) {
            // Gets all the noise in a 2x2x2 cube and interpolates it together.
            // Lower pieces
            double x0z0y0 = noiseData[0][noiseY];
            double x0z1y0 = noiseData[1][noiseY];
            double x1z0y0 = noiseData[2][noiseY];
            double x1z1y0 = noiseData[3][noiseY];
            // Upper pieces
            double x0z0y1 = noiseData[0][noiseY + 1];
            double x0z1y1 = noiseData[1][noiseY + 1];
            double x1z0y1 = noiseData[2][noiseY + 1];
            double x1z1y1 = noiseData[3][noiseY + 1];

            // [0, 8] -> noise pieces
            for(int pieceY = this.verticalNoiseResolution - 1; pieceY >= 0; --pieceY) {
                double yLerp = (double) pieceY / (double)this.verticalNoiseResolution;
                // Density at this position given the current y interpolation
                double density = MathHelper.lerp3(yLerp, xLerp, zLerp, x0z0y0, x0z0y1, x1z0y0, x1z0y1, x0z1y0, x0z1y1, x1z1y0, x1z1y1);

                // Get the real y position (translate noise chunk and noise piece)
                int y = (noiseY * this.verticalNoiseResolution + pieceY) - 64;

                BlockState state = this.getBlockState(density, y, 0);
                if (blockStates != null) {
                    blockStates[y + 64] = state;
                }

                // return y if it fails the check
                if (predicate != null && predicate.test(state)) {
                    return y + 1;
                }
            }
        }

        return 0;
    }

    protected BlockState getBlockState(double density, int y, int minY) {
        if (density > 0.0D) {
            if (y < -8) {
                return Blocks.DEEPSLATE.getDefaultState();
            } else if (y < 0) {
                double chance = -(y / 8.0);

                // TODO: not setting seed breaks heightmap sample
                if (this.random.nextDouble() < chance) {
                    return Blocks.DEEPSLATE.getDefaultState();
                }
            }
            return this.defaultBlock;
        } else if (y < this.getSeaLevel() && y > (minY * 8)) {
            return this.defaultFluid;
        } else {
            if (y <= -56) {
                return Blocks.LAVA.getDefaultState();
            }

            return AIR;
        }
    }

    protected void sampleNoiseColumn(double[] buffer, int x, int z) {
        ChunkPos pos = new ChunkPos(x << 2, z << 2);
        PlateSet plateSet = this.riverWorker.forChunk(pos);

        sampleNoiseColumn(null, plateSet.findForChunk(pos), buffer, x, z);
    }

    protected int sampleNoiseColumn(Chunk chunk, List<RiverNode> riverPlate, double[] buffer, int x, int z) {
        this.noiseCache.get().get(buffer, x, z);

        for (int i = 0; i < buffer.length; i++) {
            buffer[i] /= 200;
        }

        // River filter
        if (riverPlate != null) {
            // Ugly hack- we don't have (or need) the chunk in heightmap sampling
            // TODO: shared empty chunk data storage
            ChunkDataStorage storage = chunk == null ? new ChunkDataStorage() : ChunkStorageView.getStorage(chunk);
            if (storage != null) {
                filterForRiver(storage, riverPlate, buffer, x, z);
            }
        }

        NoiseColumn acol = new NoiseColumn();
        acol.buffer = buffer;
        generateCavesInto(x, z, acol);

        return acol.maxY;
    }

    protected abstract void generateCavesInto(int x, int z, NoiseColumn col);

    private void filterForRiver(ChunkDataStorage storage, List<RiverNode> riverPlate, double[] buffer, int x, int z) {
        BitSet bits = new BitSet(36);
        for (RiverNode node : riverPlate) {
            double radius = node.radius();
            int rad = (int) radius;
            if (!node.getSuccessors().isEmpty()) {
                RiverNode successor = node.getSuccessors().get(0);

                int xStart = ((int) node.x());
                int zStart = ((int) node.z());
                int xNext = ((int) successor.x());
                int zNext = ((int) successor.z());

                for (int i = 0; i < 20; i++) {
                    double progress = (double) i / 20.0;

                    int ax = (int) MathHelper.lerp(progress, xStart, xNext);
                    int az = (int) MathHelper.lerp(progress, zStart, zNext);

                    for (int x1 = -rad; x1 <= rad; x1++) {
                        for (int z1 = -rad; z1 <= rad; z1++) {
                            if (x1 * x1 + z1 * z1 <= radius * radius) {
                                int gx = ax + x1;
                                int gz = az + z1;

                                if (gx >= (x * 4) - 1 && gx < ((x + 1) * 4) + 1 && gz >= (z * 4) - 1 && gz < ((z + 1) * 4) + 1) {
                                    int index1 = (gx - ((x * 4) - 1));
                                    int index2 = (gz - ((z * 4) - 1)) * 6;

                                    int index = index1 + index2;

                                    if (index < 0 || index >= 36) {
                                        throw new IllegalStateException("Index out of bounds: " + index + " (x=" + x + ", z=" + z + ")" + " (gx=" + gx + ", gz=" + gz + ")" + " (x1=" + x1 + ", z1=" + z1 + ")");
                                    }

                                    bits.set(index);
                                }
                            }
                        }
                    }
                }
            }
        }

        int card = bits.cardinality();

        double lerpstr = (double) card / 36.0;
        if (lerpstr > 0.0) {
            RiverData data = storage.get(StorageKeys.RIVER_DATA);

            // Add river data if it doesn't exist
            if (data == null) {
                storage.add(StorageKeys.RIVER_DATA, new RiverData(buffer[9 + 8] < 0));
            } else {
                // Update open to air
                if (!data.openToAir() && (buffer[9 + 8] < 0)) {
                    storage.add(StorageKeys.RIVER_DATA, new RiverData(true));
                }
            }
        }

        double noise = this.riverNoiseNoise.sample(x, z);
        // River area- actual river transformation
        buffer[8 + 8] = MathHelper.lerp(lerpstr, buffer[8 + 8], -0.7 + noise);

        // Reduce edges
        //1+\left(x-1\right)^{3}
        double lerp9 = 1 + Math.pow(lerpstr - 1, 3);
        buffer[9 + 8] = MathHelper.lerp(lerp9, buffer[9 + 8], buffer[9 + 8] - MathHelper.clampedMap(buffer[10 + 8], -1, 0, 1.6, 0));
    }

    protected abstract void fillNoiseColumn(double[] buffer, int x, int z);

    public int getNoiseSizeY() {
        return this.noiseSizeY + 1;
    }

    protected abstract RegistryKey<Biome> key(Biome biome);

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        ChunkPos pos = chunk.getPos();
        int chunkX = pos.x;
        int chunkZ = pos.z;
        ImprovedChunkRandom random = new ImprovedChunkRandom(0);
        random.setTerrainSeed(chunkX, chunkZ);
        int chunkStartX = pos.getStartX();
        int chunkStartZ = pos.getStartZ();
        Mutable mutable = new Mutable();

        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                int localX = chunkStartX + x;
                int localZ = chunkStartZ + z;

                int y = chunk.sampleHeightmap(Type.WORLD_SURFACE_WG, x, z) + 1;

                double noise = this.surfaceDepthNoise.sample((double) localX * 0.0625D, (double) localZ * 0.0625D, true) * 0.55 * 15.0D;


                Biome biome = region.getBiome(mutable.set(localX, y, localZ)).value();
                ConfiguredSurfaceBuilder<?> configuredSurfaceBuilder = BiomeGenData.LOOKUP.getOrDefault(key(biome), BiomeGenData.DEFAULT).surface();
                configuredSurfaceBuilder.initSeed(region.getSeed());

                dispatchSurfaceBuilder(configuredSurfaceBuilder, region, chunk, random, localX, localZ, y, noise, biome);
            }
        }

        this.buildBedrock(chunk, random);
    }

    private <SC extends SurfaceConfig> void dispatchSurfaceBuilder(ConfiguredSurfaceBuilder<SC> configuredSurfaceBuilder, ChunkRegion region, Chunk chunk, Random random, int x, int z, int y, double noise, Biome biome) {
        SurfaceBuilder<SC> builder =  configuredSurfaceBuilder.surfaceBuilder;

        SC config = configuredSurfaceBuilder.config;

        if (builder instanceof SlopedSurfaceBuilder<SC> sloped) {
            int dx = (getHeight(x + 1, z, Type.WORLD_SURFACE_WG, region, null) + 1) - (getHeight(x - 1, z, Type.WORLD_SURFACE_WG, region, null) + 1);
            int dz = (getHeight(x, z - 1, Type.WORLD_SURFACE_WG, region, null) + 1) - (getHeight(x, z - 1, Type.WORLD_SURFACE_WG, region, null) + 1);

            int slope = dx * dx + dz * dz;

            sloped.generate(random, chunk, biome, x, z, y, noise,
                    this.defaultBlock, this.defaultFluid, this.getSeaLevel(), 0, region.getSeed(), slope, config);
        } else {
            builder.generate(random, chunk, biome, x, z, y, noise,
                    this.defaultBlock, this.defaultFluid, this.getSeaLevel(), 0, region.getSeed(), config);
        }
    }

    protected void buildBedrock(Chunk chunk, Random random) {
        Mutable mutable = new Mutable();
        int chunkStartX = chunk.getPos().getStartX();
        int chunkStartZ = chunk.getPos().getStartZ();

        int start = GENERATE_384 ? -64 : 0;

        for (BlockPos pos : BlockPos.iterate(chunkStartX, 0, chunkStartZ, chunkStartX + 15, 0, chunkStartZ + 15)) {
            for (int y = start + 4; y >= start; --y) {
                if (y <= random.nextInt(5)) {
                    chunk.setBlockState(mutable.set(pos.getX(), y, pos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
                }
            }
        }
    }


    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        // TODO: proper multithreading
        populateNoise(structureAccessor, chunk);

        return CompletableFuture.completedFuture(chunk);
    }

    public void populateNoise(StructureAccessor accessor, Chunk chunk) {
        ChunkPos pos = chunk.getPos();
        int chunkX = pos.x;
        int chunkZ = pos.z;
        int chunkStartX = chunkX << 4;
        int chunkStartZ = chunkZ << 4;

        StructureWeightSampler structures = StructureWeightSampler.createStructureWeightSampler(accessor, pos);

        // Holds the rolling noise data for this chunk
        // Instead of being noise[4 * 33 * 4] it's actually noise[2 * 5 * 33] to reuse noise data when moving onto the next column on the x axis.
        // This could probably be optimized but I'm a bit too lazy to figure out the best way to do so :P
        double[][][] noiseData = new double[this.noiseSizeX + 1][this.noiseSizeZ + 1][this.noiseSizeY + 1];

        PlateSet riverPlate = this.riverWorker.forChunk(pos);

        List<RiverNode> riverNodes = riverPlate.findForChunk(pos);

        // Initialize noise data on the x0 column.
        int minY = 10000;
        for(int noiseX = 0; noiseX < this.noiseSizeX + 1; ++noiseX) {
            for(int noiseZ = 0; noiseZ < this.noiseSizeZ + 1; ++noiseZ) {
                noiseData[noiseX][noiseZ] = new double[this.noiseSizeY + 1];
                int minYHere = this.sampleNoiseColumn(chunk, riverNodes, noiseData[noiseX][noiseZ], chunkX * this.noiseSizeX + noiseX, chunkZ * this.noiseSizeZ + noiseZ);
                minY = Math.min(minY, minYHere);
            }
        }

        ProtoChunk protoChunk = (ProtoChunk)chunk;
        Heightmap oceanFloor = protoChunk.getHeightmap(Type.OCEAN_FLOOR_WG);
        Heightmap worldSurface = protoChunk.getHeightmap(Type.WORLD_SURFACE_WG);
        Mutable mutable = new Mutable();

        this.random.setPopulationSeed(this.seed, chunkX * 100, chunkZ * 100);

        MutableNoisePos mpos = new MutableNoisePos();

        // [0, 4] -> x noise chunks
        for(int noiseX = 0; noiseX < this.noiseSizeX; ++noiseX) {
            // Initialize noise data on the x1 column

            // [0, 4] -> z noise chunks
            for(int noiseZ = 0; noiseZ < this.noiseSizeZ; ++noiseZ) {
                int lastSecY = chunk.getSectionIndex(319);
                ChunkSection section = protoChunk.getSection(lastSecY);
                section.lock();

                // [0, 32] -> y noise chunks
                for(int noiseY = this.noiseSizeY - 1; noiseY >= 0; --noiseY) {
                    // Lower samples
                    double x0z0y0 = noiseData[noiseX][noiseZ][noiseY];
                    double x0z1y0 = noiseData[noiseX][noiseZ + 1][noiseY];
                    double x1z0y0 = noiseData[noiseX + 1][noiseZ][noiseY];
                    double x1z1y0 = noiseData[noiseX + 1][noiseZ + 1][noiseY];
                    // Upper samples
                    double x0z0y1 = noiseData[noiseX][noiseZ][noiseY + 1];
                    double x0z1y1 = noiseData[noiseX][noiseZ + 1][noiseY + 1];
                    double x1z0y1 = noiseData[noiseX + 1][noiseZ][noiseY + 1];
                    double x1z1y1 = noiseData[noiseX + 1][noiseZ + 1][noiseY + 1];

                    // [0, 8) -> y noise pieces
                    for(int pieceY = this.verticalNoiseResolution - 1; pieceY >= 0; --pieceY) {
                        int realY = noiseY * this.verticalNoiseResolution + pieceY - 64;
                        if (!GENERATE_384) {
                            if (realY < 0 || realY > 255) {
                                continue;
                            }
                        }

                        int localY = realY & 15;
                        int sectionY = chunk.getSectionIndex(realY);
                        // Get the chunk section
                        if (lastSecY != sectionY) {
                            section.unlock();
                            section = protoChunk.getSection(sectionY);
                            lastSecY = sectionY;
                            section.lock();
                        }

                        // progress within loop
                        double yLerp = (double) pieceY / (double)this.verticalNoiseResolution;

                        // Interpolate noise data based on y progress
                        double x0z0 = MathHelper.lerp(yLerp, x0z0y0, x0z0y1);
                        double x1z0 = MathHelper.lerp(yLerp, x1z0y0, x1z0y1);
                        double x0z1 = MathHelper.lerp(yLerp, x0z1y0, x0z1y1);
                        double x1z1 = MathHelper.lerp(yLerp, x1z1y0, x1z1y1);

                        // [0, 4) -> x noise pieces
                        for(int pieceX = 0; pieceX < this.horizontalNoiseResolution; ++pieceX) {
                            int realX = chunkStartX + noiseX * this.horizontalNoiseResolution + pieceX;
                            int localX = realX & 15;
                            double xLerp = (double) pieceX / (double)this.horizontalNoiseResolution;
                            // Interpolate noise based on x progress
                            double z0 = MathHelper.lerp(xLerp, x0z0, x1z0);
                            double z1 = MathHelper.lerp(xLerp, x0z1, x1z1);

                            // [0, 4) -> z noise pieces
                            for(int pieceZ = 0; pieceZ < this.horizontalNoiseResolution; ++pieceZ) {
                                int realZ = chunkStartZ + noiseZ * this.horizontalNoiseResolution + pieceZ;
                                int localZ = realZ & 15;
                                double zLerp = (double) pieceZ / (double)this.horizontalNoiseResolution;
                                // Get the real noise here by interpolating the last 2 noises together
                                double rawNoise = MathHelper.lerp(zLerp, z0, z1);
                                // Normalize the noise from (-256, 256) to [-1, 1]
                                double density = MathHelper.clamp(rawNoise /*rawNoise / 200.0D*/, -1.0D, 1.0D);

                                // Iterate through structures to add density
                                density = density / 2.0D - density * density * density / 24.0D;

                                mpos.set(realX, realY, realZ);

                                double structureAdd = structures.sample(mpos);

                                density += structureAdd;

                                // Get the blockstate based on the y and density
                                BlockState state = this.getBlockState(density, realY, minY);

                                if (state != AIR) {
                                    // Add light source if the state has light
                                    if (state.getLuminance() != 0) {
                                        mutable.set(realX, realY, realZ);
                                    }

                                    // Place the state at the position
                                    section.setBlockState(localX, localY, localZ, state, false);
                                    // Track heightmap data
                                    oceanFloor.trackUpdate(localX, realY, localZ, state);
                                    worldSurface.trackUpdate(localX, realY, localZ, state);
                                }
                            }
                        }
                    }
                }

                section.unlock();
            }
        }

    }

    private static double computeNoiseWeight(int x, int y, int z) {
        double squaredXZ = x * x + z * z;
        double normalizedY = (double)y + 0.5D;
        double squaredY = normalizedY * normalizedY;
        double ePow = Math.pow(2.718281828459045D, -(squaredY / 16.0D + squaredXZ / 16.0D));
        double finalizedVal = -normalizedY * MathHelper.fastInverseSqrt(squaredY / 2.0D + squaredXZ / 2.0D) / 2.0D;
        return finalizedVal * ePow;
    }

    private class NoiseCache {
        private final long[] keys;
        private final double[] values;

        private final int mask;

        private NoiseCache(int size, int noiseSize) {
            size = MathHelper.smallestEncompassingPowerOfTwo(size);
            this.mask = size - 1;

            this.keys = new long[size];
            Arrays.fill(this.keys, Long.MIN_VALUE);
            this.values = new double[size * noiseSize];
        }

        public synchronized double[] get(double[] buffer, int x, int z) {
            long key = key(x, z);
            int idx = hash(key) & this.mask;

            // if the entry here has a key that matches ours, we have a cache hit
            if (this.keys[idx] == key) {
                // Copy values into buffer
                System.arraycopy(this.values, idx * buffer.length, buffer, 0, buffer.length);
            } else {
                // cache miss: sample and put the result into our cache entry

                // Sample the noise column to store the new values
                fillNoiseColumn(buffer, x, z);

                // Create copy of the array
                System.arraycopy(buffer, 0, this.values, idx * buffer.length, buffer.length);

                this.keys[idx] = key;
            }

            return buffer;
        }

        private int hash(long key) {
            return (int) HashCommon.mix(key);
        }

        private long key(int x, int z) {
            return ChunkPos.toLong(x, z);
        }
    }

    private static final class MutableNoisePos implements DensityFunction.NoisePos {
        private int x;
        private int y;
        private int z;

        public MutableNoisePos() {

        }

        private void set(int x, int y, int z) {

            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public int blockX() {
            return x;
        }

        @Override
        public int blockY() {
            return y;
        }

        @Override
        public int blockZ() {
            return z;
        }
    }
}
