package com.minechai.create_sandustry.content.sandification;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * 沙化之兆的状态事件与饮用检测。
 *
 * <p><b>自然到期 vs 主动解除</b>：NeoForge 的 {@link MobEffectEvent.Expired} 只在状态持续时间自然耗尽时
 * 触发（见 {@code LivingEntity#tickEffects}），而牛奶 / 命令 / 其它模组移除效果走的是
 * {@link MobEffectEvent.Remove}。因此这里只在 Expired 中触发沙化死亡，提前解除绝不会误杀，
 * 也不需要任何“延迟死亡任务”或轮询。</p>
 *
 * <p><b>饮用检测</b>：监听 {@link LivingEntityUseItemEvent.Finish}，用物品自身的饮用动画
 * （{@link UseAnim#DRINK}）判断是否真正完成了饮用，因此覆盖原版药水、牛奶以及其它模组中
 * 真正的饮用类物品；普通进食（{@link UseAnim#EAT}）不会误触发。</p>
 */
public final class SandHardcoreEvents {
    private static final ResourceKey<DamageType> SANDIFICATION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sandification"));

    private SandHardcoreEvents() {
    }

    /**
     * 沙化之兆自然到期：使用专用伤害来源杀死持有者（玩家走 {@code death.attack.sandification} 死亡播报）。
     *
     * <p>玩家与生物通用：生物被沙化挤占满生命上限后同样会在沙化之兆到期时变成沙子。</p>
     */
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        if (event.getEffectInstance() == null
                || event.getEffectInstance().getEffect().value() != ModMobEffects.SAND_HARDCORE.get()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        // 只在这里触发一次；不留下任何延迟死亡标记，牛奶提前解除不会走到这里。
        entity.invulnerableTime = 0;
        entity.hurt(entity.level().damageSources().source(SANDIFICATION), Float.MAX_VALUE);
    }

    /** 成功完成一次饮用：随机减少 1~3 个内部沙心单位（半颗~1.5 颗沙心）。 */
    public static void onItemUseFinished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getItem().getUseAnimation() != UseAnim.DRINK) {
            return;
        }
        SandHeartManager.addUnits(player, -(1 + player.getRandom().nextInt(3)));
    }
}
