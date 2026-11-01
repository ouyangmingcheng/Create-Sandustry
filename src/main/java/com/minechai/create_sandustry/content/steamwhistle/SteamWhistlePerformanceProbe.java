package com.minechai.create_sandustry.content.steamwhistle;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 仅用于本阶段验证的服务端捕获器；不会参与后续唱片机谜题判定。 */
public final class SteamWhistlePerformanceProbe {
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateSandustry/SteamWhistleProbe");
    private static final List<SteamWhistlePerformanceEvent> CAPTURED = new ArrayList<>();
    private static boolean capturing;

    public static void startCapture() {
        CAPTURED.clear();
        capturing = true;
    }

    public static void stopCapture() {
        capturing = false;
        CAPTURED.clear();
    }

    public static void onPerformance(SteamWhistlePerformanceEvent event) {
        if (!capturing) {
            return;
        }
        CAPTURED.add(event);
        LOGGER.info(
                "steam whistle: pos={} size={} pitch={} pitchId={} midi={} sourcePos={} sourceBlock={} audible={} tick={}",
                event.getWhistlePos(), event.getSize(), event.getPitch(), event.getPitchId(),
                event.getMidiNote(), event.getSourcePos(), event.getSourceState().getBlock().getName().getString(),
                event.isAudibleOnServer(), event.getGameTime());
    }

    public static List<SteamWhistlePerformanceEvent> eventsAt(ServerLevel level, BlockPos absoluteWhistlePos) {
        List<SteamWhistlePerformanceEvent> matches = new ArrayList<>();
        for (SteamWhistlePerformanceEvent event : CAPTURED) {
            if (event.getLevel() == level && event.getWhistlePos().equals(absoluteWhistlePos)) {
                matches.add(event);
            }
        }
        return matches;
    }

    private SteamWhistlePerformanceProbe() {
    }
}
