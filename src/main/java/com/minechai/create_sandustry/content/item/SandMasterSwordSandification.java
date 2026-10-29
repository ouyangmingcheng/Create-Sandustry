package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * 沙噬剑近战与剑气共用的沙化施加规则。
 *
 * <p>沙心附件以半颗沙心为一个内部单位；这里先按
 * {@link SandHeartManager#UNITS_PER_HEART} 换成完整沙心数，再比较阈值，半颗沙心不会提前触发等级。</p>
 */
public final class SandMasterSwordSandification {
    /** 每次命中的沙化时长增量：3 秒。 */
    public static final int DURATION_TICKS = 60;

    /** 达到沙化 I 所需完整沙心数。 */
    public static final int LEVEL_1_MIN_HEARTS = 5;
    /** 达到沙化 II 所需完整沙心数。 */
    public static final int LEVEL_2_MIN_HEARTS = 10;
    /** 达到沙化 III 所需完整沙心数。 */
    public static final int LEVEL_3_MIN_HEARTS = 15;

    private SandMasterSwordSandification() {
    }

    /**
     * 按攻击者当前完整沙心数对目标施加沙化。
     *
     * <p>攻击者必须是从服务端上下文取得的玩家。每次命中都会在目标剩余沙化时长上追加
     * {@value #DURATION_TICKS} tick；已有更高等级时保留更高等级，不因本次较低等级而降级。
     * 已有无限时长时保持无限。</p>
     *
     * @return 本次是否实际提交了沙化效果
     */
    public static boolean apply(LivingEntity target, @Nullable Entity source) {
        if (target.level().isClientSide || !(source instanceof Player attacker) || attacker.level().isClientSide) {
            return false;
        }

        int units = SandHeartManager.getUnits(attacker);
        int fullHearts = units / SandHeartManager.UNITS_PER_HEART;
        int amplifier = amplifierForHearts(fullHearts);
        if (amplifier < 0) {
            // 不足 5 颗完整沙心时不施加，也不清除目标已有的沙化。
            return false;
        }

        int duration = DURATION_TICKS;
        MobEffectInstance existing = target.getEffect(ModMobEffects.SANDIFICATION);
        if (existing != null) {
            // 等级只升级不降级；持续时间在剩余时长上叠加，不覆盖已有更长的时长。
            amplifier = Math.max(amplifier, existing.getAmplifier());
            duration = existing.isInfiniteDuration() ? MobEffectInstance.INFINITE_DURATION
                    : existing.getDuration() + DURATION_TICKS;
        }

        MobEffectInstance effect = new MobEffectInstance(ModMobEffects.SANDIFICATION, duration, amplifier,
                false, true, true);
        // 普通目标走 addEffect；末影龙/凋零等拒绝 addEffect 的目标仍由 SandHeartManager 的
        // forceAddEffect 回退写入，但最终等级/时长由本方法统一决定，不再有 boss 专属等级。
        SandHeartManager.applyEffect(target, effect, source);
        return true;
    }

    /**
     * 完整沙心数对应的沙化等级，返回 {@code -1} 表示不施加。
     * amplifier 从 0 开始计数：5~9 颗 = I，10~14 颗 = II，15 颗及以上 = III。
     */
    public static int amplifierForHearts(int fullHearts) {
        if (fullHearts < LEVEL_1_MIN_HEARTS) {
            return -1;
        }
        if (fullHearts < LEVEL_2_MIN_HEARTS) {
            return 0;
        }
        if (fullHearts < LEVEL_3_MIN_HEARTS) {
            return 1;
        }
        return 2;
    }
}
