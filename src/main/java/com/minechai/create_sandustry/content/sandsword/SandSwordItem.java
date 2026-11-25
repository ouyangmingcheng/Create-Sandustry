package com.minechai.create_sandustry.content.sandsword;

import com.minechai.create_sandustry.foundation.item.BreakableSwordDurability;
import com.minechai.create_sandustry.registry.ModTiers;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 沙金剑（完整形态）。
 *
 * <p>攻击力 3 点、攻击速度 1.6、最大耐久 40。耐久耗尽时与沙噬剑一样转为破损形态，
 * 但破损形态不会自动修复，只能通过与沙团无序合成恢复耐久。</p>
 */
public class SandSwordItem extends SwordItem {
    public static final int MAX_DURABILITY = 40;
    public static final int ATTACK_DAMAGE_BONUS = 1;
    /** 原版玩家基础攻击速度为 4.0，修正 -2.4 后面板攻击速度为 1.6。 */
    public static final float ATTACK_SPEED = -2.4F;

    public SandSwordItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    public static Item.Properties applyProperties(Item.Properties properties) {
        return properties
                .stacksTo(1)
                .attributes(SwordItem.createAttributes(ModTiers.SAND_SWORD, ATTACK_DAMAGE_BONUS, ATTACK_SPEED));
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        BreakableSwordDurability.hurtAndBreakOrBreak(stack, attacker, () -> SandSwordForms.toBroken(stack));
    }
}
