package com.minechai.create_sandustry.content.qunyou;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 群友的通用游戏事件。 */
public final class QunyouEvents {
    private QunyouEvents() {
    }


    /**
     * 群友受到攻击时先打断睡眠或胡萝卜召唤，再按原有规则尝试闪避。
     * 中断不依赖暴击、攻击者类型或最终伤害是否被盾牌/闪避完全抵消。
     */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount() <= 0.0F
                || !(event.getEntity() instanceof Qunyou qunyou)) {
            return;
        }
        qunyou.interruptSleepFromAttack();
        qunyou.interruptCarrotSummonFromAttack();

        LivingEntity attacker = livingAttacker(event);
        // 只有闪避真正成功启动时才取消伤害；忙碌状态必须继续正常结算。
        if (attacker != null && qunyou.canDodgeAttack(attacker)
                && qunyou.tryDodgeAttack(attacker)) {
            event.setCanceled(true);
        }
    }

    private static LivingEntity livingAttacker(LivingIncomingDamageEvent event) {
        Entity sourceEntity = event.getSource().getEntity();
        if (sourceEntity instanceof LivingEntity living) {
            return living;
        }
        Entity directEntity = event.getSource().getDirectEntity();
        if (directEntity instanceof Projectile projectile
                && projectile.getOwner() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }
}