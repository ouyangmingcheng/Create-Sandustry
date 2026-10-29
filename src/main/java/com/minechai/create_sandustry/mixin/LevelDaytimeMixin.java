package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.timebearing.TimeBearingService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelDaytimeMixin {
    @Inject(method = "advanceDaytime", at = @At("RETURN"), cancellable = true)
    private void createsandustry$applyTimeBearingAdvance(CallbackInfoReturnable<Long> cir) {
        if ((Object) this instanceof ServerLevel serverLevel) {
            long advance = TimeBearingService.tryAdvanceDaytime(serverLevel);
            if (advance >= 0L) {
                cir.setReturnValue(advance);
            }
        }
    }
}
