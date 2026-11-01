package com.minechai.create_sandustry.network;

import com.minechai.create_sandustry.content.client.event.MelodyPuzzleAudioClient;
import com.minechai.create_sandustry.content.client.event.MelodyPuzzleClientState;
import com.minechai.create_sandustry.content.client.event.SwordPullClient;
import com.minechai.create_sandustry.content.item.SandMasterSwordEvents;
import com.minechai.create_sandustry.content.sandheart.SandSwordPullManager;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 沙金工艺网络包注册。 */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("5");
        registrar.playToServer(FireSandBeamPayload.TYPE, FireSandBeamPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        SandMasterSwordEvents.fireBeam(player);
                    }
                }));
        registrar.playToServer(SwordPullCancelPayload.TYPE, SwordPullCancelPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        SandSwordPullManager.cancelFromClient(player, payload.pos());
                    }
                }));
        registrar.playToClient(SwordPullStatePayload.TYPE, SwordPullStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> SwordPullClient.handleState(payload)));
        registrar.playToClient(SwordPullHurtPayload.TYPE, SwordPullHurtPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(SwordPullClient::handleHurt));
        registrar.playToClient(MelodyPuzzleStatePayload.TYPE, MelodyPuzzleStatePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MelodyPuzzleClientState.handle(payload)));
        registrar.playToClient(MelodyPuzzleAudioPayload.TYPE, MelodyPuzzleAudioPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> MelodyPuzzleAudioClient.handle(payload)));
    }
}
