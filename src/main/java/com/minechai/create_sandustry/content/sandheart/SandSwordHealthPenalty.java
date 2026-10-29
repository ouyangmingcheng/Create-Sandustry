package com.minechai.create_sandustry.content.sandheart;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** 拔剑成功后支付的永久生命上限代价。 */
public final class SandSwordHealthPenalty {
    /** 每次成功拔剑永久扣除的生命值点数：6 颗心 = 12 点。 */
    public static final int PENALTY_PER_SUCCESS = 12;

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand_sword_health_penalty");

    private SandSwordHealthPenalty() {
    }

    public static int getTotalPenalty(ServerPlayer player) {
        return Math.max(0, player.getData(ModAttachments.SAND_SWORD_HEALTH_PENALTY));
    }

    /** 成功拔剑时调用；只在这里增加永久代价。 */
    public static void applySuccessfulPullPenalty(ServerPlayer player) {
        player.setData(ModAttachments.SAND_SWORD_HEALTH_PENALTY,
                getTotalPenalty(player) + PENALTY_PER_SUCCESS);
        refresh(player);
    }

    /** 登录、重生和成功结算后重新施加持久化修饰符。 */
    public static void refresh(ServerPlayer player) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        int penalty = getTotalPenalty(player);
        if (penalty <= 0) {
            maxHealth.removeModifier(MODIFIER_ID);
        } else {
            maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                    MODIFIER_ID, -penalty, AttributeModifier.Operation.ADD_VALUE));
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }
}
