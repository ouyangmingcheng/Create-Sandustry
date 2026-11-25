package com.minechai.create_sandustry.foundation.item;

import java.util.function.Supplier;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** 可在耐久耗尽时转换为破损形态的剑所共用的耐久辅助方法。 */
public final class BreakableSwordDurability {
    private BreakableSwordDurability() {
    }

    /**
     * 按原版规则结算一次命中耐久；若本应损坏，则改放回破损形态。
     *
     * <p>先在副本上执行 {@link ItemStack#hurtAndBreak(int, LivingEntity, EquipmentSlot)}，
     * 从而保留耐久附魔减免与创造模式免消耗规则；只有副本真正耗尽时，才替换手中的真实物品。</p>
     */
    public static void hurtAndBreakOrBreak(ItemStack stack, LivingEntity attacker, Supplier<ItemStack> brokenForm) {
        if (stack.isEmpty() || !(attacker.level() instanceof ServerLevel)) {
            return;
        }
        ItemStack probe = stack.copy();
        probe.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        if (!probe.isEmpty()) {
            stack.setDamageValue(probe.getDamageValue());
            return;
        }
        attacker.setItemInHand(handOf(attacker, stack), brokenForm.get());
    }

    /** 固定消耗 1 点耐久；若会耗尽，则改为放回破损形态。 */
    public static void consumeOneDurability(ItemStack stack, LivingEntity holder, Supplier<ItemStack> brokenForm) {
        if (stack.isEmpty()) {
            return;
        }
        int next = stack.getDamageValue() + 1;
        if (next >= stack.getMaxDamage()) {
            holder.setItemInHand(handOf(holder, stack), brokenForm.get());
        } else {
            stack.setDamageValue(next);
        }
    }

    private static InteractionHand handOf(LivingEntity holder, ItemStack stack) {
        return holder.getMainHandItem() == stack ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }
}
