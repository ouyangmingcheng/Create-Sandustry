package com.minechai.create_candustry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.minechai.create_candustry.item.SandMasterSwordEvents;

import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * 服务端兜底：即使客户端被修改/被其它模组绕过，也不允许用 Q 丢弃沙噬剑（两种形态）。
 *
 * <p>拦截的是原版丢弃动作包（{@code DROP_ITEM} 与整组丢弃的 {@code DROP_ALL_ITEMS}），
 * 位置在 {@code Player#drop} 之前，因此物品不会被移出物品栏。</p>
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void createsandustry$blockProtectedSwordDrop(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        ServerboundPlayerActionPacket.Action action = packet.getAction();
        if (action != ServerboundPlayerActionPacket.Action.DROP_ITEM
                && action != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            return;
        }
        if (this.player != null && SandMasterSwordEvents.isProtectedSword(this.player.getMainHandItem())) {
            ci.cancel();
        }
    }
}