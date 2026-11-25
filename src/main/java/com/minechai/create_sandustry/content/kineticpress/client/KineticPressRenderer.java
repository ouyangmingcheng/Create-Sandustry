package com.minechai.create_sandustry.content.kineticpress.client;

import java.util.List;

import com.minechai.create_sandustry.content.kineticpress.KineticPressAnimationRenderState;
import com.minechai.create_sandustry.content.kineticpress.KineticPressBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class KineticPressRenderer extends SafeBlockEntityRenderer<KineticPressBlockEntity> {
    private static final double MAX_RENDER_DISTANCE_SQUARED = 16.0D * 16.0D;

    private final ItemRenderer itemRenderer;

    public KineticPressRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    protected void renderSafe(KineticPressBlockEntity press, float partialTick, PoseStack poseStack,
                              MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (!shouldRender(press, camera)) {
            return;
        }

        List<KineticPressAnimationRenderState> animations = press.getAnimationRenderStates();
        if (animations.isEmpty()) {
            return;
        }

        // Interpolate between two synchronized values. Partial ticks never advance
        // server progress, and a powered press holds the latest synchronized endpoint.
        double interpolation = press.isPowered() ? 1.0D : Math.clamp(partialTick, 0.0F, 1.0F);
        for (KineticPressAnimationRenderState animation : animations) {
            if (animation.stack().isEmpty() || animation.totalTime() <= 0) {
                continue;
            }
            double elapsedTicks = Math.min(animation.totalTime(),
                    animation.progressAt((float) interpolation));
            double workingHeight = Math.max(0, animation.workingHeight());
            double startY = 1.0D + workingHeight;
            double renderY = KineticPressAnimation.getPosition(
                    startY, elapsedTicks, animation.totalTime(), workingHeight);

            poseStack.pushPose();
            try {
                poseStack.translate(0.5D, renderY, 0.5D);
                poseStack.scale(0.5F, 0.5F, 0.5F);
                this.itemRenderer.renderStatic(animation.stack(), ItemDisplayContext.FIXED,
                        packedLight, packedOverlay, poseStack, buffer, press.getLevel(),
                        animation.id());
            } finally {
                poseStack.popPose();
            }
        }
    }

    @Override
    public boolean shouldRender(KineticPressBlockEntity press, Vec3 cameraPos) {
        // A viewer standing beside the input of a 23-block column must still
        // see the fall, even when the base itself is more than 16 blocks away.
        return getRenderBoundingBox(press).distanceToSqr(cameraPos) <= MAX_RENDER_DISTANCE_SQUARED;
    }

    @Override
    public AABB getRenderBoundingBox(KineticPressBlockEntity press) {
        int animationHeight = press.hasActiveAnimation()
                ? press.getAnimationMaxWorkingHeight()
                : press.getWorkingHeight();
        return new AABB(press.getBlockPos())
                .expandTowards(0.0D, animationHeight + 2.0D, 0.0D)
                .inflate(1.0D);
    }
}
