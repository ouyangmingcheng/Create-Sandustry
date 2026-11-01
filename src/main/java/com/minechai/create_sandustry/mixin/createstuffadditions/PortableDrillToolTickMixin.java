package com.minechai.create_sandustry.mixin.createstuffadditions;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;

import net.mcreator.createstuffadditions.procedures.PortableDrillToolInHandTickProcedure;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the upstream lava-empty mining-fatigue check while FE mode is bound. */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.procedures.PortableDrillToolInHandTickProcedure", remap = false)
public abstract class PortableDrillToolTickMixin {
    @Inject(method = "execute", at = @At("HEAD"), cancellable = true)
    private static void createsandustry$skipFluidTickInFeMode(Entity entity, ItemStack stack,
                                                              CallbackInfo ci) {
        if (FlorinolToolEnergy.hasBinding(stack)) {
            ci.cancel();
        }
    }
}