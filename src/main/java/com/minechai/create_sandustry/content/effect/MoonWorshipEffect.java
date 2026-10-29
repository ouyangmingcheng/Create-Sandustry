package com.minechai.create_sandustry.content.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

/** Harmful countdown effect used by the Moon Crystal ritual. */
public class MoonWorshipEffect extends MobEffect {
    public static final int COLOR = 0xAFB8CC;

    public MoonWorshipEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()
                || !(entity instanceof ServerPlayer player)
                || player.isSpectator()
                || player.hasEffect(MobEffects.BLINDNESS)
                || player.hasEffect(MobEffects.DARKNESS)) {
            return true;
        }

        // About once every five seconds on average, briefly black out the screen.
        if (player.getRandom().nextFloat() < 0.2F) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,
                    20, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,
                    20, 0, false, true, true));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
