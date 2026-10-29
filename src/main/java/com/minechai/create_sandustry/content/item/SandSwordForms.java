package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/** 沙金剑完整形态、破损形态以及沙团修复之间的状态转换。 */
public final class SandSwordForms {
    /** 每个沙团恢复的耐久点数。 */
    public static final int DURABILITY_PER_SAND_BALL = 2;

    private SandSwordForms() {
    }

    public static ItemStack createBrokenInstance() {
        ItemStack stack = new ItemStack(ModItems.SAND_SWORD_BROKEN.get());
        stack.setDamageValue(stack.getMaxDamage());
        return stack;
    }

    /** 完整形态 -> 破损形态：保留附魔、自定义名称等全部数据组件，耐久归零。 */
    public static ItemStack toBroken(ItemStack source) {
        ItemStack broken = source.transmuteCopy(ModItems.SAND_SWORD_BROKEN.get(), 1);
        broken.setDamageValue(broken.getMaxDamage());
        return broken;
    }

    /** 破损形态 -> 完整形态：保留全部数据组件，耐久回满。 */
    public static ItemStack toRepaired(ItemStack broken) {
        return restoreDurability(broken, broken.getMaxDamage());
    }

    /**
     * 与一个沙团合成后的结果。
     *
     * <p>无论输入是完整还是破损形态，都会转换回完整物品 ID，并恢复
     * {@value #DURABILITY_PER_SAND_BALL} 点耐久；若输入是破损剑且恢复后耐久回满，
     * 输出自然就是完整的沙金剑。</p>
     */
    public static ItemStack repairWithSandBall(ItemStack source) {
        return restoreDurability(source, DURABILITY_PER_SAND_BALL);
    }

    private static ItemStack restoreDurability(ItemStack source, int amount) {
        int damage = Math.max(0, source.getDamageValue() - Math.max(0, amount));
        ItemStack repaired = source.transmuteCopy(ModItems.SAND_SWORD.get(), 1);
        repaired.setDamageValue(damage);
        return repaired;
    }
}
