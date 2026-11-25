package com.minechai.create_sandustry.content.kineticpress;

import com.minechai.create_sandustry.registry.ModSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** One quiet cue per processing wave, never per item or while waiting. */
public final class KineticPressSounds {
    private static final int COOLDOWN_TICKS = 8;
    private static final float START_VOLUME = 0.22F;
    private static final float IMPACT_VOLUME = 0.38F;
    private long lastStartTick = Long.MIN_VALUE;
    private long lastImpactTick = Long.MIN_VALUE;

    public void tick(Level level, BlockPos pos, boolean started, boolean completed) {
        if (level.isClientSide) return;
        long now = level.getGameTime();
        // Completion takes priority if a previous wave lands as new work starts.
        if (completed) {
            if (ready(now, lastImpactTick)) {
                playImpact(level, pos);
                lastImpactTick = now;
            }
        } else if (started && ready(now, lastStartTick) && ready(now, lastImpactTick)) {
            playStart(level, pos);
            lastStartTick = now;
        }
    }

    private static boolean ready(long now, long previous) {
        return previous == Long.MIN_VALUE || now < previous || now - previous >= COOLDOWN_TICKS;
    }

    private static void playStart(Level level, BlockPos pos) {
        play(level, pos, ModSounds.KINETIC_PRESS_START.get(), START_VOLUME);
    }

    private static void playImpact(Level level, BlockPos pos) {
        play(level, pos, ModSounds.KINETIC_PRESS_IMPACT.get(), IMPACT_VOLUME);
    }

    private static void play(Level level, BlockPos pos, SoundEvent sound, float volume) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, volume, 1.0F);
    }
}
