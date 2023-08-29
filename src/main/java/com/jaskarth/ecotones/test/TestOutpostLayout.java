package com.jaskarth.ecotones.test;

import com.mojang.serialization.Codec;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import com.jaskarth.ecotones.util.Vec2i;
import com.jaskarth.ecotones.world.worldgen.structure.gen.layout.OutpostLayout;
import com.jaskarth.ecotones.world.worldgen.structure.gen.layout.building.Building;
import com.jaskarth.ecotones.world.worldgen.structure.gen.layout.building.House;
import com.jaskarth.ecotones.world.worldgen.structure.gen.layout.cell.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class TestOutpostLayout {
    public static void main(String[] args) {
        OutpostLayout layout = new OutpostLayout(100, 0, 0);
        SharedConstants.createGameVersion();
        Bootstrap.initialize();
        layout.generate(new FakeChunkGenerator(), new HeightLimitView() {
            @Override
            public int getHeight() {
                return 256;
            }

            @Override
            public int getBottomY() {
                return 0;
            }
        });

        BufferedImage img = new BufferedImage(512, 512, BufferedImage.TYPE_INT_RGB);
        for (Cell cell : layout.getCells()) {
            for (Vec2i position : cell.getPositions()) {
                img.setRGB(128 + position.x(), 128 + position.y(), getColor(cell));
            }

            if (cell instanceof BuildingCell buildingCell) {

                for (Building building : buildingCell.getBuildings()) {
                    if (building instanceof House) {
                        for (int x = 0; x < 4; x++) {
                            for (int z = 0; z < 4; z++) {
                                img.setRGB(128 + building.getWeldingPoint().getX() + x, 128 + building.getWeldingPoint().getZ() + z, ImageDumper.getIntFromColor(255, 255, 255));
                            }
                        }
                    }
                }
            }
        }

        // Debug cell centers
//        for (Cell cell : layout.getCells()) {
//            img.setRGB(128 + ((int) cell.getCenter().x()), 128 + ((int) cell.getCenter().y()), ImageDumper.getIntFromColor(0, 0, 0));
//        }

        Path p = Paths.get("outpost_layout.png");
        try {
            ImageIO.write(img, "png", p.toAbsolutePath().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int getColor(Cell cell) {
        Random random = new Random();
        random.setSeed(cell.getUniqueId());

        // Should be impossible- Things have gone horribly wrong!
        if (cell instanceof GeneralCell) {
            return ImageDumper.getIntFromColor(255, 0, 0);
        }

        if (cell instanceof EmptyCell) {
            int r = 120 + random.nextInt(40) - random.nextInt(40);
            int g = 190 + random.nextInt(22) - random.nextInt(22);
            int b = 60 + random.nextInt(30) - random.nextInt(30);

            return ImageDumper.getIntFromColor(r, g, b);
        }

        if (cell instanceof FarmCell) {
            int r = 70 + random.nextInt(20) - random.nextInt(20);
            int g = 230 + random.nextInt(15) - random.nextInt(15);
            int b = 40 + random.nextInt(30) - random.nextInt(30);

            return ImageDumper.getIntFromColor(r, g, b);
        }

        if (cell instanceof BuildingCell) {
            int r = 160 + random.nextInt(40) - random.nextInt(40);
            int g = 100 + random.nextInt(50) - random.nextInt(50);
            int b = 200 + random.nextInt(40) - random.nextInt(40);

            return ImageDumper.getIntFromColor(r, g, b);
        }

        return ImageDumper.getIntFromColor(255, 255, 255);
    }

    private static class FakeChunkGenerator extends ChunkGenerator {
        public FakeChunkGenerator() {
            super(null, null);
        }

        @Override
        protected Codec<? extends ChunkGenerator> getCodec() {
            return null;
        }

        @Override
        public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess world, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {

        }

        @Override
        public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {

        }

        @Override
        public void populateEntities(ChunkRegion region) {

        }

        @Override
        public int getWorldHeight() {
            return 0;
        }

        @Override
        public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
            return null;
        }

        @Override
        public int getSeaLevel() {
            return 0;
        }

        @Override
        public int getMinimumY() {
            return 0;
        }

        @Override
        public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
            return 0;
        }

        @Override
        public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
            return null;
        }

        @Override
        public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {

        }
    }
}
