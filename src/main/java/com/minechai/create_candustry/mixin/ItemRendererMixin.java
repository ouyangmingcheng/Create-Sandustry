package com.minechai.create_candustry.mixin;

import com.minechai.create_candustry.client.GoldenGlintContext;
import com.minechai.create_candustry.client.GoldenGlintRenderType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void createsandustry$beginGoldenGlint(ItemStack itemStack, ItemDisplayContext displayContext,
                                                    boolean leftHand, PoseStack poseStack,
                                                    MultiBufferSource bufferSource, int combinedLight,
                                                    int combinedOverlay, BakedModel model, CallbackInfo ci) {
        GoldenGlintContext.begin(itemStack);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void createsandustry$endGoldenGlint(ItemStack itemStack, ItemDisplayContext displayContext,
                                                 boolean leftHand, PoseStack poseStack,
                                                 MultiBufferSource bufferSource, int combinedLight,
                                                 int combinedOverlay, BakedModel model, CallbackInfo ci) {
        GoldenGlintContext.end();
    }

    @Inject(method = "getFoilBuffer", at = @At("HEAD"), cancellable = true)
    private static void createsandustry$goldenGlint(MultiBufferSource bufferSource, RenderType renderType,
                                                     boolean isItem, boolean glint,
                                                     CallbackInfoReturnable<VertexConsumer> cir) {
        if (!glint || !GoldenGlintContext.isGolden()) {
            return;
        }

        RenderType goldenGlint = Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
                ? GoldenGlintRenderType.GOLDEN_GLINT_TRANSLUCENT
                : GoldenGlintRenderType.GOLDEN_GLINT;
        cir.setReturnValue(VertexMultiConsumer.create(
                bufferSource.getBuffer(goldenGlint),
                bufferSource.getBuffer(renderType)));
    }

    @Inject(method = "getFoilBufferDirect", at = @At("HEAD"), cancellable = true)
    private static void createsandustry$goldenGlintDirect(MultiBufferSource bufferSource, RenderType renderType,
                                                           boolean noEntity, boolean withGlint,
                                                           CallbackInfoReturnable<VertexConsumer> cir) {
        if (!withGlint || !GoldenGlintContext.isGolden()) {
            return;
        }

        RenderType goldenGlint = noEntity
                ? GoldenGlintRenderType.GOLDEN_GLINT
                : GoldenGlintRenderType.GOLDEN_ENTITY_GLINT_DIRECT;
        cir.setReturnValue(VertexMultiConsumer.create(
                bufferSource.getBuffer(goldenGlint),
                bufferSource.getBuffer(renderType)));
    }
}