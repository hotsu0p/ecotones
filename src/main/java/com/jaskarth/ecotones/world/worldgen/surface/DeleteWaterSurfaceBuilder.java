package com.jaskarth.ecotones.world.worldgen.surface;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import com.jaskarth.ecotones.world.worldgen.surface.system.SurfaceBuilder;
import com.jaskarth.ecotones.world.worldgen.surface.system.TernarySurfaceConfig;

import java.util.Random;

public class DeleteWaterSurfaceBuilder extends SurfaceBuilder<TernarySurfaceConfig> {

    public DeleteWaterSurfaceBuilder(Codec<TernarySurfaceConfig> codec) {
        super(codec);
    }

    @Override
    public void generate(Random random, Chunk chunk, Biome biome, int x, int z, int height, double noise, BlockState defaultBlock, BlockState defaultFluid, int seaLevel, int start, long seed, TernarySurfaceConfig surfaceBlocks) {
        BlockPos.Mutable pos = new BlockPos.Mutable();
        pos.set(x, 0, z);
        for (int i = 0; i < 64; i++) {
            pos.setY(i);
            if (chunk.getBlockState(pos) == Blocks.WATER.getDefaultState()) {
                chunk.setBlockState(pos, Blocks.AIR.getDefaultState(), false);
            }
        }
    }
}
