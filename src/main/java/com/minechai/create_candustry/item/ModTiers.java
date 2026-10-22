package com.minechai.create_candustry.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/**
 * 沙金工艺自有的武器材质等级。
 *
 * <p>目前只有沙噬剑使用：除“耐久上限”外全部沿用原版石剑的数值
 * （{@link Tiers#STONE}：挖掘速度 4.0F、攻击伤害加成 1.0F、错误方块标签也为石剑那一套），
 * 沙噬剑自身再提供 1 点攻击加成，因此 {@code SwordItem.createAttributes} 算出的攻击力为
 * {@code 1(物品加成) + 1(材质加成) + 1(空手基础) = 3 点}。</p>
 *
 * <p><b>耐久</b>：{@value SandMasterSwordItem#MAX_DURABILITY} 点，由
 * {@code TieredItem} 构造时写入物品的 {@code MAX_DAMAGE} 组件。</p>
 *
 * <p><b>修复材料</b>：使用 {@link Ingredient#EMPTY}，即原版铁砧<b>无法</b>修复沙噬剑——
 * 完整形态的修复途径只有“用到断裂后放在背包里随时间自动恢复”，避免绕过破损机制。</p>
 */
public final class ModTiers {
    /** 沙噬剑的附魔能力：取原版铁质工具档位（15），保证剑类附魔都容易获得。 */
    public static final int SAND_MASTER_SWORD_ENCHANTMENT_VALUE = 15;

    /** 沙噬剑材质：耐久 40、其余同原版石剑、铁砧不可修复。 */
    public static final Tier SAND_MASTER_SWORD = new Tier() {
        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_STONE_TOOL;
        }

        @Override
        public int getUses() {
            return SandMasterSwordItem.MAX_DURABILITY;
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
            return SAND_MASTER_SWORD_ENCHANTMENT_VALUE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    };

    private ModTiers() {
    }
}
