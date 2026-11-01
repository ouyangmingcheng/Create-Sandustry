package com.minechai.create_sandustry.mixin.createstuffadditions;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;

import net.mcreator.createstuffadditions.procedures.PortableDrillBlockDestroyedWithToolProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** FE mode is handled by PortableDrillFlorinolCompat before the block is removed. */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.procedures.PortableDrillBlockDestroyedWithToolProcedure", remap = false)
public abstract class PortableDrillBlockDestroyedMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void createsandustry$skipFluidProcedureInFeMode(LevelAccessor world,
                                                                   double x, double y, double z,
                                                                   BlockState state, Entity entity,
                                                                   ItemStack stack, CallbackInfo ci) {
        if (FlorinolToolEnergy.hasBinding(stack)) {
            ci.cancel();
        }
    }
}