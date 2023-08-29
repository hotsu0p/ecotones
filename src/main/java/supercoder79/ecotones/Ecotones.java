package supercoder79.ecotones;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import supercoder79.ecotones.advancement.EcotonesCriteria;
import supercoder79.ecotones.api.BiomeRegistries;
import supercoder79.ecotones.api.ModCompatRunner;
import supercoder79.ecotones.blocks.EcotonesBlocks;
import supercoder79.ecotones.blocks.entity.EcotonesBlockEntities;
import supercoder79.ecotones.client.particle.EcotonesParticles;
import supercoder79.ecotones.client.sound.EcotonesSounds;
import supercoder79.ecotones.command.*;
import supercoder79.ecotones.config.ConfigReader;
import supercoder79.ecotones.config.ConfigSpec;
import supercoder79.ecotones.entity.EcotonesEntities;
import supercoder79.ecotones.gen.DataGen;
import supercoder79.ecotones.items.EcotonesItemGroups;
import supercoder79.ecotones.items.EcotonesItems;
import supercoder79.ecotones.recipe.EcotonesRecipes;
import supercoder79.ecotones.screen.EcotonesScreenHandlers;
import supercoder79.ecotones.util.*;
import supercoder79.ecotones.util.compat.*;
import supercoder79.ecotones.util.deco.BlockDecorations;
import supercoder79.ecotones.util.register.EarlyRegistrationState;
import supercoder79.ecotones.util.state.EcotonesBlockStateProviders;
import supercoder79.ecotones.util.vein.OreVeins;
import supercoder79.ecotones.world.EcotonesWorldType;
import supercoder79.ecotones.world.biome.EarlyBiomeRegistry;
import supercoder79.ecotones.world.biome.EcotonesBiomeBuilder;
import supercoder79.ecotones.world.biome.EcotonesBiomes;
import supercoder79.ecotones.world.data.EcotonesData;
import supercoder79.ecotones.world.decorator.EcotonesDecorators;
import supercoder79.ecotones.world.edge.EcotonesEdgeDecorations;
import supercoder79.ecotones.world.features.EcotonesFeatures;
import supercoder79.ecotones.world.features.foliage.EcotonesFoliagePlacers;
import supercoder79.ecotones.world.gen.BiomeGenData;
import supercoder79.ecotones.world.gen.EcotonesBiomeSource;
import supercoder79.ecotones.world.gen.EcotonesChunkGenerator;
import supercoder79.ecotones.world.structure.*;
import supercoder79.ecotones.world.structure.EcotonesStructures;
import supercoder79.ecotones.world.surface.EcotonesSurfaces;
import supercoder79.ecotones.world.tree.trait.EcotonesTreeTraits;
import supercoder79.ecotones.world.treedecorator.EcotonesTreeDecorators;

public final class Ecotones implements ModInitializer {
	public static final ConfigSpec CONFIG = ConfigReader.read();
	private static final boolean RUN_DATA_GEN = "true".equals(System.getProperty("ECOTONES_RUN_DATAGEN", null));

	public static final Identifier WORLD_TYPE = new Identifier("ecotones", "world_type");

	public static final Logger LOGGER = LogManager.getLogger("ecotones");

	// Dynamic registry
	public static Registry<Biome> REGISTRY;
	public static boolean isServerEcotones = false;

	@Override
	public void onInitialize() {
		long start = System.currentTimeMillis();
		EarlyRegistrationState.globalBuiltins = BuiltinRegistries.createWrapperLookup();

		EcotonesCriteria.init();
		EcotonesSounds.init();

		EcotonesParticles.init();
		EcotonesBlockStateProviders.init();
		EcotonesFoliagePlacers.init();
		EcotonesTreeDecorators.init();

		EcotonesTreeTraits.init();

		EcotonesItemGroups.init();
		EcotonesScreenHandlers.init();

		EcotonesBlocks.init();
		EcotonesBlockEntities.init();
		EcotonesItems.init();

		EcotonesRecipes.init();

		EcotonesDecorators.init();
		OreVeins.init();
		EcotonesFeatures.init();
		EcotonesSurfaces.init();

		EcotonesStructurePieces.init();
		EcotonesStructureTypes.init();
		EcotonesStructures.init();
		EcotonesStructureSets.init();

		EcotonesEntities.init();

		ModCompat.initEarly();

		EcotonesBiomes.init();

		EcotonesEdgeDecorations.init();

		EcotonesData.init();

		EcotonesFuels.init();
		EcotonesComposting.init();

		CampfireLogHelper.initVanilla();
		BlockDecorations.init();

		if (RUN_DATA_GEN) {
			DataGen.run();
		}

		ModCompat.initLate();

		AiLog.init();
		AiLog.log("[System] Starting AI log");

		GetDataAtCommand.init();
		TreeTraitsCommand.init();

		// Dev only commands
		if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
			MapSoilQualityCommand.init();
			MapSoilPhCommand.init();
			DumpClimatesCommand.init();
			TestTranslationKeysCommand.init();
			MapClimatesCommand.init();
			MapMountainsCommand.init();
			MapBiomeColorsCommand.init();
			TestLootTablesCommand.init();
			MapRiversCommand.init();
		}

		// Biome count summary and biome finalization
		int ecotonesBiomes = 0;
		BiomeRegistries.finalizeValues();
		for (Identifier id : EarlyBiomeRegistry.ids()) {
			if (id.getNamespace().contains("ecotones")) {
				Biome biome = EarlyBiomeRegistry.get(id);
				BiomeGenData data = EcotonesBiomeBuilder.OBJ2DATA.get(biome);

				RegistryKey<Biome> key = EarlyBiomeRegistry.get(biome).get();
				BiomeGenData.LOOKUP.put(key, data);
				if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
					BiomeChecker.check(biome);
				}

				ecotonesBiomes++;
			}
		}

		VanillaBiomeData.init();

		log("Registering " + ecotonesBiomes + " ecotones biomes!");
		RegistryReport.report(ecotonesBiomes);

		// register chunk generator and world type
		Registry.register(Registries.BIOME_SOURCE, new Identifier("ecotones", "ecotones"), EcotonesBiomeSource.CODEC);
		Registry.register(Registries.CHUNK_GENERATOR, new Identifier("ecotones", "ecotones"), EcotonesChunkGenerator.CODEC);

		log("Ecotones init took " + (System.currentTimeMillis() - start) + "ms!");
	}

	public static Identifier id(String name) {
		return new Identifier("ecotones", name);
	}

	public static void log(String str) {
		LOGGER.info("[ecotones] " + str);
	}

	public static boolean isModLoaded(String modid) {
		return FabricLoader.getInstance().isModLoaded(modid);
	}
}