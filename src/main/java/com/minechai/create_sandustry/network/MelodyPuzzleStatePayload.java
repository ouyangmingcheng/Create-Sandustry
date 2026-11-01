package com.minechai.create_sandustry.network;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** 服务端权威谜题状态的客户端展示同步。 */
public record MelodyPuzzleStatePayload(BlockPos pos, int state, int progress, int total)
        implements CustomPacketPayload {
    public static final Type<MelodyPuzzleStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "melody_puzzle_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MelodyPuzzleStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, MelodyPuzzleStatePayload::pos,
                    ByteBufCodecs.VAR_INT, MelodyPuzzleStatePayload::state,
                    ByteBufCodecs.VAR_INT, MelodyPuzzleStatePayload::progress,
                    ByteBufCodecs.VAR_INT, MelodyPuzzleStatePayload::total,
                    MelodyPuzzleStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
