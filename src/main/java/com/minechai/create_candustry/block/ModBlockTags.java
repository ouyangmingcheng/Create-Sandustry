package com.minechai.create_candustry.block;

import com.minechai.create_candustry.CreateSandustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 沙金工艺自身的方块标签。
 *
 * <p>{@link #SAND} 对应 {@code createsandustry:sand}，是“沙子类方块”的统一分类来源，
 * 数据文件位于 {@code data/createsandustry/tags/block/sand.json}，同时包含：</p>
 * <ul>
 *     <li>原版沙子：{@code minecraft:sand}、{@code minecraft:red_sand}；</li>
 *     <li>沙金工艺注册的全部沙类方块：湿沙子、硬化沙子、石化沙、烈性沙子、雷霆沙子、猫砂、活沙。</li>
 * </ul>
 *
 * <p>它不依赖方块 ID 里是否含有 {@code sand}，也不依赖方块类继承关系（本模组的沙类方块分别继承
 * {@code Block} / {@code SpecialSandBlock} / {@code BrushableBlock} / {@code BaseEntityBlock}，
 * 没有共同基类），因此是唯一可维护的判定方式。可疑沙子 / 可疑沙砾等具有独立考古机制的方块
 * 不属于本标签。</p>
 */
public final class ModBlockTags {
    /** 沙子类方块标签（原版沙子 + 沙金工艺沙类方块）：{@code #createsandustry:sand}。 */
    public static final TagKey<Block> SAND = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand"));

    private ModBlockTags() {
    }
}
