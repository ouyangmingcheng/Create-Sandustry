package com.minechai.create_candustry.interaction;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.simibubi.create.content.equipment.sandPaper.SandPaperItem;
import com.simibubi.create.content.equipment.sandPaper.SandPaperPolishingRecipe;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HardenedSandSandpaperInteraction {
    private static final Map<UUID, Long> LAST_HANDLED_TICK = new HashMap<>();

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled()) {
            return;
        }

        Player player = event.getEntity();
        if (player.isSpectator()) {
            return;
        }

        Level level = event.getLevel();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (isSandpaper(mainHand) && isPolishableBlockItem(level, offHand)) {
            handle(event, player, InteractionHand.MAIN_HAND, offHand);
        } else if (isSandpaper(offHand) && isPolishableBlockItem(level, mainHand)) {
            handle(event, player, InteractionHand.OFF_HAND, mainHand);
        }
    }

    private static void handle(PlayerInteractEvent.RightClickBlock event, Player player,
                               InteractionHand sandpaperHand, ItemStack polishableStack) {
        long now = player.level().getGameTime();
        UUID playerId = player.getUUID();
        Long lastTick = LAST_HANDLED_TICK.get(playerId);
        if (lastTick != null && lastTick == now) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        ItemStack sandpaperStack = player.getItemInHand(sandpaperHand);
        if (!(sandpaperStack.getItem() instanceof SandPaperItem sandPaperItem)) {
            return;
        }

        var result = sandPaperItem.use(player.level(), player, sandpaperHand);
        if (result.getResult() != InteractionResult.FAIL) {
            LAST_HANDLED_TICK.put(playerId, now);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static boolean isSandpaper(ItemStack stack) {
        return stack.getItem() instanceof SandPaperItem;
    }

    private static boolean isPolishableBlockItem(Level level, ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof BlockItem
                && SandPaperPolishingRecipe.canPolish(level, stack);
    }

    private HardenedSandSandpaperInteraction() {
    }
}
