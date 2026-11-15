package com.minechai.create_sandustry.content.moonhop;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Persistent state for one incomplete moonhop board.
 *
 * <p>The list of used Y values preserves the order in which presses occurred
 * and is exposed as an immutable copy. This keeps ItemStack copies independent
 * and prevents mutable state from leaking through Data Components.</p>
 */
public record MoonhopProcessState(int target, int progress, List<Integer> usedY) {
    public static final int OVERWORLD_TARGET = 320;
    public static final int NETHER_TARGET = 128;
    private static final int MAX_USED_Y = 640;

    private static final Codec<MoonhopProcessState> RAW_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("target").forGetter(MoonhopProcessState::target),
            Codec.INT.fieldOf("progress").forGetter(MoonhopProcessState::progress),
            Codec.INT.listOf(1, MAX_USED_Y).fieldOf("used_y").forGetter(MoonhopProcessState::usedY)
    ).apply(instance, MoonhopProcessState::new));

    public static final Codec<MoonhopProcessState> CODEC = RAW_CODEC.flatXmap(
            MoonhopProcessState::validate,
            state -> state.isValid()
                    ? DataResult.success(state)
                    : DataResult.error(() -> "Invalid moonhop process state"));

    private static final StreamCodec<ByteBuf, List<Integer>> USED_Y_STREAM_CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(MAX_USED_Y));

    public static final StreamCodec<ByteBuf, MoonhopProcessState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MoonhopProcessState::target,
            ByteBufCodecs.VAR_INT, MoonhopProcessState::progress,
            USED_Y_STREAM_CODEC, MoonhopProcessState::usedY,
            MoonhopProcessState::new
    ).map(MoonhopProcessState::validatedOrThrow, state -> state);

    public MoonhopProcessState {
        Objects.requireNonNull(usedY, "usedY");
        usedY = List.copyOf(usedY);
    }

    public boolean hasUsed(int y) {
        return usedY.contains(y);
    }

    public boolean isValid() {
        if (target != OVERWORLD_TARGET && target != NETHER_TARGET) {
            return false;
        }
        if (progress < 0 || progress >= target) {
            return false;
        }
        if (usedY.isEmpty() || usedY.size() > MAX_USED_Y) {
            return false;
        }

        long sum = 0L;
        Set<Integer> seen = new HashSet<>();
        for (Integer value : usedY) {
            if (value == null || value <= -target || value >= target) {
                return false;
            }
            if (!seen.add(value)) {
                return false;
            }
            sum += value;
        }
        return sum == progress;
    }

    private static DataResult<MoonhopProcessState> validate(MoonhopProcessState state) {
        return state.isValid()
                ? DataResult.success(state)
                : DataResult.error(() -> "Invalid moonhop process state: target=" + state.target
                        + ", progress=" + state.progress + ", usedY=" + state.usedY);
    }

    private static MoonhopProcessState validatedOrThrow(MoonhopProcessState state) {
        if (!state.isValid()) {
            throw new DecoderException("Invalid moonhop process state: " + state);
        }
        return state;
    }
}

