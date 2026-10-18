package com.minechai.create_candustry.item;

import com.minechai.create_candustry.entity.ModEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public class LivingSandItem extends BlockItem {
    public LivingSandItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        return spawnEntityOn(context);
    }

    private InteractionResult spawnEntityOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockPos spawnPos = level.getBlockState(clickedPos).getCollisionShape(level, clickedPos).isEmpty()
                ? clickedPos
                : clickedPos.relative(context.getClickedFace());

        var entity = ModEntityTypes.LIVING_SAND.get().spawn(
                serverLevel,
                context.getItemInHand(),
                player,
                spawnPos,
                MobSpawnType.SPAWN_EGG,
                false,
                false);
        if (entity == null) {
            return InteractionResult.FAIL;
        }

        consume(player, context.getItemInHand());
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(stack);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        Vec3 spawnCenter = player.getEyePosition().add(player.getLookAngle().scale(2.0D));
        BlockPos spawnPos = BlockPos.containing(spawnCenter.x, spawnCenter.y, spawnCenter.z);
        var entity = ModEntityTypes.LIVING_SAND.get().spawn(
                serverLevel,
                stack,
                player,
                spawnPos,
                MobSpawnType.SPAWN_EGG,
                false,
                false);
        if (entity == null) {
            return InteractionResultHolder.fail(stack);
        }

        consume(player, stack);
        serverLevel.gameEvent(player, GameEvent.ENTITY_PLACE, entity.position());
        return InteractionResultHolder.consume(stack);
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.isCreative()) {
            stack.shrink(1);
        }
    }
}
