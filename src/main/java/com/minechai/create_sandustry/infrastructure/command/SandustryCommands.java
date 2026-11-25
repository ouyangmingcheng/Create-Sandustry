package com.minechai.create_sandustry.infrastructure.command;

import java.util.Collection;

import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import com.minechai.create_sandustry.content.sandmastersword.SandSwordHealthPenalty;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SandustryCommands {
    private SandustryCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("createsandustry")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("clear_sword_penalty")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(context -> clearPenalty(
                                        context.getSource(),
                                        EntityArgument.getPlayers(context, "targets")))));
        dispatcher.register(root);
    }

    private static int clearPenalty(CommandSourceStack source, Collection<ServerPlayer> targets) {
        int cleared = 0;
        for (ServerPlayer player : targets) {
            if (SandSwordHealthPenalty.getTotalPenalty(player) > 0) {
                cleared++;
            }
            player.setData(ModAttachments.SAND_SWORD_HEALTH_PENALTY, 0);
            SandSwordHealthPenalty.refresh(player);
            SandHeartManager.refresh(player);
        }
        final int clearedCount = cleared;
        source.sendSuccess(() -> Component.translatable(
                "commands.createsandustry.clear_sword_penalty.success", clearedCount), true);
        return clearedCount;
    }
}
