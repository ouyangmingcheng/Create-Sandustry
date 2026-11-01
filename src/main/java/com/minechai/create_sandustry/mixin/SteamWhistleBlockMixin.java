package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.steamwhistle.SteamWhistlePerformanceEvent;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlock;
import com.simibubi.create.content.decoration.steamWhistle.WhistleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在 Create 自己的服务端红石边沿处理完成后发布蒸汽笛演奏事件。 */
@Mixin(WhistleBlock.class)
public abstract class SteamWhistleBlockMixin {
    @Inject(method = "neighborChanged", at = @At("RETURN"))
    private void createsandustry$capturePerformanceStart(
            BlockState previousState,
            Level level,
            BlockPos pos,
            Block block,
            BlockPos fromPos,
            boolean isMoving,
            CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState currentState = level.getBlockState(pos);
        if (!(currentState.getBlock() instanceof WhistleBlock)
                || !previousState.hasProperty(WhistleBlock.POWERED)
                || !currentState.hasProperty(WhistleBlock.POWERED)
                || previousState.getValue(WhistleBlock.POWERED)
                || !currentState.getValue(WhistleBlock.POWERED)) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof WhistleBlockEntity whistle) {
            NeoForge.EVENT_BUS.post(new SteamWhistlePerformanceEvent(
                    serverLevel, pos, whistle, previousState, currentState, fromPos,
                    level.getBlockState(fromPos), block));
        }
    }
}
