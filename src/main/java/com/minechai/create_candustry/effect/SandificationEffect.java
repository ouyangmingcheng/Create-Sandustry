package com.minechai.create_candustry.effect;

import com.minechai.create_candustry.sandheart.SandHeartManager;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * “沙化”：吃下沙团后可能获得、被沙噬剑命中时必然获得的负面状态。
 *
 * <p>机制与原版中毒一致——不是在获得状态时一次性结算，而是在持续时间内<b>周期性判定</b>：
 * 每次判定有 {@value #UNIT_CHANCE} 的概率增加 {@value #UNITS_ON_SUCCESS} 个内部单位沙心
 * （1 个内部单位 = 半颗沙心，见 {@code SandHeartManager.UNITS_PER_HEART}），
 * 判定间隔按等级固定为：</p>
 * <ul>
 *     <li><b>沙化 I</b>（amplifier 0）：每 {@value #INTERVAL_TICKS} tick（0.5 秒）判定一次；</li>
 *     <li><b>沙化 II</b>（amplifier 1）：每 {@value #INTERVAL_TICKS_LEVEL_2} tick（0.25 秒）判定一次；</li>
 *     <li><b>沙化 III 及以上</b>（amplifier ≥ 2）：每 {@value #INTERVAL_TICKS_LEVEL_3} tick（0.2 秒）判定一次
 *         —— 三级之后不再继续加密，避免更高等级出现离谱的判定频率。</li>
 * </ul>
 *
 * <p>概率所有等级一致（{@value #UNIT_CHANCE}）。举例：3 秒沙化 I 共 6 次判定（期望 3 个单位），
 * 6 秒沙化 II 共 24 次判定（期望 12 个单位），60 秒沙化 III 共 300 次判定（期望 150 个单位）。</p>
 *
 * <p>沙心上限由 {@link SandHeartManager#setUnits} 统一夹取（不超过生命条容量），
 * 因此当沙心填满生命条时会自动进入“沙化之兆”（见 {@code SandHeartManager#syncSandHardcore}），
 * 本类不需要额外处理。</p>
 *
 * <p><b>适用范围</b>：玩家与生物通用。生物没有心形 HUD，获得的是“虚拟沙心”——同样按单位累加，
 * 并通过 {@code max_health} 上的负向修饰符挤占其生命上限；填满后同样进入“沙化之兆”。</p>
 *
 * <p>只在服务端结算：客户端的状态实例仅用于显示（图标见
 * {@code assets/createsandustry/textures/mob_effect/sandification.png}），不会修改任何数据，
 * 也就不会出现客户端/服务端重复增加沙心或随机数不同步的问题。</p>
 */
public class SandificationEffect extends MobEffect {
    /** 状态图标颜色（沙色，与沙化之兆同色系）。 */
    public static final int COLOR = 0xD8C08A;
    /** 一级（amplifier 0）的判定间隔：10 tick = 0.5 秒。 */
    public static final int INTERVAL_TICKS = 10;
    /** 二级（amplifier 1）的判定间隔：5 tick = 0.25 秒。 */
    public static final int INTERVAL_TICKS_LEVEL_2 = 5;
    /** 三级及以上（amplifier ≥ 2）的判定间隔：4 tick = 0.2 秒。 */
    public static final int INTERVAL_TICKS_LEVEL_3 = 4;
    /** 每次判定给予沙心的概率：所有等级一致，50%。 */
    public static final double UNIT_CHANCE = 0.5D;
    /** 每次判定成功时增加的沙心单位数（1 个内部单位 = 半颗沙心）。 */
    public static final int UNITS_ON_SUCCESS = 1;

    public SandificationEffect() {
        super(MobEffectCategory.HARMFUL, COLOR);
    }

    /** 某等级下的判定间隔（tick）：一级 10、二级 5、三级及以上 4。 */
    public static int intervalForAmplifier(int amplifier) {
        if (amplifier <= 0) {
            return INTERVAL_TICKS;
        }
        return amplifier == 1 ? INTERVAL_TICKS_LEVEL_2 : INTERVAL_TICKS_LEVEL_3;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 服务端权威结算；客户端实例直接跳过。
        // 玩家与生物一视同仁：生物同样会累积“虚拟沙心”，挤占它的生命上限。
        if (!entity.level().isClientSide && entity.getRandom().nextDouble() < UNIT_CHANCE) {
            SandHeartManager.addUnits(entity, UNITS_ON_SUCCESS);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // 每过“该等级的判定间隔”命中一次（沿用原版中毒 duration % i == 0 的写法）。
        return duration % intervalForAmplifier(amplifier) == 0;
    }
}