package com.jaskarth.ecotones.world.worldgen.features;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.placementmodifier.BiomePlacementModifier;

public class LookupFeature extends EcotonesFeature<LookupFeature.Config> {
    public LookupFeature(Codec<Config> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<Config> context) {
        PlacedFeature pf = context.getWorld().getServer().getRegistryManager().get(RegistryKeys.PLACED_FEATURE).get(new Identifier(context.getConfig().id()));
        PlacedFeature npf = new PlacedFeature(pf.feature(), pf.placementModifiers().stream().filter(m -> !(m instanceof BiomePlacementModifier)).toList());

        return npf.generateUnregistered(context.getWorld(), context.getGenerator(), context.getRandom(), context.getOrigin());
    }

    public record Config(String id) implements FeatureConfig {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("id").forGetter(Config::id)
        ).apply(instance, Config::new));
    }
}
