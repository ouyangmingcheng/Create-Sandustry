package com.minechai.create_sandustry.content.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** 客户端谜题音源；用显式距离计算保证声音不超过配置半径。 */
public final class MelodyPuzzleSoundInstance extends AbstractTickableSoundInstance {
    private final Vec3 source;
    private final double range;

    public MelodyPuzzleSoundInstance(SoundEvent sound, BlockPos pos, int range) {
        super(sound, SoundSource.RECORDS, SoundInstance.createUnseededRandom());
        this.source = Vec3.atCenterOf(pos);
        this.range = range;
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override
    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            requestStop();
            return;
        }
        double distance = Math.sqrt(minecraft.player.getEyePosition().distanceToSqr(source));
        this.volume = Mth.clamp(1.0F - (float)(distance / range), 0.0F, 1.0F);
    }

    public void requestStop() {
        stop();
    }
}
