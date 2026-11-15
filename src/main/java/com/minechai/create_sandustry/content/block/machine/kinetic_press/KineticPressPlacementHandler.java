package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import com.minechai.create_sandustry.registry.ModTags;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Non-destructive quick placement for a bound input chute. */
public final class KineticPressPlacementHandler {
    private KineticPressPlacementHandler() {
    }

    public static boolean tryPlace(Level level, KineticPressBlockEntity press, Player player,
                                   InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide || !(player instanceof ServerPlayer)) {
            return false;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.is(ModTags.ItemTags.KINETIC_PRESS_INPUTS)) {
            return false;
        }
        if (!(stack.getItem() instanceof BlockItem blockItem)
                || !blockItem.getBlock().defaultBlockState().is(ModTags.BlockTags.KINETIC_PRESS_INPUTS)) {
            return fail(player, "invalid_input");
        }

        BlockPos targetPos = press.getInputPosition();
        if (level.isOutsideBuildHeight(targetPos.getY())) {
            return fail(player, "out_of_world");
        }
        if (!level.mayInteract(player, targetPos)
                || !player.mayUseItemAt(targetPos, Direction.DOWN, stack)) {
            return fail(player, "no_permission");
        }
        if (!level.getBlockState(targetPos).isAir()) {
            return fail(player, "target_occupied");
        }
        for (int offset = 1; offset < press.getWorkingHeight(); offset++) {
            BlockPos pathPos = press.getBlockPos().above(offset);
            if (!level.getBlockState(pathPos).isAir()) {
                player.displayClientMessage(Component.translatable(
                        "message.createsandustry.kinetic_press.path_blocked",
                        pathPos.toShortString()).withStyle(ChatFormatting.RED), true);
                return false;
            }
        }

        BlockPlaceContext original = new BlockPlaceContext(player, hand, stack, hitResult);
        BlockPlaceContext placement = BlockPlaceContext.at(original, targetPos, Direction.DOWN);
        BlockState preview = blockItem.getBlock().getStateForPlacement(placement);
        if (preview == null
                || !preview.canSurvive(level, targetPos)
                || !level.isUnobstructed(preview, targetPos, CollisionContext.of(player))) {
            return fail(player, "placement_blocked");
        }

        InteractionResult result = blockItem.place(placement);
        if (!result.consumesAction()
                || !level.getBlockState(targetPos).is(ModTags.BlockTags.KINETIC_PRESS_INPUTS)) {
            return fail(player, "placement_failed");
        }

        press.bindInput(targetPos, level.getBlockState(targetPos));
        press.requestStructureCheck();
        player.displayClientMessage(Component.translatable(
                "message.createsandustry.kinetic_press.placed",
                level.getBlockState(targetPos).getBlock().getName()), true);
        return true;
    }

    private static boolean fail(Player player, String reason) {
        player.displayClientMessage(Component.translatable(
                "message.createsandustry.kinetic_press." + reason).withStyle(ChatFormatting.RED), true);
        return false;
    }
}