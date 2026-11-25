package com.minechai.create_sandustry.mixin.client;

import com.minechai.create_sandustry.content.sifting.client.SiftingBeltClientEvents;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.content.kinetics.belt.BeltSlope;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.createmod.catnip.render.CachedBuffers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 直接挂到 Create 的 BeltRenderer 上逐帧绘制溜槽嘴。
 * 不走 ModelData 缓存，因此附件同步后的下一帧即可显示。
 */
@Mixin(BeltRenderer.class)
public abstract class BeltRendererSiftingMixin {
    @Inject(method = "renderSafe", at = @At("TAIL"))
    private void createsandustry$renderSiftingChuteMouth(BeltBlockEntity belt, float partialTicks, PoseStack pose,
                                                          MultiBufferSource buffer, int light, int overlay,
                                                          CallbackInfo ci) {
        // Create 的传送带以 controller 为整条带的渲染锚点。
        // 由 controller 统一绘制所有筛振段，避免非 controller 段不执行/不在正确位置。
        if (!belt.isController() || belt.getLevel() == null) {
            return;
        }

        Level level = belt.getLevel();
        for (BlockPos segmentPos : BeltBlock.getBeltChain(level, belt.getBlockPos())) {
            BeltBlockEntity segment = BeltHelper.getSegmentBE(level, segmentPos);
            if (segment == null
                    || !segment.hasData(ModAttachments.SIFTING_BELT)
                    || !segment.getData(ModAttachments.SIFTING_BELT)) {
                continue;
            }

            BlockState state = segment.getBlockState();
            if (!state.hasProperty(BeltBlock.CASING) || !state.getValue(BeltBlock.CASING)) {
                continue;
            }
            if (state.getValue(BeltBlock.SLOPE) != BeltSlope.HORIZONTAL) {
                continue;
            }

            int segmentLight = LevelRenderer.getLightColor(level, segmentPos);
            boolean brass = segment.hasData(ModAttachments.SIFTING_BELT_BRASS)
                    && segment.getData(ModAttachments.SIFTING_BELT_BRASS);
            boolean connectedBrass = segment.hasData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS)
                    && segment.getData(ModAttachments.SIFTING_BELT_CONNECTED_BRASS)
                    && SiftingBeltClientEvents.CONNECTED_BRASS_CHUTE_MOUTH != null;
            boolean locked = level.hasNeighborSignal(segmentPos);
            pose.pushPose();
            pose.translate(
                    segmentPos.getX() - belt.getBlockPos().getX(),
                    segmentPos.getY() - belt.getBlockPos().getY(),
                    segmentPos.getZ() - belt.getBlockPos().getZ());
            var mouth = connectedBrass
                    ? SiftingBeltClientEvents.CONNECTED_BRASS_CHUTE_MOUTH
                    : brass
                        ? locked
                            ? SiftingBeltClientEvents.SMART_CHUTE_POWERED_MOUTH
                            : SiftingBeltClientEvents.SMART_CHUTE_MOUTH
                        : SiftingBeltClientEvents.CHUTE_MOUTH;
            CachedBuffers.partial(mouth, state)
                    .light(segmentLight)
                    .renderInto(pose, buffer.getBuffer(RenderType.solid()));
            pose.popPose();
        }
    }
}
