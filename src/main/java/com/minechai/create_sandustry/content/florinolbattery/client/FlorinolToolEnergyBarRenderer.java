package com.minechai.create_sandustry.content.florinolbattery.client;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;
import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergySnapshot;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

/** Draws one FE bar in the GUI where the portable drill normally draws fuel and water bars. */
@OnlyIn(Dist.CLIENT)
public final class FlorinolToolEnergyBarRenderer {
    private static final float WIDTH = 13.0F;
    private static final float HEIGHT = 2.0F;

    public static void render(ItemStack stack, PoseStack poseStack, MultiBufferSource buffer) {
        FlorinolToolEnergySnapshot snapshot = FlorinolToolEnergy.getEnergySnapshot(stack);
        float ratio = snapshot == null ? 0.0F : snapshot.ratio();
        int color = !snapshotAvailable(snapshot) ? 0x6B6B6B
                : ratio > 0.5F ? 0x35D6E9
                : ratio > 0.2F ? 0xFFC24A
                : 0xFF5A4A;

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 1.0F);
        poseStack.scale(0.0625F, 0.0625F, -0.0625F);
        drawBar(poseStack, buffer, -6.0F, -7.0F, ratio, color);
        poseStack.popPose();
    }

    private static boolean snapshotAvailable(FlorinolToolEnergySnapshot snapshot) {
        return snapshot != null && snapshot.available();
    }

    private static void drawBar(PoseStack poseStack, MultiBufferSource buffer, float x, float y,
                                float ratio, int color) {
        float filled = Mth.floor(WIDTH * Mth.clamp(ratio, 0.0F, 1.0F));
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());

        addQuad(consumer, matrix, x, y, x + WIDTH, y + HEIGHT, 0.0F, 0, 0, 0, 255);
        int red = color >> 16 & 0xFF;
        int green = color >> 8 & 0xFF;
        int blue = color & 0xFF;
        addQuad(consumer, matrix, x, y + HEIGHT * 0.5F, x + filled, y + HEIGHT,
                -0.5F, red, green, blue, 255);
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix,
                                float x1, float y1, float x2, float y2, float z,
                                int red, int green, int blue, int alpha) {
        consumer.addVertex(matrix, x1, y1, z).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x2, y1, z).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x2, y2, z).setColor(red, green, blue, alpha);
        consumer.addVertex(matrix, x1, y2, z).setColor(red, green, blue, alpha);
    }

    private FlorinolToolEnergyBarRenderer() {
    }
}