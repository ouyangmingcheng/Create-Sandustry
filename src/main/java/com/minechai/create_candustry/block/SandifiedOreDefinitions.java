package com.minechai.create_candustry.block;

import java.util.List;

import com.minechai.create_candustry.CreateSandustry;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * 沙化矿石的单一事实来源。
 *
 * <p>源方块与等级来自 Minecraft 1.21.1 / Create 6.0.10-280 的实际资源与数据：
 * 普通矿石映射到硬化沙子矿石，深层矿石映射到石化沙子矿石。矿石经验范围同样
 * 按源方块实现保留；Create 锌矿实际使用普通 {@link Block} 且无矿石经验。</p>
 */
public final class SandifiedOreDefinitions {
    public enum ToolTier {
        WOOD(null),
        STONE(BlockTags.NEEDS_STONE_TOOL),
        IRON(BlockTags.NEEDS_IRON_TOOL);

        private final @Nullable TagKey<Block> needsTag;

        ToolTier(@Nullable TagKey<Block> needsTag) {
            this.needsTag = needsTag;
        }

        public @Nullable TagKey<Block> needsTag() {
            return needsTag;
        }
    }

    public record Ore(
            String key,
            String sourceBlock,
            String deepslateSourceBlock,
            ToolTier toolTier,
            int xpMin,
            int xpMax,
            boolean redstoneBehavior) {
        public Ore {
            if (xpMin < 0 || xpMax > 10 || xpMin > xpMax) {
                throw new IllegalArgumentException("invalid experience range for " + key);
            }
        }
    }

    public record Variant(
            String key,
            String targetPrefix,
            boolean usesDeepslateSource,
            String targetBaseBlock) {
    }

    public record Entry(Ore ore, Variant variant) {
        public String id() {
            return variant.targetPrefix() + "_" + ore.key() + "_ore";
        }

        public String targetBlock() {
            return CreateSandustry.MODID + ":" + id();
        }

        public String sourceBlock() {
            return variant.usesDeepslateSource()
                    ? ore.deepslateSourceBlock()
                    : ore.sourceBlock();
        }
    }

    public static final Variant HARDENED = new Variant(
            "hardened", "hardened_sand", false, CreateSandustry.MODID + ":hardened_sand");
    public static final Variant PETRIFIED = new Variant(
            "petrified", "petrified_sand", true, CreateSandustry.MODID + ":petrified_sand");

    public static final List<Ore> ORES = List.of(
            new Ore("coal", "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
                    ToolTier.WOOD, 0, 2, false),
            new Ore("iron", "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
                    ToolTier.STONE, 0, 0, false),
            new Ore("copper", "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
                    ToolTier.STONE, 0, 0, false),
            new Ore("gold", "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
                    ToolTier.IRON, 0, 0, false),
            new Ore("redstone", "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
                    ToolTier.IRON, 1, 5, true),
            new Ore("lapis", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
                    ToolTier.STONE, 2, 5, false),
            new Ore("diamond", "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
                    ToolTier.IRON, 3, 7, false),
            new Ore("emerald", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
                    ToolTier.IRON, 3, 7, false),
            new Ore("zinc", "create:zinc_ore", "create:deepslate_zinc_ore",
                    ToolTier.IRON, 0, 0, false));

    public static final List<Variant> VARIANTS = List.of(HARDENED, PETRIFIED);

    public static final List<Entry> ENTRIES = VARIANTS.stream()
            .flatMap(variant -> ORES.stream().map(ore -> new Entry(ore, variant)))
            .toList();

    private SandifiedOreDefinitions() {
    }
}
