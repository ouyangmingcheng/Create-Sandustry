package com.minechai.create_sandustry.content.item;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 沙噬剑（破损形态）。
 *
 * <p>不是被销毁的物品，而是一把待修复的武器：</p>
 * <ul>
 *     <li><b>无法作为武器使用</b>：{@code SandMasterSwordEvents#onAttackEntity} 会在原版
 *         {@code Player#attack} 的最开始取消玩家的攻击事件（服务端权威），因此用破损沙噬剑攻击
 *         既不会造成伤害，也不会触发附魔、击退、命中效果——即使这把剑上还带着锋利附魔也一样。
 *         物品自身也不再提供任何攻击伤害加成（只保留 -2.4 的攻击速度，挥砍手感与完整形态一致）；</li>
 *     <li><b>不会因攻击/使用消耗耐久</b>：攻击被取消，根本走不到原版的耐久结算；耐久值在这里只用于
 *         记录修复进度（{@code damage = 40} 表示完全断裂，{@code damage = 0} 表示修好）；</li>
 *     <li><b>放在背包里会自动修复（不需要材料）</b>：铁砧修复被显式禁用（{@link #isValidRepairItem} 恒为 false），
 *         修复逻辑见 {@code SandMasterSwordEvents}；</li>
 *     <li>破损形态与完整形态使用不同的物品 ID，因此存档、丢弃、拾取、移动、退出重进都天然安全。</li>
 * </ul>
 *
 * <p>附魔、自定义名称等数据在完整 ⇄ 破损 转换时由
 * {@link SandMasterSwordForms} 通过原版 {@code ItemStack#transmuteCopy} 完整保留。</p>
 */
public class BrokenSandMasterSwordItem extends Item {
    public BrokenSandMasterSwordItem(Item.Properties properties) {
        super(properties);
    }

    /** 破损形态的物品属性：不可堆叠、可显示耐久条（修复进度）、只保留攻击速度。 */
    public static Item.Properties applyProperties(Item.Properties properties) {
        return properties
                .stacksTo(1)
                .fireResistant()
                .durability(SandMasterSwordItem.MAX_DURABILITY)
                .attributes(createAttributes());
    }

    /** 破损形态的属性修饰符：不再提供任何攻击伤害（攻击本身也被事件取消），只保留剑的攻击速度。 */
    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, SandMasterSwordItem.ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** 破损形态不使用铁砧修复：放在背包里会随时间自动恢复（见 {@code SandMasterSwordEvents}）。 */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }

}
