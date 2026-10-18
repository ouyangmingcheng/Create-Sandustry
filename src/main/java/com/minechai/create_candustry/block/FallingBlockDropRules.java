package com.minechai.create_candustry.block;

import com.minechai.create_candustry.config.ModConfigs;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 统一处理“下落失败、准备转化为物品掉落”时的掉落规则。
 *
 * <p>作用范围覆盖所有使用原版 {@code FallingBlockEntity} 下落机制的方块：沙子、红沙、
 * 沙砾、各色混凝土粉末、本模组的特殊沙子（烈性沙子 / 雷霆沙子 / 猫砂）以及其它模组
 * 的重力方块。规则写在原版公共下落失败路径上，因此新方块会自动适用，无需维护方块 ID 列表。</p>
 *
 * <p>唯一排除项是铁砧变种（{@code #minecraft:anvil}：铁砧、开裂的铁砧、损坏的铁砧），
 * 它们原有的下落、损坏与掉落行为完全保持不变。</p>
 *
 * <p>此外，当 {@code prevent_brushable_blocks_breaking_from_falling} 开启时，受保护的
 * 可刷方块若因落点无法放置而进入掉落分支，也会跳过掉落生成（与关闭该配置时原版的
 * “落地即销毁”一致），避免凭空转化成可疑沙子 / 猫砂物品。</p>
 */
public final class FallingBlockDropRules {
    private FallingBlockDropRules() {
    }

    /**
     * 判断某个方块是否属于本规则的作用范围。
     *
     * <p>除铁砧变种外，所有使用原版下落机制的方块都适用。</p>
     */
    public static boolean isAffectedFallingBlock(BlockState fallingState) {
        return !fallingState.is(BlockTags.ANVIL);
    }

    /**
     * 当下落方块因落点被不完整方块等阻挡而无法正常放置、即将进入原版
     * “将下落方块转化为物品掉落”流程时，是否应跳过掉落物生成。
     *
     * @param fallingState 正在下落方块携带的方块状态
     */
    public static boolean shouldSkipDisplacedDrop(BlockState fallingState) {
        if (!isAffectedFallingBlock(fallingState)) {
            // 铁砧变种保持原版下落 / 损坏 / 掉落行为。
            return false;
        }
        // 配置开启的通用无掉落规则；或“可刷方块防重力摔碎”的配套规则：
        // 让无法放置的可刷方块与关闭防摔碎时一样“落地即销毁”，而不是转化成物品掉落。
        return ModConfigs.NO_DROPS_WHEN_FALLING_BLOCK_DISPLACED.get()
                || BrushableFallProtection.isProtected(fallingState);
    }
}
