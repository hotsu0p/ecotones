package com.jaskarth.ecotones.world.worldgen.tree.trait.oak;

public class DefaultOakTrait implements OakTrait {
    public static final DefaultOakTrait INSTANCE = new DefaultOakTrait();

    @Override
    public String name() {
        return "Default";
    }
}
