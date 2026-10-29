package com.minechai.create_sandustry.content.timebearing;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class TimeBearingEvents {
    private TimeBearingEvents() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        TimeBearingService.initializeServer(event.getServer());
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        TimeBearingService.clearServer(event.getServer());
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        TimeBearingService.tickExistingControllers(event.getServer());
    }

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TimeBearingService.onChunkLoaded(level, event.getChunk().getPos());
        }
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TimeBearingService.onChunkUnloaded(level, event.getChunk().getPos());
        }
    }

    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            TimeBearingService.onLevelUnloaded(level);
        }
    }
}
