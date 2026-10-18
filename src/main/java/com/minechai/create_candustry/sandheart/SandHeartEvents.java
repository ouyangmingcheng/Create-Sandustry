package com.minechai.create_candustry.sandheart;

import com.minechai.create_candustry.CreateSandustry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 沙心数据相关事件：玩家登录/重生、以及任何实体进入世界时重新施加沙心属性修饰符。
 *
 * <p>沙心的存档值保存在数据附件 {@code ModAttachments.SAND_HEARTS} 中（玩家不随死亡复制），
 * 而有效生命上限使用的是<b>临时</b>属性修饰符（不写入存档）。因此需要在玩家登录、以及死亡重生
 * （{@link PlayerEvent.Clone}，新玩家实例）时重新计算一次，避免修饰符丢失或重复累加。</p>
 *
 * <p><b>生物</b>：生物没有登录事件，而且区块卸载/重新加载后临时修饰符同样会消失，
 * 因此这里监听 {@link EntityJoinLevelEvent}：只要实体身上已经带着沙心，就在它进入世界时重算一次。
 * 沙心数量本身随实体存档（附件序列化），所以重新加载后挤占的生命上限会被正确恢复，
 * 不会出现“沙心还在、血量却恢复了”的错位。</p>
 */
public final class SandHeartEvents {
    private SandHeartEvents() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SandHeartManager.refresh(player);
        }
    }

    /**
     * 实体进入世界（含区块重新加载）：如果它已经带着沙心，就重算一次临时属性修饰符。
     *
     * <p>对没有沙心的实体只是一次附件读取，代价可以忽略；玩家同样会被重算，且重算是幂等的
     * （{@code addOrUpdateTransientModifier}）。</p>
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity entity)) {
            return;
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
            player.setData(com.minechai.create_candustry.registry.ModAttachments.SAND_HEARTS, 0);
        }
        // 临时属性修饰符不写入存档，重生后需要重新计算一次。
        SandHeartManager.refresh(player);
    }
}
