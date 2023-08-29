package com.jaskarth.ecotones.world.worldgen.surface;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import com.jaskarth.ecotones.world.worldgen.surface.system.SurfaceBuilder;
import com.jaskarth.ecotones.world.worldgen.surface.system.SurfaceConfig;

import java.util.Random;

public abstract class SlopedSurfaceBuilder<SC extends SurfaceConfig> extends SurfaceBuilder<SC> {
    public SlopedSurfaceBuilder(Codec<SC> codec) {
        super(codec);
    }

    public abstract void generate(Random random, Chunk chunk, Biome biome, int x, int z, int height, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, int startHeight, long seed, double slope, SC surfaceConfig);

    @Override
    @Deprecated
    public void generate(Random random, Chunk chunk, Biome biome, int x, int z, int height, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, int startHeight, long seed, SC surfaceConfig) {
        // Shouldn't happen!
    }
}
