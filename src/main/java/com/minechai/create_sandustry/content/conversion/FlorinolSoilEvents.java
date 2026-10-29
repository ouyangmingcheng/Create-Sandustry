package com.minechai.create_sandustry.content.conversion;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModTags;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class FlorinolSoilEvents {
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Player player = event.getEntity();
        if (player.isSpectator()) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (stack.isEmpty() || !stack.is(ModTags.ItemTags.FLORINOL_SOIL_CATALYZER)) {
            return;
        }

        if (level.getBlockState(event.getPos()).is(ModBlocks.FLORINOL_SOIL.get())) {
            FlorinolSoilConversionManager.addTicker(level, event.getPos(), false, false);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            FlorinolSoilConversionManager.tick(level);
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        FlorinolSoilConversionManager.clearQueues(event.getServer());
    }

    private FlorinolSoilEvents() {
    }
}