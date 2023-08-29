package com.jaskarth.ecotones.world.worldgen.features.tree;

import com.jaskarth.ecotones.world.worldgen.features.FeatureHelper;
import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.treedecorator.TreeDecorator;
import com.jaskarth.ecotones.world.worldgen.features.EcotonesFeature;
import com.jaskarth.ecotones.world.worldgen.features.config.SimpleTreeFeatureConfig;
import com.jaskarth.ecotones.world.worldgen.tree.decorator.PineconeTreeDecorator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

public class LarchTreeFeature extends EcotonesFeature<SimpleTreeFeatureConfig> {
    private static final PineconeTreeDecorator PINECONES = new PineconeTreeDecorator(2);

    public LarchTreeFeature(Codec<SimpleTreeFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<SimpleTreeFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = context.getRandom();
        SimpleTreeFeatureConfig config = context.getConfig();

        if (!world.getBlockState(pos.down()).isIn(BlockTags.DIRT)) {
            return false;
        }

        int heightAddition = 3 + random.nextInt(4);

        double maxRadius = random.nextDouble() * 0.3 + 2.2;

        BlockPos.Mutable mutable = pos.mutableCopy();

        List<BlockPos> logs = new ArrayList<>();
        for (int y = 0; y < 8 + heightAddition; y++) {
            world.setBlockState(mutable, config.woodState, 3);
            logs.add(mutable.toImmutable());
            mutable.move(Direction.UP);
        }

        mutable = pos.mutableCopy();
        mutable.move(Direction.UP, 1 + heightAddition);

        List<BlockPos> leaves = new ArrayList<>();
        for (int y = 0; y < 8; y++) {
            FeatureHelper.circle(mutable.mutableCopy(), maxRadius * model(y / 8.f), leafPos -> {
                if (AbstractTreeFeature.isAirOrLeaves(world, leafPos)) {
                    world.setBlockState(leafPos, config.leafState, 3);
                    leaves.add(leafPos.toImmutable());
                }
            });
            mutable.move(Direction.UP);
        }

        BiConsumer<BlockPos, BlockState> replacer = (p, s) -> world.setBlockState(p, s, 3);

        PINECONES.generate(new TreeDecorator.Generator(world, replacer, random, new HashSet<>(logs), new HashSet<>(leaves), Set.of()));

        return true;
    }

    // Desmos: -x^{2}+0.4x+0.6
    private static double model(double x) {
        return (-(x * x) + (0.4 * x) + 0.6) * 1.6;
    }
}
