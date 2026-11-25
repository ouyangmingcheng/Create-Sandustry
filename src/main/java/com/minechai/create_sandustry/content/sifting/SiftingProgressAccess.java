package com.minechai.create_sandustry.content.sifting;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * 传送带物品上的筛振处理进度。
 *
 * <p>进度保存在 {@code TransportedItemStack} 上，因此可以直接跟随 Create
 * 的传送带复制与库存 NBT 一起跨段、跨存档保存。</p>
 */
public interface SiftingProgressAccess {
    @Nullable
    ResourceLocation getSiftingRecipeId();

    int getSiftingProgress();

    int getSiftingRequiredProgress();

    void setSiftingProgress(@Nullable ResourceLocation recipeId, int progress, int requiredProgress);

    void clearSiftingProgress();
}
