package com.jaskarth.ecotones.world.worldgen.features.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

import java.util.List;
import java.util.Optional;

public class WaterFeatureConfig implements FeatureConfig {
    public static final Codec<WaterFeatureConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockState.CODEC.listOf().fieldOf("targets").forGetter(config -> config.targets),
            BlockStateProvider.TYPE_CODEC.optionalFieldOf("surroundings").forGetter(config -> config.surroundings),
            Codec.INT.fieldOf("spread").forGetter(config -> config.spread)
    ).apply(instance, WaterFeatureConfig::new));

    public final List<BlockState> targets;
    public final Optional<BlockStateProvider> surroundings;
    public final int spread;

    public WaterFeatureConfig(List<BlockState> targets, Optional<BlockStateProvider> surroundings, int spread) {
        this.targets = targets;
        this.surroundings = surroundings;
        this.spread = spread;
    }
}
