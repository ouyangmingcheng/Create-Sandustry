package com.minechai.create_candustry.network;

import com.minechai.create_candustry.CreateSandustry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：请求发射一次沙噬剑气（无附加数据）。
 *
 * <p>服务端收到后会完整复核主手物品、攻击冷却等条件，不接受客户端自报的“可以发射”。</p>
 */
public record FireSandBeamPayload() implements CustomPacketPayload {
    public static final Type<FireSandBeamPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "fire_sand_beam"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FireSandBeamPayload> STREAM_CODEC =
            StreamCodec.unit(new FireSandBeamPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}