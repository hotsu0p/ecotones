package com.jaskarth.ecotones.world.worldgen.features;

import com.mojang.serialization.Codec;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import com.jaskarth.ecotones.world.blocks.CattailBlock;
import com.jaskarth.ecotones.world.worldgen.features.config.CattailFeatureConfig;

public class CattailFeature extends EcotonesFeature<CattailFeatureConfig> {
    public CattailFeature(Codec<CattailFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<CattailFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = context.getRandom();
        CattailFeatureConfig config = context.getConfig();

        int count = config.count.get(random);
        int spread = config.spread.get(random);

        if (config.needsWater) {
            int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, pos.getX(), pos.getZ());

            if (!world.getFluidState(pos.withY(y)).isIn(FluidTags.WATER)) {
                return false;
            }
        }

        BlockPos.Mutable mutable = pos.mutableCopy();
        for (int i = 0; i < count; i++) {
            int dx = random.nextInt(spread) - random.nextInt(spread) + pos.getX();
            int dz = random.nextInt(spread) - random.nextInt(spread) + pos.getZ();
            int y = world.getTopY(Heightmap.Type.OCEAN_FLOOR_WG, dx, dz);

            mutable.set(dx, y, dz);

            boolean waterlogged = world.getFluidState(mutable).isIn(FluidTags.WATER);

            if (world.getBlockState(mutable).isReplaceable() && CattailBlock.PLANTABLE.contains(world.getBlockState(mutable.down()).getBlock())) {
                world.setBlockState(mutable, config.state.with(CattailBlock.WATERLOGGED, waterlogged), 3);
            }
        }

        return true;
    }
}
