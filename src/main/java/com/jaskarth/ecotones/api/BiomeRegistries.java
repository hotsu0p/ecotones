package com.jaskarth.ecotones.api;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.gen.structure.Structure;
import com.jaskarth.ecotones.Ecotones;
import com.jaskarth.ecotones.world.worldgen.biome.EarlyBiomeRegistry;
import com.jaskarth.ecotones.world.worldgen.river.deco.RiverDecorationCollector;
import com.jaskarth.ecotones.world.worldgen.river.deco.RiverDecorator;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public final class BiomeRegistries {
    public static final Map<RegistryKey<Biome>, IntFunction<Boolean>> SPECIAL_BIOMES = new HashMap<>();
    public static final Map<RegistryKey<Biome>, Integer> BIG_SPECIAL_BIOMES = new HashMap<>();
    public static final Map<RegistryKey<Biome>, Integer> SMALL_SPECIAL_BIOMES = new HashMap<>();
    public static final List<RegistryKey<Biome>> BEACH_LIST = new ArrayList<>();
    public static final List<RegistryKey<Biome>> NO_BEACH_BIOMES = new ArrayList<>();
    public static final List<RegistryKey<Biome>> NO_RIVER_BIOMES = new ArrayList<>();
    public static final Map<RegistryKey<Biome>, Double> MOUNTAIN_BIOMES = new HashMap<>();
    public static final Map<ClimateType, List<RegistryKey<Biome>>> TYPED_MOUNTAIN_BIOMES = new HashMap<>();
    static {
        TYPED_MOUNTAIN_BIOMES.put(ClimateType.MOUNTAIN_FOOTHILLS, new ArrayList<>());
        TYPED_MOUNTAIN_BIOMES.put(ClimateType.MOUNTAIN_FOOTHILLS_UPPER, new ArrayList<>());
        TYPED_MOUNTAIN_BIOMES.put(ClimateType.MOUNTAIN_PLAINS, new ArrayList<>());
        TYPED_MOUNTAIN_BIOMES.put(ClimateType.MOUNTAIN_PEAKS, new ArrayList<>());
    }

    public static final Map<RegistryKey<Biome>, Integer> BIOME_VARIANT_CHANCE = new HashMap<>();
    public static final Map<RegistryKey<Biome>, RegistryKey<Biome>[]> BIOME_VARIANTS = new HashMap<>();
    public static final Map<RegistryKey<Biome>, RiverDecorator> RIVER_DECORATORS = new HashMap<>();
    private static final Map<Biome, FeatureList> FEATURE_LISTS_PRE = new HashMap<>();
    public static final Map<RegistryKey<Biome>, FeatureList> FEATURE_LISTS = new HashMap<>();
    public static final Map<RegistryEntry<Structure>, List<Biome>> STRUCTURE_TO_BIOME_LIST = new HashMap<>();
    public static final Map<TagKey<Biome>, List<Biome>> TAG_TO_BIOMES = new HashMap<>();

    public static void registerSpecialBiome(Biome biome, IntFunction<Boolean> rule) {
        SPECIAL_BIOMES.put(key(biome), rule);
    }
    public static void registerAllSpecial(IntFunction<Boolean> rule, Biome... biomes) {
        for (Biome biome : biomes) {
            SPECIAL_BIOMES.put(key(biome), rule);
        }
    }

    public static void registerBigSpecialBiome(Biome biome, int chance) {
        BIG_SPECIAL_BIOMES.put(key(biome), chance);
    }

    public static void registerSmallSpecialBiome(Biome biome, int chance) {
        SMALL_SPECIAL_BIOMES.put(key(biome), chance);
    }

    public static void registerBiomeVariantChance(Biome biome, int chance) {
        BIOME_VARIANT_CHANCE.put(key(biome), chance);
    }

    public static void registerRiverDecorator(Biome biome, Consumer<RiverDecorationCollector> acceptor) {
        RiverDecorator decorator = RIVER_DECORATORS.getOrDefault(key(biome), new RiverDecorator());
        acceptor.accept(decorator.getDecorations());
        RIVER_DECORATORS.put(key(biome), decorator);
    }

    public static void registerBiomeVariants(Biome parent, Biome... variants) {
        registerBiomeVariants(parent, true, variants);
    }

    public static void registerBiomeVariants(Biome parent, boolean addSelf, Biome... variants) {
        RegistryKey<Biome>[] ids = new RegistryKey[variants.length + (addSelf ? 1 : 0)];
        for (int i = 0; i < variants.length; i++) {
            Biome b = variants[i];

            if (b == parent) {
                throw new IllegalStateException("Don't add parent as variant!");
            }

            ids[i] = key(b);
        }

        if (addSelf) {
            ids[variants.length] = key(parent);
        }

        BIOME_VARIANTS.put(key(parent), ids);
    }

    @Deprecated // Redundant
    public static void addMountainBiome(Biome biome) {
        addMountainBiome(biome, 1.0);
    }

    public static void addMountainBiome(Biome biome, double weight) {
        MOUNTAIN_BIOMES.put(key(biome), weight);
    }

    public static void addMountainType(ClimateType type, Biome biome) {
        if (type == ClimateType.REGULAR) {
            throw new IllegalArgumentException("Can't add mountain type of regular!");
        }

        TYPED_MOUNTAIN_BIOMES.get(type).add(key(biome));
    }

    public static void registerNoBeachBiome(Biome biome) {
        NO_BEACH_BIOMES.add(key(biome));
    }

    public static void registerBeach(Biome biome) {
        BEACH_LIST.add(key(biome));
    }

    public static void registerNoBeachBiomes(Biome... biomes) {
        for (Biome biome : biomes) {
            registerNoBeachBiome(biome);
        }
    }

    public static void registerNoRiverBiomes(Biome... biomes) {
        for (Biome biome : biomes) {
            registerNoRiverBiome(biome);
        }
    }

    public static void registerNoRiverBiome(Biome biome) {
        NO_RIVER_BIOMES.add(key(biome));
    }

    public static RegistryKey<Biome> key(Biome biome) {
        RegistryKey<Biome> key = keyOrNull(biome);

        if (key == null) {
            throw new IllegalStateException("Impossible state when trying to get biome key");
        }

        return key;
    }

    public static void associateTag(TagKey<Biome> s, Biome b) {
        TAG_TO_BIOMES.computeIfAbsent(s, k -> new ArrayList<>()).add(b);
    }

    public static void registerStructure(RegistryEntry<Structure> s, Biome b) {
        STRUCTURE_TO_BIOME_LIST.computeIfAbsent(s, k -> new ArrayList<>()).add(b);
    }

    public static void registerFeatureList(Biome b, FeatureList list) {
        FEATURE_LISTS_PRE.put(b, list);
    }

    public static void finalizeValues() {
        FEATURE_LISTS_PRE.forEach((b, list) -> FEATURE_LISTS.put(key(b), list));
    }

    public static RegistryKey<Biome> keyOrNull(Biome biome) {
        Optional<RegistryKey<Biome>> optional = EarlyBiomeRegistry.get(biome);

        if (optional.isEmpty()) {
            if (Ecotones.REGISTRY == null) {
                return BiomeKeys.PLAINS; // This really shouldn't exist!!
            }
        }

        return optional.orElseGet(() -> Ecotones.REGISTRY.getKey(biome)
                .orElse(null));
    }
}