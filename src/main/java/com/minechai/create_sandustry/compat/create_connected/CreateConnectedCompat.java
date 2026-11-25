package com.minechai.create_sandustry.compat.create_connected;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Create: Connected 可选兼容。
 *
 * <p>仅通过注册名查找黄铜溜槽，避免在该模组缺席时触发类加载错误。</p>
 */
public final class CreateConnectedCompat {
    private static final ResourceLocation BRASS_CHUTE_ID =
            ResourceLocation.fromNamespaceAndPath("create_connected", "brass_chute");

    public static boolean isBrassChute(ItemStack stack) {
        Item item = BuiltInRegistries.ITEM.get(BRASS_CHUTE_ID);
        return item != null && item != Items.AIR && stack.is(item);
    }

    public static ItemStack brassChuteStack() {
        Item item = BuiltInRegistries.ITEM.get(BRASS_CHUTE_ID);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private CreateConnectedCompat() {
    }
}
