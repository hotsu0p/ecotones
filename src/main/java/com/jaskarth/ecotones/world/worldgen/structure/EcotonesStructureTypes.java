package com.jaskarth.ecotones.world.worldgen.structure;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;
import com.jaskarth.ecotones.Ecotones;

public final class EcotonesStructureTypes {
    public static final StructureType<CampfireStructureFeature> CAMPFIRE = () -> CampfireStructureFeature.CODEC;
    public static final StructureType<CottageStructureFeature> COTTAGE = () -> CottageStructureFeature.CODEC;
    public static final StructureType<OutpostStructure> OUTPOST = () -> OutpostStructure.CODEC;

    public static void init() {
        register(Ecotones.id("campfire"), CAMPFIRE);
        register(Ecotones.id("cottage"), COTTAGE);
        register(Ecotones.id("outpost"), OUTPOST);
    }

    private static <S extends Structure> StructureType<S> register(Identifier id, StructureType<S> st) {
        return Registry.register(Registries.STRUCTURE_TYPE, id, st);
    }
}
