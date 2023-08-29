package com.jaskarth.ecotones.util;

import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;

public final class BoxHelper {
    public static BlockBox box(BlockPos a, BlockPos b) {
        return new BlockBox(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    public static BlockBox empty() {
        return new BlockBox(0, 0, 0, 0, 0, 0);
    }
}
