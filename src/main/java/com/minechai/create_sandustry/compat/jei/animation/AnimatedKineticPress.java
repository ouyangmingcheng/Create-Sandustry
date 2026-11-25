package com.minechai.create_sandustry.compat.jei.animation;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;

import net.minecraft.client.gui.GuiGraphics;

/** Uses the same scale, rotation, lighting and offset as Create's AnimatedSaw. */
public final class AnimatedKineticPress extends AnimatedKinetics {
    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(xOffset, yOffset, 0.0F);
        pose.translate(0.0F, 0.0F, 200.0F);
        pose.translate(2.0F, 22.0F, 0.0F);
        pose.mulPose(Axis.XP.rotationDegrees(-15.5F));
        pose.mulPose(Axis.YP.rotationDegrees(112.5F));
        blockElement(ModBlocks.KINETIC_PRESS.get().defaultBlockState())
                .scale(25)
                .render(graphics);
        pose.popPose();
    }
}