package com.minechai.create_sandustry.content.sandification;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * “沙化之兆”：沙心填满生命条后进入的倒计时状态，持续 2 分钟（2400 tick）。玩家与生物通用。
 *
 * <p>效果期间持有者（玩家或生物）的实际生命值始终保持 1 点（半颗原版心）：只在生命值高于 1 时修正，
 * 不每 tick 施加伤害、也不播放音效。自然到期后的死亡由
 * {@code SandHardcoreEvents#onEffectExpired} 处理（仅自然到期触发，牛奶等提前解除不会）。</p>
 */
public class SandHardcoreEffect extends MobEffect {
    public static final int COLOR = 0xD8C08A;

    public SandHardcoreEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getHealth() > 1.0F) {
            entity.setHealth(1.0F);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

}
