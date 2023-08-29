package com.jaskarth.ecotones.world.worldgen.gen;

import com.jaskarth.ecotones.Ecotones;
import com.jaskarth.ecotones.world.worldgen.layers.LayerHelper;
import com.jaskarth.ecotones.world.worldgen.layers.system.BiomeLayerSampler;
import com.jaskarth.ecotones.world.worldgen.layers.system.layer.*;
import com.jaskarth.ecotones.world.worldgen.layers.system.layer.util.*;
import com.jaskarth.ecotones.world.worldgen.layers.generation.*;
import com.jaskarth.ecotones.world.worldgen.layers.util.*;

import java.util.function.LongFunction;

public final class EcotonesBiomeLayers {
    public static <T extends LayerSampler, C extends LayerSampleContext<T>> LayerFactory<T> build(long seed, LongFunction<C> contextProvider) {
        //Initialize land
        LayerFactory<T> layerFactory = LandLayer.INSTANCE.create(contextProvider.apply(1L));

        layerFactory = ScaleLayer.FUZZY.create(contextProvider.apply(2000L), layerFactory);
        layerFactory = ScaleLayer.NORMAL.create(contextProvider.apply(2001L), layerFactory);

        // RemoveTooMuchOcean- do we need it?
        layerFactory = AddIslandLayer.INSTANCE.create(contextProvider.apply(2L), layerFactory);

        layerFactory = LayerHelper.stack(2001L, ScaleLayer.NORMAL, layerFactory, 2, contextProvider);

        //Add ocean temperatures and deep oceans
        LayerFactory<T> layerFactory2 = OceanTemperatureLayer.INSTANCE.create(contextProvider.apply(2L));
        layerFactory2 = LayerHelper.stack(2301L, ScaleLayer.NORMAL, layerFactory2, 1, contextProvider);
        layerFactory = ApplyOceanTemperatureLayer.INSTANCE.create(contextProvider.apply(100L), layerFactory, layerFactory2);
        layerFactory = DeepOceanLayer.INSTANCE.create(contextProvider.apply(102L), layerFactory);

        //scale up the land to be bigger
        layerFactory = LayerHelper.stack(2001L, ScaleLayer.NORMAL, layerFactory, 2, contextProvider);
        //add beaches
        layerFactory = ShorelineLayer.INSTANCE.create(contextProvider.apply(54L), layerFactory, seed + 43);
        layerFactory = LayerHelper.stack(2081L, ScaleLayer.NORMAL, layerFactory, 3, contextProvider);
        // Total: 2^7 (2 + 2 + 3)

        //Add our biomes
        LayerFactory<T> climateLayer = ClimateLayer.INSTANCE.create(contextProvider.apply(2L), seed + 79);
        LayerFactory<T> biomeLayer = PickFromClimateLayer.INSTANCE.create(contextProvider.apply(2L), climateLayer);
        LayerFactory<T> initialBiomeLayer = MountainLayer.INSTANCE.create(contextProvider.apply(49L), biomeLayer, climateLayer, seed + 1337);

        biomeLayer = LayerHelper.stack(7970L, ScaleLayer.NORMAL, initialBiomeLayer, 1, contextProvider);
        climateLayer = LayerHelper.stack(7970L, ScaleLayer.NORMAL, climateLayer, 1, contextProvider);

        biomeLayer = MountainBigEdgeLayer.INSTANCE.create(contextProvider.apply(1001L), biomeLayer, climateLayer);

        biomeLayer = LayerHelper.stack(7975L, ScaleLayer.NORMAL, biomeLayer, 1, contextProvider);
        climateLayer = LayerHelper.stack(7975L, ScaleLayer.NORMAL, climateLayer, 1, contextProvider);

        biomeLayer = SmoothLayer.INSTANCE.create(contextProvider.apply(402), biomeLayer);
        biomeLayer = MountainSmallEdgeLayer.INSTANCE.create(contextProvider.apply(1002L), biomeLayer, climateLayer);
        LayerFactory<T> zoom2BiomeLayer = BiomeVariantLayer.INSTANCE.create(contextProvider.apply(632L), biomeLayer);
        biomeLayer = LayerHelper.stack(7979L, ScaleLayer.NORMAL, zoom2BiomeLayer, 3, contextProvider);

        // 1.5x zoom
        biomeLayer = sizeLayer(contextProvider, biomeLayer);
        // Total: 2^6.5 (1 + 1 + 1 + 2 + 1.5)

        //Initialize special biomes
        LayerFactory<T> specialBiomesLayer = PlainsOnlyLayer.INSTANCE.create(contextProvider.apply(3L));

        specialBiomesLayer = BigSpecialBiomesLayer.INSTANCE.create(contextProvider.apply(38L), specialBiomesLayer, initialBiomeLayer);
        specialBiomesLayer = LayerHelper.stack(3043L, ScaleLayer.NORMAL, specialBiomesLayer, 2, contextProvider);

        specialBiomesLayer = SmallSpecialBiomesLayer.INSTANCE.create(contextProvider.apply(32L), specialBiomesLayer, zoom2BiomeLayer);
        specialBiomesLayer = BiomeVariantLayer.INSTANCE.create(contextProvider.apply(632L), specialBiomesLayer);

        specialBiomesLayer = LayerHelper.stack(3080L, ScaleLayer.NORMAL, specialBiomesLayer, 5, contextProvider);

        specialBiomesLayer = BiomeEdgeLayer.INSTANCE.create(contextProvider.apply(36L), specialBiomesLayer);
        specialBiomesLayer = BiomeEdgeEnsureLayer.INSTANCE.create(contextProvider.apply(37L), specialBiomesLayer);

        //River stuff
        LayerFactory<T> riverLayer = SimpleLandNoiseLayer.INSTANCE.create(contextProvider.apply(100L), LayerHelper.stack(1000L, ScaleLayer.NORMAL, layerFactory, 0, contextProvider));
        riverLayer = LayerHelper.stack(1000L, ScaleLayer.NORMAL, riverLayer, 7, contextProvider);
        riverLayer = GenerateRiversLayer.INSTANCE.create(contextProvider.apply(1L), riverLayer);
        riverLayer = LayerHelper.stack(3000L, ScaleLayer.NORMAL, riverLayer, 2, contextProvider);

        // merge rivers
        biomeLayer = ApplyRiversLayer.INSTANCE.create(contextProvider.apply(79L), biomeLayer, riverLayer);

        //Merge special biomes with the regular biome layer
        biomeLayer = SpecialBiomeMerger.INSTANCE.create(contextProvider.apply(65L), biomeLayer, specialBiomesLayer);

        //Merge biomes onto the continent
        layerFactory = BiomeMergeLayer.INSTANCE.create(contextProvider.apply(84L), layerFactory, biomeLayer);
        return layerFactory;
    }

    private static <T extends LayerSampler, C extends LayerSampleContext<T>> LayerFactory<T> sizeLayer(LongFunction<C> contextProvider, LayerFactory<T> biomeLayer) {
        int size = Ecotones.CONFIG.biomeSize;
        if (size == 0) { // / 1
            return biomeLayer;
        } else if (size == 1) { // / 2
            return ScaleLayer.NORMAL.create(contextProvider.apply(32), biomeLayer);
        } else if (size == 3) { // / 4
            return ScaleLayer.NORMAL.create(contextProvider.apply(31), ScaleLayer.NORMAL.create(contextProvider.apply(32), biomeLayer));
        } else if (size == 4) { // / 6
            return Div3ScaleLayer.INSTANCE.create(contextProvider.apply(31), ScaleLayer.NORMAL.create(contextProvider.apply(32), biomeLayer));
        } else { // / 3
            return Div3ScaleLayer.INSTANCE.create(contextProvider.apply(32), biomeLayer);
        }
    }

    public static BiomeLayerSampler build(long seed) {
        LayerFactory<CachingLayerSampler> layerFactory = build(seed, (salt) -> new CachingLayerContext(25, seed, salt));
        return new BiomeLayerSampler(layerFactory);
    }
}
