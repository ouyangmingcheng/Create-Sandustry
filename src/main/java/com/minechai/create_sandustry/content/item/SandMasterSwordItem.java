package com.minechai.create_sandustry.content.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;

/**
 * 沙噬剑（完整形态）。
 *
 * <p>一把普通的原版剑（{@link SwordItem}，因此攻击判定、冷却、挥砍动画、耐久条、附魔、物品栏行为
 * 全部走原版），在此基础上加了两条特殊规则：</p>
 * <ol>
 *     <li><b>命中施加“沙化”</b>：{@link #hurtEnemy} 只在<b>真正命中</b>时被调用；</li>
 *     <li><b>耐久耗尽不会销毁物品</b>：由 {@link BreakableSwordDurability} 共用实现，
 *         在“本应碎掉”时被换成破损形态并放回同一个手部槽位。</li>
 * </ol>
 *
 * <p>物品属性见 {@link #applyProperties(Item.Properties)}：攻击力 3 点、攻击速度 -2.4、
 * 耐久上限 {@value #MAX_DURABILITY}（由 {@link ModTiers#SAND_MASTER_SWORD} 提供）。</p>
 */
public class SandMasterSwordItem extends SwordItem {
    /** 最大耐久：40 点。 */
    public static final int MAX_DURABILITY = 40;
    /** 攻击伤害加成：1 + 材质加成 1 = 2，叠加空手基础的 1 点 ⇒ 面板 3 点。 */
    public static final int ATTACK_DAMAGE_BONUS = 1;
    /** 攻击速度修正：-2.4，与原版剑一致。 */
    public static final float ATTACK_SPEED = -2.4F;

    public SandMasterSwordItem(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    /** 完整形态的物品属性：不可堆叠、攻击力 3 点、攻击速度同原版剑、耐久由材质提供。 */
    public static Item.Properties applyProperties(Item.Properties properties) {
        return properties
                .stacksTo(1)
                .fireResistant()
                .attributes(SwordItem.createAttributes(ModTiers.SAND_MASTER_SWORD, ATTACK_DAMAGE_BONUS, ATTACK_SPEED));
    }

    /**
     * 固定消耗 1 点耐久（剑气属于固定代价，不经过耐久附魔减免）。
     *
     * <p>如果这一下会把耐久耗尽，则复用现有的破损形态转换：物品被换成
     * {@code createsandustry:sand_master_sword_broken} 并放回同一只手，不会消失。</p>
     */
    public static void consumeOneDurability(ItemStack stack, LivingEntity holder) {
        BreakableSwordDurability.consumeOneDurability(stack, holder, () -> SandMasterSwordForms.toBroken(stack));
    }

    /**
     * 成功命中目标后施加“沙化”。
     *
     * <p>返回 {@code true} 与原版剑一致，用于让原版记录“使用物品”统计，并让原版继续调用
     * {@link #postHurtEnemy} 处理耐久。</p>
     */
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!attacker.level().isClientSide) {
            // 近战与剑气共用同一套沙化规则。
            SandMasterSwordSandification.apply(target, attacker);
        }
        return true;
    }

    /**
     * 攻击命中后的耐久结算：规则与原版剑相同，唯一区别是“耐久耗尽”时转为破损形态而不是销毁物品。
     */
    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        BreakableSwordDurability.hurtAndBreakOrBreak(stack, attacker, () -> SandMasterSwordForms.toBroken(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.createsandustry.sand_master_sword.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
