package com.minechai.create_sandustry.content.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 群友的猪分身。
 *
 * <p>复用 Qunyou 的完整状态机与渲染，但使用独立实体类型、独立属性、
 * 主群友实时同步的阶段与目标，以及永久禁止再次召唤分身的身份标记。</p>
 */
public final class QunyouClone extends Qunyou {
    public QunyouClone(EntityType<? extends QunyouClone> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    @Override
    public boolean isClone() {
        return true;
    }
}
