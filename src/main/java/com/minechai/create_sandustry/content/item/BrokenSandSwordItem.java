package com.minechai.create_sandustry.content.item;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * 沙金剑（破损形态）。
 *
 * <p>破损后不能作为武器使用，耐久值用于记录剩余的修复进度。它不会像破损沙噬剑一样自动恢复，
 * 只能由玩家将它与沙团无序合成来恢复耐久。</p>
 */
public class BrokenSandSwordItem extends Item {
    public BrokenSandSwordItem(Item.Properties properties) {
        super(properties);
    }

    public static Item.Properties applyProperties(Item.Properties properties) {
        return properties
                .stacksTo(1)
                .durability(SandSwordItem.MAX_DURABILITY)
                .attributes(createAttributes());
    }

    private static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, SandSwordItem.ATTACK_SPEED,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** 禁止铁砧修复，确保只能走沙团合成修复流程。 */
    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return false;
    }
}
