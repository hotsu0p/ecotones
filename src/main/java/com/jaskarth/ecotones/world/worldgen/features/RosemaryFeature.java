package com.jaskarth.ecotones.world.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import com.jaskarth.ecotones.world.blocks.EcotonesBlocks;
import com.jaskarth.ecotones.world.blocks.RosemaryBlock;
import com.jaskarth.ecotones.world.data.DataHolder;
import com.jaskarth.ecotones.world.data.EcotonesData;

import java.util.Random;

public class RosemaryFeature extends EcotonesFeature<DefaultFeatureConfig> {
    public RosemaryFeature(Codec<DefaultFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = new Random(context.getRandom().nextLong());
        ChunkGenerator generator = context.getGenerator();

        BlockState state = EcotonesBlocks.ROSEMARY.getDefaultState();

        int extra = 0;
        if (generator instanceof DataHolder) {
            extra = Math.max((int) (((DataHolder)generator).get(EcotonesData.SOIL_DRAINAGE, pos.getX(), pos.getZ()) * 12), 0);
        }

        int count = 2 + extra;

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(6) - random.nextInt(6) + pos.getX();
            int z = random.nextInt(6) - random.nextInt(6) + pos.getZ();
            int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, x, z);
            BlockPos local = new BlockPos(x, y, z);

            if (state.canPlaceAt(world, local) && world.getBlockState(local).isAir()) {
                world.setBlockState(local, state.with(RosemaryBlock.FLOWERING, random.nextBoolean()), 3);
            }
        }

        return false;
    }
}
