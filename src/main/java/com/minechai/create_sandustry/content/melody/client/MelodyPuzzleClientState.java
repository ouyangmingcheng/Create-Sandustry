package com.minechai.create_sandustry.content.melody.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.minechai.create_sandustry.content.melody.MelodyPuzzleState;
import com.minechai.create_sandustry.network.MelodyPuzzleStatePayload;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

/** 客户端只读镜像；后续 HUD 或唱片机界面可直接消费，不参与服务端判定。 */
public final class MelodyPuzzleClientState {
    private static final Map<BlockPos, Snapshot> STATES = new ConcurrentHashMap<>();

    public static void handle(MelodyPuzzleStatePayload payload) {
        if (payload.state() == MelodyPuzzleState.IDLE.ordinal()) {
            STATES.remove(payload.pos());
        } else {
            STATES.put(payload.pos(), new Snapshot(payload.state(), payload.progress(), payload.total()));
        }
    }

    public static @Nullable Snapshot get(BlockPos pos) {
        return STATES.get(pos);
    }

    public record Snapshot(int state, int progress, int total) {
        public MelodyPuzzleState stateValue() {
            MelodyPuzzleState[] values = MelodyPuzzleState.values();
            return state >= 0 && state < values.length ? values[state] : MelodyPuzzleState.IDLE;
        }
    }

    private MelodyPuzzleClientState() {
    }
}
