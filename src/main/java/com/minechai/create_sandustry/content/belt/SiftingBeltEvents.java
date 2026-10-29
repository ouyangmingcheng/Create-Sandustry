package com.minechai.create_sandustry.content.belt;

import com.minechai.create_sandustry.content.compat.create_connected.CreateConnectedCompat;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltSlope;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 机壳传送带 + 溜槽右键安装筛网，扳手右键拆除并返还溜槽。
 */
public final class SiftingBeltEvents {
    private SiftingBeltEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        Player player = event.getEntity();
        if (!player.mayBuild()) {
            return;
        }
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BeltBlock)) {
            return;
        }

        BeltBlockEntity belt = BeltHelper.getSegmentBE(level, event.getPos());
        if (belt == null) {
            return;
        }

        ItemStack held = event.getItemStack();
        boolean installed = belt.hasData(ModAttachments.SIFTING_BELT)
                && belt.getData(ModAttachments.SIFTING_BELT);
        boolean smartInstalled = belt.hasData(ModAttachments.SIFTING_BELT_BRASS)
                && belt.getData(ModAttachments.SIFTING_BELT_BRASS);
        boolean connectedBrassInstalled = belt.hasData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS)
                && belt.getData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);

        if (installed && AllItems.WRENCH.isIn(held)) {
            if (!level.isClientSide) {
                belt.removeData(ModAttachments.SIFTING_BELT);
                belt.removeData(ModAttachments.SIFTING_BELT_BRASS);
                belt.removeData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
                belt.setChanged();
                belt.syncData(ModAttachments.SIFTING_BELT);
                belt.syncData(ModAttachments.SIFTING_BELT_BRASS);
                belt.syncData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
                belt.notifyUpdate();
                if (!player.getAbilities().instabuild) {
                    ItemStack returned = connectedBrassInstalled
                            ? CreateConnectedCompat.brassChuteStack()
                            : smartInstalled ? AllBlocks.SMART_CHUTE.asStack() : AllBlocks.CHUTE.asStack();
                    if (returned.isEmpty()) {
                        returned = AllBlocks.CHUTE.asStack();
                    }
                    player.getInventory().placeItemBackInInventory(returned);
                }
                level.playSound(null, event.getPos(), SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.6F, 1.15F);
                player.displayClientMessage(
                        Component.translatable("message.createsandustry.sifting_belt.removed"), true);
            }
            cancel(event);
            return;
        }

        boolean normalChute = AllBlocks.CHUTE.isIn(held);
        boolean smartChute = AllBlocks.SMART_CHUTE.isIn(held);
        boolean connectedBrassChute = CreateConnectedCompat.isBrassChute(held);
        if (!normalChute && !smartChute && !connectedBrassChute) {
            return;
        }

        // 潜行右键或点击传送带底面时，允许照常放置溜槽。
        // 这样可以在已安装筛网的带段下方接溜槽/容器。
        if (player.isShiftKeyDown() || event.getFace() == Direction.DOWN) {
            return;
        }

        if (state.getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.createsandustry.sifting_belt.requires_horizontal"), true);
            }
            cancel(event);
            return;
        }

        if (installed) {
            if (!level.isClientSide) {
                player.displayClientMessage(
                        Component.translatable("message.createsandustry.sifting_belt.already_installed"), true);
            }
            cancel(event);
            return;
        }

        if (!state.getValue(BeltBlock.CASING) || !BeltBlock.canTransportObjects(state)) {
            return;
        }

        if (!level.isClientSide) {
            belt.setData(ModAttachments.SIFTING_BELT, true);
            belt.removeData(ModAttachments.SIFTING_BELT_BRASS);
            belt.removeData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
            if (smartChute) {
                belt.setData(ModAttachments.SIFTING_BELT_BRASS, true);
            } else if (connectedBrassChute) {
                belt.setData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS, true);
            }
            belt.setChanged();
            belt.syncData(ModAttachments.SIFTING_BELT);
            belt.syncData(ModAttachments.SIFTING_BELT_BRASS);
            belt.syncData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
            belt.notifyUpdate();
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            level.playSound(null, event.getPos(), SoundEvents.ANVIL_PLACE, SoundSource.BLOCKS, 0.7F, 1.05F);
            player.displayClientMessage(
                    Component.translatable("message.createsandustry.sifting_belt.installed"), true);
        }
        cancel(event);
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }
}
