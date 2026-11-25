package com.minechai.create_sandustry.compat.createstuffadditions;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Extends Create: Stuff 'N Additions' Digging enchantment to shovel-mineable blocks.
 *
 * <p>The upstream procedure only mines a 3x3 plane when the broken block is in
 * {@code minecraft:mineable/pickaxe}, and only for horizontal block faces. This
 * compatibility layer applies the same fluid cost and plane logic to
 * {@code minecraft:mineable/shovel} blocks, including the top and bottom faces
 * that are most commonly used when digging sand, dirt, and gravel.</p>
 */
public final class CreateStuffAdditionsDiggingCompat {
    private static final ResourceLocation PORTABLE_DRILL_ID =
            ResourceLocation.fromNamespaceAndPath("create_sa", "portable_drill");
    private static final ResourceKey<Enchantment> DIGGING_ENCHANTMENT =
            ResourceKey.create(Registries.ENCHANTMENT,
                    ResourceLocation.fromNamespaceAndPath("create_sa", "digging"));
    private static final int FLUID_COST = 10;

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getAbilities().instabuild || player.isShiftKeyDown()) {
            return;
        }

        ItemStack drill = player.getMainHandItem();
        if (!PORTABLE_DRILL_ID.equals(BuiltInRegistries.ITEM.getKey(drill.getItem()))) {
            return;
        }
        // FE mode has its own per-block calculation, including Digging and Chunk Eater extras.
        if (FlorinolToolEnergy.hasBinding(drill)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var digging = enchantments.get(DIGGING_ENCHANTMENT).orElse(null);
        if (digging == null || drill.getEnchantmentLevel(digging) <= 0) {
            return;
        }

        BlockState originState = event.getState();
        if (!originState.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return;
        }

        Direction face = getHitFace(player);
        if (face == null) {
            return;
        }

        // Upstream already handles horizontal pickaxe faces. Avoid doing the same
        // work twice for blocks that belong to both tool tags.
        if (originState.is(BlockTags.MINEABLE_WITH_PICKAXE) && face.getAxis() != Direction.Axis.Y) {
            return;
        }

        BlockPos origin = event.getPos();
        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                if (first == 0 && second == 0) {
                    continue;
                }
                BlockPos extraPos = switch (face.getAxis()) {
                    case X -> origin.offset(0, first, second);
                    case Y -> origin.offset(first, 0, second);
                    case Z -> origin.offset(first, second, 0);
                };
                if (!breakExtraShovelBlock(level, player, drill, extraPos)) {
                    return;
                }
            }
        }
    }

    private static Direction getHitFace(Player player) {
        HitResult hit = player.pick(5.0D, 1.0F, false);
        return hit.getType() == HitResult.Type.BLOCK
                ? ((BlockHitResult) hit).getDirection()
                : null;
    }

    private static boolean breakExtraShovelBlock(ServerLevel level, ServerPlayer player,
                                                 ItemStack drill, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return true;
        }
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F || hardness >= 50.0F) {
            return true;
        }
        if (!hasFluid(drill, Fluids.WATER) || !hasFluid(drill, Fluids.LAVA)) {
            return false;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        Block.dropResources(state, level, pos, blockEntity, player, drill);
        if (!level.destroyBlock(pos, false)) {
            return true;
        }

        drainFluid(drill, Fluids.WATER);
        drainFluid(drill, Fluids.LAVA);
        return true;
    }

    private static boolean hasFluid(ItemStack stack, Fluid fluid) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return false;
        }
        FluidStack drained = handler.drain(new FluidStack(fluid, FLUID_COST), FluidAction.SIMULATE);
        return drained.getAmount() == FLUID_COST;
    }

    private static void drainFluid(ItemStack stack, Fluid fluid) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            handler.drain(new FluidStack(fluid, FLUID_COST), FluidAction.EXECUTE);
        }
    }

    private CreateStuffAdditionsDiggingCompat() {
    }
}