package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.timebearing.TimeBearingService;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity;

import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClockworkBearingBlockEntity.class)
public abstract class ClockworkBearingTimeMixin {
    @Shadow
    protected boolean running;

    @Shadow
    protected ControlledContraptionEntity hourHand;


    @Inject(method = "tick", at = @At("RETURN"))
    private void createsandustry$updateTimeLock(CallbackInfo ci) {
        ClockworkBearingBlockEntity bearing = (ClockworkBearingBlockEntity) (Object) this;
        if (bearing.getLevel() instanceof ServerLevel) {
            boolean assembled = this.running && this.hourHand != null;
            boolean stalled = this.hourHand != null && this.hourHand.isStalled();
            TimeBearingService.onClockworkBearingTick(bearing, assembled, stalled, ((KineticStressAccessor) (Object) bearing).createsandustry$getNetworkCapacity());
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void createsandustry$notifyRemoved(CallbackInfo ci) {
        ClockworkBearingBlockEntity bearing = (ClockworkBearingBlockEntity) (Object) this;
        if (bearing.getLevel() instanceof ServerLevel level) {
            TimeBearingService.onBearingRemoved(level, bearing.getBlockPos());
        }
    }
}
