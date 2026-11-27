package com.minechai.create_sandustry.content.archaeology;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.minechai.create_sandustry.CreateSandustry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.jetbrains.annotations.Nullable;

/**
 * 全局考古战利品池：收集当前服务器最终加载的所有类型为 {@code minecraft:archaeology}
 * 的战利品表，并在刷取时等概率选择其中一张执行。
 *
 * <p>战利品表来自数据包注册表 {@link Registries#LOOT_TABLE}，在 {@code /reload} 后会替换为
 * 新实例，因此 KubeJS / LootJS 新增或覆盖的考古战利品表会在这里取到最终生效的版本。</p>
 *
 * <p>池按“注册表实例”缓存：只有当数据重载产生新的战利品注册表时才重建一次，避免每次刷取都
 * 遍历全部战利品表，也避免数据重载后继续使用旧缓存。缓存只保留最新一份注册表引用，不会泄漏。</p>
 */
public final class UniversalArchaeologyLootPool {
    /**
     * 特殊方块专用的考古战利品表，不参与全局考古池抽取（原方块仍使用自身专属表）。
     */
    private static final Set<ResourceLocation> EXCLUDED_ARCHAEOLOGY_LOOT = Set.of(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "archaeology/cat_litter"),
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "archaeology/cat_litter_fallback"));

    /** 按命名空间排除整个模组的考古战利品表，也覆盖后续新增的表。 */
    private static final Set<String> EXCLUDED_ARCHAEOLOGY_NAMESPACES = Set.of("kaleidoscope_end");

    private static Registry<LootTable> cachedRegistry;
    private static List<LootTable> cachedPool = List.of();

    private UniversalArchaeologyLootPool() {
    }

    /**
     * 从全局池中等概率随机选择一张考古战利品表；池为空时返回 {@code null}。
     */
    @Nullable
    public static LootTable pickRandom(ServerLevel level) {
        List<LootTable> pool = pool(level);
        if (pool.isEmpty()) {
            return null;
        }
        return pool.get(level.random.nextInt(pool.size()));
    }

    private static List<LootTable> pool(ServerLevel level) {
        Registry<LootTable> registry = level.getServer()
                .reloadableRegistries()
                .get()
                .registryOrThrow(Registries.LOOT_TABLE);

        if (registry != cachedRegistry) {
            List<LootTable> built = new ArrayList<>();
            for (Map.Entry<ResourceKey<LootTable>, LootTable> entry : registry.entrySet()) {
                boolean archaeologyType = entry.getValue().getParamSet() == LootContextParamSets.ARCHAEOLOGY;
                ResourceLocation id = entry.getKey().location();
                boolean excluded = EXCLUDED_ARCHAEOLOGY_NAMESPACES.contains(id.getNamespace())
                        || EXCLUDED_ARCHAEOLOGY_LOOT.contains(id);
                if (archaeologyType && !excluded) {
                    built.add(entry.getValue());
                }
            }
            cachedRegistry = registry;
            cachedPool = List.copyOf(built);
        }
        return cachedPool;
    }
}
