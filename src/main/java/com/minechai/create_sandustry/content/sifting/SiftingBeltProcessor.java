package com.minechai.create_sandustry.content.sifting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.minechai.create_sandustry.content.sifting.SiftingRecipe;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity.CasingType;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour.TransportedResult;
import com.simibubi.create.content.kinetics.belt.transport.BeltInventory;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * 服务器端筛振传送带处理。
 *
 * <p>物品在传送带上正常移动，每个筛振段每 tick 最多推进一个物品的进度。
 * 进度未完成时只累计，不锁定物品；完成时才由当前段执行配方并输出副产物。</p>
 */
public final class SiftingBeltProcessor {
    private static final int NORMAL_CHUTE_MAX_BATCH = 16;
    private static final int CONNECTED_BRASS_MAX_BATCH = 64;
    private static final double MOUTH_OUTPUT_Y_OFFSET = 0.30D;
    private static final double MOUTH_PARTICLE_Y_OFFSET = 0.22D;

    private SiftingBeltProcessor() {
    }

    public static void tickSegment(BeltBlockEntity belt) {
        if (!(belt.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (!belt.hasData(ModAttachments.SIFTING_BELT)
                || !belt.getData(ModAttachments.SIFTING_BELT)) {
            return;
        }

        BlockState state = belt.getBlockState();
        if (!state.hasProperty(BeltBlock.CASING) || !state.getValue(BeltBlock.CASING)) {
            clearMesh(level, belt);
            return;
        }
        if (state.getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL) {
            return;
        }
        // 只有智能溜槽支持红石锁定；普通溜槽和 Create: Connected 黄铜溜槽不受红石影响。
        if (hasSmartChute(belt) && level.hasNeighborSignal(belt.getBlockPos())) {
            return;
        }
        if (belt.getSpeed() == 0 || !BeltBlock.canTransportObjects(state)) {
            return;
        }

        BeltBlockEntity controller = belt.getControllerBE();
        if (controller == null) {
            return;
        }
        BeltInventory inventory = controller.getInventory();
        if (inventory == null) {
            return;
        }

        Target target = findTarget(level, belt, inventory);
        if (target == null) {
            return;
        }

        TransportedItemStackHandlerBehaviour handler = BlockEntityBehaviour.get(
                level, belt.getBlockPos(), TransportedItemStackHandlerBehaviour.TYPE);
        if (handler == null) {
            return;
        }

        processTarget(level, belt, controller, handler, target);
    }

    private static Target findTarget(ServerLevel level, BeltBlockEntity segment, BeltInventory inventory) {
        TransportedItemStack bestItem = null;
        RecipeHolder<SiftingRecipe> bestRecipe = null;
        boolean bestOngoing = false;
        float bestDistance = Float.MAX_VALUE;
        float segmentCenter = segment.index + 0.5F;

        for (TransportedItemStack item : inventory.getTransportedItems()) {
            if (item.stack.isEmpty() || Mth.floor(item.beltPosition) != segment.index) {
                continue;
            }

            Optional<RecipeHolder<SiftingRecipe>> recipeHolder = findRecipe(level, item.stack);
            if (recipeHolder.isEmpty()) {
                continue;
            }

            SiftingProgressAccess progress = (SiftingProgressAccess) (Object) item;
            boolean ongoing = recipeHolder.get().id().equals(progress.getSiftingRecipeId())
                    && progress.getSiftingProgress() > 0;
            float distance = Math.abs(item.beltPosition - segmentCenter);

            if (bestItem == null
                    || ongoing && !bestOngoing
                    || ongoing == bestOngoing && distance < bestDistance) {
                bestItem = item;
                bestRecipe = recipeHolder.get();
                bestOngoing = ongoing;
                bestDistance = distance;
            }
        }

        return bestItem == null ? null : new Target(bestItem, bestRecipe);
    }

    private static void processTarget(
            ServerLevel level,
            BeltBlockEntity belt,
            BeltBlockEntity controller,
            TransportedItemStackHandlerBehaviour handler,
            Target target) {
        TransportedItemStack item = target.item();
        RecipeHolder<SiftingRecipe> recipeHolder = target.recipe();
        SiftingRecipe recipe = recipeHolder.value();
        ResourceLocation recipeId = recipeHolder.id();
        SiftingProgressAccess progress = (SiftingProgressAccess) (Object) item;

        int baseDuration = Math.max(1, recipe.getProcessingDuration());
        if (!recipeId.equals(progress.getSiftingRecipeId())
                || progress.getSiftingRequiredProgress() <= 0) {
            progress.setSiftingProgress(recipeId, 0, rollRequiredProgress(level, baseDuration));
        }

        int requiredProgress = Math.max(1, progress.getSiftingRequiredProgress());
        int progressStep = belt.casing == CasingType.BRASS
                ? ModConfigs.SIFTING_BRASS_PROGRESS_PER_TICK.get()
                : 1;
        int nextProgress = progress.getSiftingProgress() + progressStep;
        if (nextProgress < requiredProgress) {
            progress.setSiftingProgress(recipeId, nextProgress, requiredProgress);
            controller.setChanged();
            playProcessingParticles(level, handler, item);
            return;
        }

        int processingCount = hasSmartChute(belt)
                ? item.stack.getCount()
                : hasConnectedBrassChute(belt)
                    ? Math.min(item.stack.getCount(), CONNECTED_BRASS_MAX_BATCH)
                    : Math.min(item.stack.getCount(), NORMAL_CHUTE_MAX_BATCH);
        List<ItemStack> outputs = rollOutputs(recipe, processingCount, level);
        if (outputs.isEmpty() || outputs.getFirst().isEmpty()) {
            progress.clearSiftingProgress();
            controller.setChanged();
            return;
        }

        ItemStack onBelt = outputs.getFirst().copy();
        for (int i = 1; i < outputs.size(); i++) {
            ejectBelow(level, belt.getBlockPos(), outputs.get(i));
        }

        TransportedItemStack converted = item.copy();
        converted.stack = onBelt;
        ((SiftingProgressAccess) (Object) converted).clearSiftingProgress();

        ItemStack remainingInput = item.stack.copy();
        remainingInput.shrink(processingCount);
        if (remainingInput.isEmpty()) {
            handler.handleProcessingOnItem(item, TransportedResult.convertTo(converted));
        } else {
            TransportedItemStack remainder = item.copy();
            remainder.stack = remainingInput;
            ((SiftingProgressAccess) (Object) remainder).clearSiftingProgress();
            // Held outputs are forcibly moved to the segment center by BeltInventory.
            // Submit both as regular outputs so the remaining wet sand keeps its position.
            handler.handleProcessingOnItem(
                    item,
                    TransportedResult.convertTo(List.of(converted, remainder)));
        }

        controller.setChanged();
        playProcessFeedback(level, belt.getBlockPos());
    }

    private static Optional<RecipeHolder<SiftingRecipe>> findRecipe(ServerLevel level, ItemStack stack) {
        return level.getRecipeManager()
                .getRecipeFor(ModRecipeTypes.SIFTING.get(), new SingleRecipeInput(stack), level);
    }

    /**
     * 保留少量单件随机性，让完成段在长期运行中自然漂移。
     * 平均值仍为配方的 processing_time。
     */
    private static int rollRequiredProgress(ServerLevel level, int baseDuration) {
        float jitter = ModConfigs.SIFTING_PROGRESS_JITTER.get().floatValue();
        double factor = 1.0D + (level.random.nextDouble() * 2.0D - 1.0D) * jitter;
        return Math.max(1, (int) Math.round(baseDuration * factor));
    }

    /**
     * 按输入数量逐件掷出每个结果槽，再合并同类物品。
     * 保留结果列表索引，确保“第一个结果留在带上”的约定不受概率空结果影响。
     */
    private static List<ItemStack> rollOutputs(SiftingRecipe recipe, int inputCount, ServerLevel level) {
        var resultSlots = recipe.getRollableResults();
        List<ItemStack> totals = new ArrayList<>(resultSlots.size());
        for (var resultSlot : resultSlots) {
            ItemStack total = ItemStack.EMPTY;
            for (int i = 0; i < inputCount; i++) {
                total = merge(total, resultSlot.rollOutput(level.random));
            }
            totals.add(total);
        }
        return totals;
    }

    private static ItemStack merge(ItemStack total, ItemStack addition) {
        if (addition.isEmpty()) {
            return total;
        }
        if (total.isEmpty()) {
            return addition.copy();
        }
        if (ItemStack.isSameItemSameComponents(total, addition)) {
            total.grow(addition.getCount());
        }
        return total;
    }

    private static void ejectBelow(ServerLevel level, BlockPos beltPos, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        BlockPos outputPos = beltPos.below();
        IItemHandler handler = level.getCapability(
                Capabilities.ItemHandler.BLOCK, outputPos, Direction.UP);
        if (handler != null) {
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(handler, stack.copy(), false);
            if (remainder.isEmpty()) {
                return;
            }
            stack = remainder;
        }

        dropFromMouth(level, beltPos, stack);
    }

    private static void dropFromMouth(ServerLevel level, BlockPos beltPos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(
                level,
                beltPos.getX() + 0.5D,
                beltPos.getY() - MOUTH_OUTPUT_Y_OFFSET,
                beltPos.getZ() + 0.5D,
                stack);
        entity.setDeltaMovement(0.0D, -0.04D, 0.0D);
        entity.setPickUpDelay(10);
        level.addFreshEntity(entity);
    }

    private static void clearMesh(ServerLevel level, BeltBlockEntity belt) {
        belt.removeData(ModAttachments.SIFTING_BELT);
        belt.removeData(ModAttachments.SIFTING_BELT_BRASS);
        belt.removeData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
        belt.setChanged();
        belt.syncData(ModAttachments.SIFTING_BELT);
        belt.syncData(ModAttachments.SIFTING_BELT_BRASS);
        belt.syncData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
        belt.notifyUpdate();
    }

    private static boolean hasSmartChute(BeltBlockEntity belt) {
        return belt.hasData(ModAttachments.SIFTING_BELT_BRASS)
                && belt.getData(ModAttachments.SIFTING_BELT_BRASS);
    }

    private static boolean hasConnectedBrassChute(BeltBlockEntity belt) {
        return belt.hasData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS)
                && belt.getData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS);
    }

    private static void playProcessFeedback(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.85D;
        double z = pos.getZ() + 0.5D;
        level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.35F, 1.25F);
        double mouthY = pos.getY() - MOUTH_PARTICLE_Y_OFFSET;
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 5, 0.18D, 0.08D, 0.18D, 0.02D);
        level.sendParticles(ParticleTypes.CRIT, x, mouthY, z, 3, 0.08D, 0.03D, 0.08D, 0.01D);
    }

    private static void playProcessingParticles(
            ServerLevel level,
            TransportedItemStackHandlerBehaviour handler,
            TransportedItemStack item) {
        if (level.random.nextFloat() > 0.4F) {
            return;
        }

        Vec3 itemPos = handler.getWorldPositionOf(item);
        double x = itemPos.x;
        double y = itemPos.y + 0.42D;
        double z = itemPos.z;
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 1, 0.08D, 0.03D, 0.08D, 0.005D);
    }

    private record Target(TransportedItemStack item, RecipeHolder<SiftingRecipe> recipe) {
    }
}
