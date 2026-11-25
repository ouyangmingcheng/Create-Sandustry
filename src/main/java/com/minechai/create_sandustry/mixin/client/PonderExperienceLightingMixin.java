package com.minechai.create_sandustry.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.scene.Selection;
import net.createmod.ponder.foundation.element.WorldSectionElementImpl;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve the unshaded experience models in the Golden Experience demonstration. */
@Mixin(value = WorldSectionElementImpl.class, remap = false)
public abstract class PonderExperienceLightingMixin {
    @Unique
    private static final ResourceLocation createsandustry$SCENE =
            ResourceLocation.fromNamespaceAndPath("createsandustry", "golden_experience");
    @Unique
    private static final BlockPos createsandustry$DEMO_BLOCK = new BlockPos(2, 1, 2);

    @Shadow
    Selection section;

    @ModifyExpressionValue(method = "renderLayer", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/render/SuperByteBuffer;light(I)Lnet/createmod/catnip/render/SuperByteBuffer;"))
    private SuperByteBuffer createsandustry$preserveExperienceBrightness(
            SuperByteBuffer buffer, PonderLevel world, MultiBufferSource renderBuffers,
            RenderType layer, GuiGraphics graphics, float fade, float partialTicks) {
        if (world.scene == null || !createsandustry$SCENE.equals(world.scene.getId())
                || section == null || !section.test(createsandustry$DEMO_BLOCK)) {
            return buffer;
        }
        var state = world.getBlockState(createsandustry$DEMO_BLOCK);
        if (state.is(AllBlocks.EXPERIENCE_BLOCK.get()) || state.is(ModBlocks.GOLDEN_EXPERIENCE_BLOCK.get())) {
            // Ponder already supplies full-bright light coordinates. Catnip's extra
            // diffuse multiplier can still darken shade:false faces under GUI lights.
            // The block is an independent section, so the floor is unaffected.
            return buffer.disableDiffuse();
        }
        return buffer;
    }
}
