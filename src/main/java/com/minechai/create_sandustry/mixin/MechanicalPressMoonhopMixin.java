package com.minechai.create_sandustry.mixin;

import java.util.List;

import com.minechai.create_sandustry.content.moonhop.MoonhopPressLogic;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the dynamic Y-accumulation process to Create's existing mechanical press paths. */
@Mixin(MechanicalPressBlockEntity.class)
public abstract class MechanicalPressMoonhopMixin {
    @Inject(method = "tryProcessOnBelt", at = @At("HEAD"), cancellable = true)
    private void createsandustry$processMoonhopOnBelt(TransportedItemStack input, List<ItemStack> outputList,
                                                       boolean simulate, CallbackInfoReturnable<Boolean> cir) {
        MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) (Object) this;
        Level level = press.getLevel();
        if (level == null || input == null || !MoonhopPressLogic.isMoonhopInput(input.stack)) {
            return;
        }

        MoonhopPressLogic.Result result = MoonhopPressLogic.evaluate(
                input.stack, level.dimension(), press.getBlockPos().getY());
        if (!result.startsProcessing()) {
            cir.setReturnValue(false);
            return;
        }
        if (simulate) {
            cir.setReturnValue(true);
            return;
        }

        if (level.isClientSide()) {
            cir.setReturnValue(false);
            return;
        }

        int processedCount = MoonhopPressLogic.processedCount(input.stack, press.canProcessInBulk());
        if (processedCount <= 0) {
            cir.setReturnValue(false);
            return;
        }

        List<ItemStack> outputs = MoonhopPressLogic.createOutputs(input.stack, result, processedCount);
        if (result.type() == MoonhopPressLogic.ResultType.DESTROY) {
            createsandustry$playDestroySound(level, press);
        }
        if (outputList != null) {
            outputList.addAll(outputs);
        }
        if (!outputs.isEmpty()) {
            press.onItemPressed(outputs.getFirst());
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "tryProcessInWorld", at = @At("HEAD"), cancellable = true)
    private void createsandustry$processMoonhopInWorld(ItemEntity itemEntity, boolean simulate,
                                                        CallbackInfoReturnable<Boolean> cir) {
        MechanicalPressBlockEntity press = (MechanicalPressBlockEntity) (Object) this;
        Level level = press.getLevel();
        if (level == null || itemEntity == null || !MoonhopPressLogic.isMoonhopInput(itemEntity.getItem())) {
            return;
        }

        ItemStack input = itemEntity.getItem();
        MoonhopPressLogic.Result result = MoonhopPressLogic.evaluate(
                input, level.dimension(), press.getBlockPos().getY());
        if (!result.startsProcessing()) {
            cir.setReturnValue(false);
            return;
        }
        if (simulate) {
            cir.setReturnValue(true);
            return;
        }

        if (level.isClientSide()) {
            cir.setReturnValue(false);
            return;
        }

        int processedCount = MoonhopPressLogic.processedCount(input, press.canProcessInBulk());
        if (processedCount <= 0) {
            cir.setReturnValue(false);
            return;
        }

        List<ItemStack> outputs = MoonhopPressLogic.createOutputs(input, result, processedCount);
        if (result.type() == MoonhopPressLogic.ResultType.DESTROY) {
            createsandustry$playDestroySound(level, press);
        }
        createsandustry$applyWorldOutputs(level, itemEntity, outputs, processedCount);
        if (!outputs.isEmpty()) {
            press.onItemPressed(outputs.getFirst());
        }
        cir.setReturnValue(true);
    }

    @Unique
    private static void createsandustry$playDestroySound(Level level, MechanicalPressBlockEntity press) {
        level.playSound(null, press.getBlockPos(), SoundEvents.ITEM_BREAK, SoundSource.PLAYERS,
                0.8F, 0.8F + level.random.nextFloat() * 0.4F);
    }

    @Unique
    private static void createsandustry$applyWorldOutputs(Level level, ItemEntity sourceEntity,
                                                          List<ItemStack> outputs, int processedCount) {
        ItemStack source = sourceEntity.getItem();
        int consumed = Math.min(processedCount, source.getCount());

        if (consumed < source.getCount()) {
            source.shrink(consumed);
            createsandustry$spawnAdditionalOutputs(level, sourceEntity, outputs, 0);
            return;
        }

        if (outputs.isEmpty()) {
            sourceEntity.discard();
            return;
        }

        sourceEntity.setItem(outputs.getFirst());
        createsandustry$spawnAdditionalOutputs(level, sourceEntity, outputs, 1);
    }

    @Unique
    private static void createsandustry$spawnAdditionalOutputs(Level level, ItemEntity sourceEntity,
                                                               List<ItemStack> outputs, int startIndex) {
        for (int i = startIndex; i < outputs.size(); i++) {
            ItemStack output = outputs.get(i);
            ItemEntity created = new ItemEntity(
                    level, sourceEntity.getX(), sourceEntity.getY(), sourceEntity.getZ(), output);
            created.setDefaultPickUpDelay();
            created.setDeltaMovement(VecHelper.offsetRandomly(Vec3.ZERO, level.random, 0.05F));
            level.addFreshEntity(created);
        }
    }
}
