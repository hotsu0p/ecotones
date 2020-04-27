package supercoder79.ecotones.api;

import net.minecraft.block.Blocks;

public enum TreeType {
    OAK(new TreeGenerationConfig(1, Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), 4, 4, 6, 18, 0.75, 0.5)),
    DRY_OAK(new TreeGenerationConfig(0.2, Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), 3, 6, 4, 10, 0.35, 0.25)),
    RARE_LARGE_OAK(new TreeGenerationConfig(0.06, Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState(), 3, 5, 8, 4, 0.85, 0.65)),
    BIRCH(new TreeGenerationConfig(1, Blocks.BIRCH_LOG.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState(), 5, -1, 8, 20, 0.25, 0.25)),
    DRY_BIRCH(new TreeGenerationConfig(0.3, Blocks.BIRCH_LOG.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState(), 3, -1, 5, 12, 0.2, 0.2)),
    ACACIA(new TreeGenerationConfig(0.8, Blocks.ACACIA_LOG.getDefaultState(), Blocks.ACACIA_LEAVES.getDefaultState(), 3, 8, 6, 6, 0.8, 0.6)),
    RARE_ACACIA(new TreeGenerationConfig(0.1, Blocks.ACACIA_LOG.getDefaultState(), Blocks.ACACIA_LEAVES.getDefaultState(), 3, 8, 6, 6, 0.8, 0.6));

    public final TreeGenerationConfig config;

    TreeType(TreeGenerationConfig config) {
        this.config = config;
    }
}