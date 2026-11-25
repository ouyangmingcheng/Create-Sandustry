package com.minechai.create_sandustry.content.melody.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.network.MelodyPuzzleAudioPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class MelodyPuzzleAudioClient {
    private static final Map<BlockPos, MelodyPuzzleSoundInstance> SOUNDS = new ConcurrentHashMap<>();

    public static void handle(MelodyPuzzleAudioPayload payload) {
        stop(payload.pos());
        if (payload.action() == MelodyPuzzleAudioPayload.STOP) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        MelodyPuzzleSoundInstance instance = new MelodyPuzzleSoundInstance(
                payload.sound().value(), payload.pos(), payload.range());
        SOUNDS.put(payload.pos(), instance);
        minecraft.getSoundManager().play(instance);
    }

    private static void stop(BlockPos pos) {
        MelodyPuzzleSoundInstance instance = SOUNDS.remove(pos);
        if (instance != null) {
            instance.requestStop();
        }
    }

    private static void stopAll() {
        for (MelodyPuzzleSoundInstance instance : SOUNDS.values()) {
            instance.requestStop();
        }
        SOUNDS.clear();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        stopAll();
    }

    private MelodyPuzzleAudioClient() {
    }
}
