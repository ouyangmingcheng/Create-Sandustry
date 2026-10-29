package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/**
 * 沙噬剑两种形态之间的转换。
 *
 * <p>转换统一走原版 {@link ItemStack#transmuteCopy(net.minecraft.world.level.ItemLike, int)}：
 * 它会把源物品栈的<b>整套数据组件</b>（附魔、自定义名称、其它模组写入的数据等）原样带到新物品上，
 * 因此完整 ⇄ 破损 的往返不会丢失附魔或玩家自定义信息；随后再显式设置耐久值：</p>
 * <ul>
 *     <li>完整 → 破损（断裂）：耐久归零，即 {@code damage = MAX_DAMAGE}；</li>
 *     <li>破损 → 完整（修好）：耐久回满，即 {@code damage = 0}。</li>
 * </ul>
 *
 * <p>调用方负责把返回的物品栈放回<b>原来的槽位</b>（{@code setItemInHand} / {@code Inventory#setItem}），
 * 因此不会复制物品、也不会丢失物品。</p>
 */
public final class SandMasterSwordForms {
    private SandMasterSwordForms() {
    }

    /** 生成一张“刚断裂”的破损沙噬剑（耐久 0），供创造模式物品栏等场景直接展示破损形态。 */
    public static ItemStack createBrokenInstance() {
        ItemStack stack = new ItemStack(ModItems.SAND_MASTER_SWORD_BROKEN.get());
        stack.setDamageValue(stack.getMaxDamage());
        return stack;
    }

    /** 完整形态 → 破损形态：保留全部数据组件，耐久归零。 */
    public static ItemStack toBroken(ItemStack source) {
        ItemStack broken = source.transmuteCopy(ModItems.SAND_MASTER_SWORD_BROKEN.get(), 1);
        broken.setDamageValue(broken.getMaxDamage());
        return broken;
    }

    /** 破损形态 → 完整形态：保留全部数据组件，耐久回满。 */
    public static ItemStack toRepaired(ItemStack broken) {
        ItemStack repaired = broken.transmuteCopy(ModItems.SAND_MASTER_SWORD.get(), 1);
        repaired.setDamageValue(0);
        return repaired;
    }
}