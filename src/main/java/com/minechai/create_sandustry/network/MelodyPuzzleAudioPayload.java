package com.minechai.create_sandustry.network;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/** 谜题音频的 16 格定向播放/停止包。 */
public record MelodyPuzzleAudioPayload(BlockPos pos, Holder<SoundEvent> sound, int action, int range)
        implements CustomPacketPayload {
    public static final int PLAY = 0;
    public static final int STOP = 1;

    public static final Type<MelodyPuzzleAudioPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "melody_puzzle_audio"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MelodyPuzzleAudioPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MelodyPuzzleAudioPayload::pos,
                    SoundEvent.STREAM_CODEC, MelodyPuzzleAudioPayload::sound,
                    ByteBufCodecs.VAR_INT, MelodyPuzzleAudioPayload::action,
                    ByteBufCodecs.VAR_INT, MelodyPuzzleAudioPayload::range,
                    MelodyPuzzleAudioPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
