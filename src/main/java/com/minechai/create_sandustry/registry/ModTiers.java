package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordItem;
import com.minechai.create_sandustry.content.sandsword.SandSwordItem;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 沙金工艺自有的武器材质等级。
 *
 * <p>沙噬剑与沙金剑除“耐久上限”外全部沿用原版石剑的数值
 * （{@link Tiers#STONE}：挖掘速度 4.0F、攻击伤害加成 1.0F、错误方块标签也为石剑那一套）。
 * 两把剑自身再提供 1 点攻击加成，因此 {@code SwordItem.createAttributes} 算出的攻击力为
 * {@code 1(物品加成) + 1(材质加成) + 1(空手基础) = 3 点}。</p>
 *
 * <p><b>修复材料</b>：使用 {@link Ingredient#EMPTY}，即原版铁砧无法修复这两把剑；
 * 它们各自只走现有的破损恢复流程。</p>
 */
public final class ModTiers {
    /** 剑类附魔能力：取原版铁质工具档位（15）。 */
    public static final int SAND_SWORD_ENCHANTMENT_VALUE = 15;

    /** 沙噬剑材质：耐久 40、其余同原版石剑、铁砧不可修复。 */
    public static final Tier SAND_MASTER_SWORD = createSwordTier(SandMasterSwordItem.MAX_DURABILITY);

    /** 沙金剑材质：耐久 40、其余同原版石剑、铁砧不可修复。 */
    public static final Tier SAND_SWORD = createSwordTier(SandSwordItem.MAX_DURABILITY);

    private static Tier createSwordTier(int uses) {
        return new Tier() {
            @Override
            public TagKey<Block> getIncorrectBlocksForDrops() {
                return BlockTags.INCORRECT_FOR_STONE_TOOL;
            }

            @Override
            public int getUses() {
                return uses;
            }

            @Override
            public float getSpeed() {
                return Tiers.STONE.getSpeed();
            }

            @Override
            public float getAttackDamageBonus() {
                return Tiers.STONE.getAttackDamageBonus();
            }

            @Override
            public int getEnchantmentValue() {
                return SAND_SWORD_ENCHANTMENT_VALUE;
            }

            @Override
            public Ingredient getRepairIngredient() {
                return Ingredient.EMPTY;
            }
        };
    }

    private ModTiers() {
    }
}
