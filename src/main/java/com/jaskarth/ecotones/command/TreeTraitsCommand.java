package com.jaskarth.ecotones.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import com.jaskarth.ecotones.world.worldgen.gen.EcotonesChunkGenerator;
import com.jaskarth.ecotones.world.worldgen.tree.trait.Trait;
import com.jaskarth.ecotones.world.worldgen.tree.trait.TraitContainer;
import com.jaskarth.ecotones.world.worldgen.tree.trait.TreeTraitRegistry;

public class TreeTraitsCommand {
    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> {
            LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.literal("treetrait").requires(source ->
                    source.hasPermissionLevel(2));

            builder.then(CommandManager.argument("trait", IdentifierArgumentType.identifier())
                            .suggests((ctx, cb) -> CommandSource.suggestIdentifiers(TreeTraitRegistry.getKeys(), cb))
                            .executes(context -> execute(context.getSource(), context.getArgument("trait", Identifier.class))));

            dispatcher.register(builder);
        });
    }

    private static int execute(ServerCommandSource source, Identifier trait) {
        ChunkGenerator generator = source.getWorld().getChunkManager().getChunkGenerator();
        if (generator instanceof EcotonesChunkGenerator) {
            TraitContainer<? extends Trait> container = TreeTraitRegistry.get(trait);
            Vec3d vec = source.getPosition();
            Trait treeTrait = container.get((EcotonesChunkGenerator) generator, BlockPos.ofFloored(vec));

            source.sendMessage(Text.literal(container.getName() + " trait: " + treeTrait.name()));
        } else {
            source.sendMessage(Text.literal("This only works on ecotones worlds."));
        }
        return 0;
    }
}
