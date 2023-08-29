package com.jaskarth.ecotones.client.render.magnifying;

import net.minecraft.block.Block;
import org.jetbrains.annotations.Nullable;
import com.jaskarth.ecotones.world.blocks.EcotonesBlocks;
import com.jaskarth.ecotones.util.RegistryReport;

import java.util.HashMap;
import java.util.Map;

public final class EcotonesMagnifyingGlassRenderers {
    private static final Map<Block, MagnifyingGlassRenderer> RENDERERS = new HashMap<>();

    public static void init() {
        register(EcotonesBlocks.FERTILIZER_SPREADER, new FertilizerSpreaderRenderer());
    }

    private static void register(Block block, MagnifyingGlassRenderer renderer) {
        RENDERERS.put(block, renderer);
        RegistryReport.increment("Magnifying Glass Renderer");
    }

    @Nullable
    public static MagnifyingGlassRenderer rendererFor(Block block) {
        return RENDERERS.get(block);
    }
}
