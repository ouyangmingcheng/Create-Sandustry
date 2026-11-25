package com.minechai.create_sandustry.content.worldgen.geology.rules;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.sand.SandifiedOreDefinitions;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyConfigSnapshot;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyGenerationContext;
import com.minechai.create_sandustry.content.worldgen.geology.GeologyRuleProcessor;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * 将新生成区块中的原版/Create 原矿精确替换为对应的沙化矿石。
 *
 * <p>普通矿石映射到硬化沙矿石，深板岩矿石映射到石化沙矿石。只有
 * {@link SandifiedOreDefinitions} 中登记的精确源方块会被处理，未登记矿石保持不变。</p>
 */
public final class SandifiedOreRule implements GeologyRuleProcessor {
    private static volatile Map<Block, Block> replacements;

    @Override
    public String id() {
        return "sandified_ore";
    }

    @Override
    public boolean isEnabled(GeologyConfigSnapshot config) {
        return config.enabled();
    }

    @Override
    public boolean apply(GeologyGenerationContext context, GeologyConfigSnapshot config) {
        return applyMappedOres(context);
    }

    boolean applyMappedOres(GeologyGenerationContext context) {
        Map<Block, Block> mapping = replacements();
        if (mapping.isEmpty()) {
            return false;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean changed = false;
        outer:
        for (int y = context.minY(); y <= context.maxY(); y++) {
            for (int x = context.chunkMinX(); x <= context.chunkMaxX(); x++) {
                for (int z = context.chunkMinZ(); z <= context.chunkMaxZ(); z++) {
                    if (!context.consumeCheck()) {
                        break outer;
                    }

                    cursor.set(x, y, z);
                    BlockState state = context.level().getBlockState(cursor);
                    Block replacement = mapping.get(state.getBlock());
                    if (replacement != null) {
                        changed |= context.replaceIfAllowed(cursor, state.getBlock(), replacement);
                    }
                }
            }
        }
        return changed;
    }

    static Map<Block, Block> replacements() {
        Map<Block, Block> cached = replacements;
        if (cached != null) {
            return cached;
        }

        synchronized (SandifiedOreRule.class) {
            cached = replacements;
            if (cached != null) {
                return cached;
            }

            Map<Block, Block> built = new LinkedHashMap<>();
            for (SandifiedOreDefinitions.Entry entry : SandifiedOreDefinitions.ENTRIES) {
                ResourceLocation sourceId = ResourceLocation.tryParse(entry.sourceBlock());
                DeferredBlock<Block> targetHolder = ModBlocks.SANDIFIED_ORES.get(entry.id());
                if (sourceId == null || targetHolder == null) {
                    CreateSandustry.LOGGER.warn("跳过无效沙化矿石映射：{} -> {}", entry.sourceBlock(), entry.targetBlock());
                    continue;
                }

                Block source = BuiltInRegistries.BLOCK.get(sourceId);
                if (source == Blocks.AIR) {
                    CreateSandustry.LOGGER.warn("跳过未注册的沙化矿石源方块：{}", entry.sourceBlock());
                    continue;
                }
                built.put(source, targetHolder.get());
            }
            cached = Collections.unmodifiableMap(built);
            replacements = cached;
            return cached;
        }
    }
}
