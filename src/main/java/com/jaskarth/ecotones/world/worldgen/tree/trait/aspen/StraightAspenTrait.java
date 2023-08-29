package com.jaskarth.ecotones.world.worldgen.tree.trait.aspen;

import java.util.Random;

public class StraightAspenTrait implements AspenTrait {
    @Override
    public double branchThreshold(Random random) {
        return 10;
    }

    @Override
    public String name() {
        return "Straight";
    }
}
