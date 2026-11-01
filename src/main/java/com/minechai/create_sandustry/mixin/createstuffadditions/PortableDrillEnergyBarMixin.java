package com.minechai.create_sandustry.mixin.createstuffadditions;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;
import com.minechai.create_sandustry.content.client.render.FlorinolToolEnergyBarRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces Create SA's two fluid bars with one FE bar for bound tools. */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.item.renderer.SteamBarItemRenderer", remap = false)
public abstract class PortableDrillEnergyBarMixin {
    @Inject(method = "renderBars", at = @At("HEAD"), cancellable = true)
    private static void createsandustry$renderFlorinolBar(ItemStack stack, PoseStack poseStack,
                                                           MultiBufferSource buffer, int light,
                                                           CallbackInfo ci) {
        if (FlorinolToolEnergy.hasBinding(stack)) {
            FlorinolToolEnergyBarRenderer.render(stack, poseStack, buffer);
            ci.cancel();
        }
    }
}