package com.jaskarth.ecotones.world.worldgen.features.tree;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.world.ModifiableTestableWorld;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import com.jaskarth.ecotones.world.blocks.EcotonesBlocks;
import com.jaskarth.ecotones.util.BoxHelper;

import java.util.Random;
import java.util.Set;

//Code from Terrestria, used with permission
public class PalmTreeFeature extends AbstractTreeFeature<TreeFeatureConfig> {
    private BlockState bark;

    public PalmTreeFeature(Codec<TreeFeatureConfig> codec, BlockState bark) {
        super(codec);
        this.bark = bark;
    }

    private static Direction spiral(Direction direction, boolean invert) {
        switch (direction) {
            case EAST:
                return invert ? Direction.NORTH : Direction.SOUTH;
            case WEST:
                return invert ? Direction.SOUTH : Direction.NORTH;
            case NORTH:
                return invert ? Direction.WEST : Direction.EAST;
            case SOUTH:
            default:
                return invert ? Direction.EAST : Direction.WEST;
        }
    }

    @Override
    public boolean generate(StructureWorldAccess world, Random rand, BlockPos origin, Set<BlockPos> logs, Set<BlockPos> leaves, BlockBox box, TreeFeatureConfig config) {
        // Total trunk height
        int height = rand.nextInt(5) + 8;

        if (origin.getY() + height + 1 > 256 || origin.getY() < 1) {
            return false;
        }

        BlockPos below = origin.down();

        boolean sand = false;

        if (!isNaturalDirtOrGrass(world, below)) {
            if(world.testBlockState(below, state -> state.isIn(BlockTags.SAND))) {
                sand = true;
            } else {
                return false;
            }
        }

        if(!check(world, origin, height)) {
            return false;
        }

        if(!sand) {
            setToDirt(world, below);
        }

        BlockPos.Mutable pos = origin.mutableCopy();
        growTrunk(world, rand, pos, logs, box, config, height);
        growLeaves(world, rand, pos, leaves, box, config);

        return true;
    }

    private boolean check(TestableWorld world, BlockPos origin, int height) {
        BlockPos.Mutable pos = origin.mutableCopy();

        for(int y = 0; y < height; y++) {
            int radius = y >= 2 ? 1 : 0;

            for(int z = -radius; z <= radius; z++) {
                for(int x = -radius; x <= radius; x++) {
                    pos.set(origin).move(x, y, z);

                    if(!canTreeReplace(world, pos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    // Grows the bent trunk of the tree.
    private void growTrunk(ModifiableTestableWorld world, Random rand, BlockPos.Mutable pos, Set<BlockPos> logs, BlockBox box, TreeFeatureConfig config, int height) {
        for (int i = 0; i < 2; i++) {
            setLogBlockState(world, rand, pos, logs, box, config);
            pos.move(Direction.UP);
        }

        int run = 0;

        int velocityX = rand.nextBoolean() ? 1 : -1;
        int velocityZ = rand.nextBoolean() ? 1 : -1;

        for (int i = 2; i < height; i++) {
            if(!canTreeReplace(world, pos)) {
                return;
            }

            if (run++ == 3) {
                setBlockState(logs, world, pos, bark, box);

                if (rand.nextBoolean()) {
                    pos.move(velocityX, 0, 0);
                } else {
                    pos.move(0, 0, velocityZ);
                }
                run = 0;

                if(!canTreeReplace(world, pos)) {
                    return;
                }

                setBlockState(logs, world, pos, bark, box);
            } else {
                setLogBlockState(world, rand, pos, logs, box, config);
            }

            pos.move(Direction.UP);
        }

        setLogBlockState(world, rand, pos, logs, box, config);
        BlockPos coconutPos = pos.toImmutable().offset(Direction.DOWN);
        world.setBlockState(coconutPos.add(-1, 0, 0), EcotonesBlocks.COCONUT.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.EAST), 2);
        world.setBlockState(coconutPos.add(1, 0, 0), EcotonesBlocks.COCONUT.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST), 2);
        world.setBlockState(coconutPos.add(0, 0, 1), EcotonesBlocks.COCONUT.getDefaultState(), 2);
        world.setBlockState(coconutPos.add(0, 0, -1), EcotonesBlocks.COCONUT.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.SOUTH), 2);
    }

    private void growLeaves(ModifiableTestableWorld world, Random rand, BlockPos.Mutable pos, Set<BlockPos> leaves, BlockBox box, TreeFeatureConfig config) {
        BlockPos center = pos.toImmutable();

        setLeavesBlockState(world, rand, pos.set(center).move(0, 1, 0), leaves, box, config);
        setLeavesBlockState(world, rand, pos.set(center).move(1, 1, 0), leaves, box, config);
        setLeavesBlockState(world, rand, pos.set(center).move(0, 1, 1), leaves, box, config);
        setLeavesBlockState(world, rand, pos.set(center).move(-1, 1, 0), leaves, box, config);
        setLeavesBlockState(world, rand, pos.set(center).move(0, 1, -1), leaves, box, config);

        boolean invertLeafSpiral = rand.nextBoolean();

        for (int dZ = -1; dZ < 2; dZ++) {
            for (int dX = -1; dX < 2; dX++) {
                setLeavesBlockState(world, rand, pos.set(center).move(dZ, 0, dX), leaves, box, config);
            }
        }

        for (int d = 0; d < 4; d++) {
            Direction direction = Direction.fromHorizontal(d);

            pos.set(center).move(direction, 2);
            placeSpiral(world, rand, pos, leaves, box, config, direction, !invertLeafSpiral);

            pos.set(center).move(direction, 3);
            placeSpiral(world, rand, pos, leaves, box, config, direction, invertLeafSpiral);
        }
    }

    private void placeSpiral(ModifiableTestableWorld world, Random rand, BlockPos.Mutable pos, Set<BlockPos> leaves, BlockBox box, TreeFeatureConfig config, Direction direction, boolean invertLeafSpiral) {
        setLeavesBlockState(world, rand, pos, leaves, box, config);

        Direction spiral = spiral(direction, invertLeafSpiral);
        setLeavesBlockState(world, rand, pos.move(spiral), leaves, box, config);

        for (int i = 0; i < 2; i++) {
            setLeavesBlockState(world, rand, pos.move(Direction.DOWN), leaves, box, config);
        }
    }

    public static boolean setBlockState(Set<BlockPos> blocks, ModifiableTestableWorld world, BlockPos pos, BlockState state, BlockBox box) {
        if (!AbstractTreeFeature.isAirOrLeaves(world, pos) && !AbstractTreeFeature.isReplaceablePlant(world, pos) && !AbstractTreeFeature.isWater(world, pos)) {
            return false;
        } else {
            blocks.add(pos.toImmutable());
            world.setBlockState(pos, state, 19);
            box.encompass(BoxHelper.box(pos, pos));

            return true;
        }
    }

    public static boolean setLeavesWithDistance(ModifiableTestableWorld world, Random random, BlockPos pos, Set<BlockPos> blocks, BlockBox box, TreeFeatureConfig config, int distance) {
        BlockState state = config.foliageProvider.get(new CheckedRandom(random.nextLong()), pos);

        if(state.contains(Properties.DISTANCE_1_7)) {
            state = state.with(Properties.DISTANCE_1_7, Math.min(distance, 7));
        }

        return setBlockState(blocks, world, pos, state, box);
    }
}