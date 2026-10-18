package com.minechai.create_candustry.network;

import com.minechai.create_candustry.item.SandMasterSwordEvents;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 沙金工艺的网络包注册（目前只有“发射沙噬剑气”一条客户端 → 服务端请求）。 */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(FireSandBeamPayload.TYPE, FireSandBeamPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        SandMasterSwordEvents.fireBeam(player);
                    }
                }));
    }
}