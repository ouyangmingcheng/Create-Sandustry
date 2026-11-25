package com.minechai.create_sandustry.compat.createstuffadditions;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * FE-powered mode for the Create: Stuff 'N Additions portable drill.
 *
 * <p>When the drill has a Florinol battery binding, lava fuel is replaced by the
 * configured FE cost. No lava or water is consumed in FE mode. Every broken block,
 * including Digging and Chunk Eater extras, pays the same operation cost.</p>
 */
public final class PortableDrillFlorinolCompat {
    private static final ResourceKey<Enchantment> DIGGING =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("create_sa", "digging"));
    private static final ResourceKey<Enchantment> CHUNK_EATER =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("create_sa", "chunk_eater"));

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack drill = player.getMainHandItem();
        if (!FlorinolToolEnergy.isPortableDrill(drill) || !FlorinolToolEnergy.hasBinding(drill)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        int feCost = FlorinolToolEnergy.getOperationCost(drill);
        if (!canConsumeEnergy(level, drill, feCost)) {
            event.setCanceled(true);
            return;
        }
        payOperation(level, drill, feCost);

        if (player.isShiftKeyDown()) {
            return;
        }

        int chunkEaterLevel = getEnchantmentLevel(level, drill, CHUNK_EATER);
        if (chunkEaterLevel > 0) {
            mineChunkEater(level, player, drill, event.getPos(), event.getState(), chunkEaterLevel);
            return;
        }

        int diggingLevel = getEnchantmentLevel(level, drill, DIGGING);
        if (diggingLevel > 0) {
            mineDiggingPlane(level, player, drill, event.getPos());
        }
    }

    private static void mineDiggingPlane(ServerLevel level, ServerPlayer player,
                                         ItemStack drill, BlockPos origin) {
        BlockState originState = level.getBlockState(origin);
        if (!originState.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && !originState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return;
        }
        Direction face = getHitFace(player);
        if (face == null) {
            return;
        }
        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                if (first == 0 && second == 0) {
                    continue;
                }
                BlockPos pos = switch (face.getAxis()) {
                    case X -> origin.offset(0, first, second);
                    case Y -> origin.offset(first, 0, second);
                    case Z -> origin.offset(first, second, 0);
                };
                if (!breakExtraBlock(level, player, drill, pos,
                        state -> state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                                || state.is(BlockTags.MINEABLE_WITH_SHOVEL))) {
                    return;
                }
            }
        }
    }

    private static void mineChunkEater(ServerLevel level, ServerPlayer player, ItemStack drill,
                                       BlockPos origin, BlockState originState, int enchantLevel) {
        float originHardness = originState.getDestroySpeed(level, origin);
        if (originHardness < 0.0F || originHardness >= 50.0F) {
            return;
        }

        int budget = 2 + Math.round(Math.min(enchantLevel, 10) * 1.5F);
        int remaining = Math.max(0, budget - 1);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            queue.add(origin.relative(direction));
        }

        while (!queue.isEmpty() && remaining > 0) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != originState.getBlock()) {
                continue;
            }
            if (!breakExtraBlock(level, player, drill, pos,
                    candidate -> candidate.getBlock() == originState.getBlock())) {
                return;
            }
            remaining--;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next)) {
                    queue.addLast(next);
                }
            }
        }
    }

    private static boolean breakExtraBlock(ServerLevel level, ServerPlayer player, ItemStack drill,
                                           BlockPos pos, BlockStatePredicate predicate) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !predicate.test(state)) {
            return true;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness >= 50.0F) {
            return true;
        }
        int feCost = FlorinolToolEnergy.getOperationCost(drill);
        if (!canConsumeEnergy(level, drill, feCost)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block.dropResources(state, level, pos, blockEntity, player, drill);
        if (!level.destroyBlock(pos, false)) {
            return true;
        }
        payOperation(level, drill, feCost);
        return true;
    }

    private static boolean canConsumeEnergy(ServerLevel level, ItemStack drill, int feCost) {
        return FlorinolToolEnergy.canConsume(level, drill, feCost);
    }

    private static void payOperation(ServerLevel level, ItemStack drill, int feCost) {
        FlorinolToolEnergy.tryConsume(level, drill, feCost);
    }

    private static int getEnchantmentLevel(ServerLevel level, ItemStack stack,
                                           ResourceKey<Enchantment> key) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var enchantment = enchantments.get(key).orElse(null);
        return enchantment == null ? 0 : stack.getEnchantmentLevel(enchantment);
    }

    private static Direction getHitFace(Player player) {
        HitResult hit = player.pick(5.0D, 1.0F, false);
        return hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getDirection()
                : null;
    }

    @FunctionalInterface
    private interface BlockStatePredicate {
        boolean test(BlockState state);
    }

    private PortableDrillFlorinolCompat() {
    }
}