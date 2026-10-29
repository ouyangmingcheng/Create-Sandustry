package com.minechai.create_sandustry.content.sandheart;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** 沙心与永久生命上限惩罚相关的登录、重生和实体载入事件。 */
public final class SandHeartEvents {
    private SandHeartEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 永久惩罚必须先于沙心占位修饰符重算，沙心容量才会使用扣除后的外部上限。
            SandSwordHealthPenalty.refresh(player);
            SandHeartManager.refresh(player);
        }
    }

    /**
     * 实体进入世界（含区块重新加载）：如果它已经带着沙心，就重算一次临时属性修饰符。
     *
     * <p>玩家登录时已经完整刷新；这里仍然保留幂等重算，避免维度切换或区块重载后修饰符丢失。</p>
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
        }
        if (entity instanceof ServerPlayer player) {
            SandSwordHealthPenalty.refresh(player);
        }
        if (SandHeartManager.getUnits(entity) > 0) {
            SandHeartManager.refresh(entity);
        }
    }

    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isWasDeath()) {
            // 死亡即清空全部沙心（附件本身也不复制到新玩家）。
            player.setData(com.minechai.create_sandustry.registry.ModAttachments.SAND_HEARTS, 0);
        }
        // 永久拔剑惩罚与沙心临时修饰符都需要在重生后的新玩家实例上重算。
        SandSwordHealthPenalty.refresh(player);
        SandHeartManager.refresh(player);
    }
}
