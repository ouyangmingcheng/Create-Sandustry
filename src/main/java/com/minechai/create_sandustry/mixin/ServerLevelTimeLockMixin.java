package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.timebearing.TimeBearingService;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerLevel.class)
public abstract class ServerLevelTimeLockMixin {
    @Redirect(
            method = "tickTime",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean createsandustry$overrideDaylightLock(
            GameRules gameRules,
            GameRules.Key<GameRules.BooleanValue> key) {
        boolean vanillaValue = gameRules.getBoolean(key);
        return vanillaValue || TimeBearingService.shouldOverrideDaylightCycle((ServerLevel) (Object) this);
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z",
                    ordinal = 0))
    private boolean createsandustry$allowSleepTimeSkip(
            GameRules gameRules,
            GameRules.Key<GameRules.BooleanValue> key) {
        boolean vanillaValue = gameRules.getBoolean(key);
        return vanillaValue || TimeBearingService.shouldAllowSleepTimeSkip((ServerLevel) (Object) this);
    }
}
