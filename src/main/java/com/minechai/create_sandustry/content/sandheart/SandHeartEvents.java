package com.minechai.create_sandustry.content.sandheart;

import com.minechai.create_sandustry.content.sandmastersword.SandSwordHealthPenalty;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModMobEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
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

    /**
     * 玩家在“沙化之兆”期间被不死图腾救下时，将当前沙心转化为等量沙之心掉落。
     *
     * <p>沙心内部按半颗心计数；掉落数量按 HUD 显示的完整/半颗沙心向上取整。
     * 掉落前会清空玩家的沙心，既让沙化之兆立即安全解除，也保证同一次状态最多结算一次，
     * 不会因继续持有不死图腾而重复复制沙之心。</p>
     */
    public static void onTotemUsed(LivingUseTotemEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasEffect(ModMobEffects.SAND_HARDCORE)) {
            return;
        }

        int units = SandHeartManager.getUnits(player);
        if (units <= 0) {
            return;
        }

        int hearts = Mth.ceil(units / (double) SandHeartManager.UNITS_PER_HEART);
        SandHeartManager.setUnits(player, 0);

        int maxStackSize = new ItemStack(ModItems.SAND_HEART.get()).getMaxStackSize();
        for (int remaining = hearts; remaining > 0; remaining -= maxStackSize) {
            int count = Math.min(remaining, maxStackSize);
            player.drop(new ItemStack(ModItems.SAND_HEART.get(), count), false);
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
