package com.minechai.create_sandustry.content.conversion;

import com.simibubi.create.AllBlocks;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.IItemHandler;

/** Handles one-item player pickups from a waterlogged Create Depot. */
public final class MoonCrystalDepotEvents {
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (event.getFace() != Direction.UP) {
            return;
        }

        Player player = event.getEntity();
        if (player.isSpectator() || player instanceof FakePlayer
                || !player.getItemInHand(event.getHand()).isEmpty()) {
            return;
        }

        BlockState state = level.getBlockState(event.getPos());
        if (!state.is(AllBlocks.DEPOT.get())) {
            return;
        }

        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                event.getPos(),
                Direction.UP);
        if (handler == null) {
            return;
        }

        ItemStack held = handler.getStackInSlot(0);
        if (!MoonCrystalConversion.canConvert(level, event.getPos(), state, held)) {
            return;
        }

        ItemStack extracted = handler.extractItem(0, 1, false);
        if (extracted.isEmpty()) {
            return;
        }

        player.getInventory().placeItemBackInInventory(extracted);
        level.playSound(null, event.getPos(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS,
                0.2F, 1.0F + level.getRandom().nextFloat());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private MoonCrystalDepotEvents() {
    }
}
