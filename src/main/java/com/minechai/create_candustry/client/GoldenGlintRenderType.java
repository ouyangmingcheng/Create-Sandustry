package com.minechai.create_candustry.client;

import com.minechai.create_candustry.config.ModConfigs;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class GoldenGlintRenderType extends RenderType {
    public static final RenderType GOLDEN_GLINT =
            new GoldenGlintRenderType("createsandustry_golden_glint", Mode.GLINT);
    public static final RenderType GOLDEN_GLINT_TRANSLUCENT =
            new GoldenGlintRenderType("createsandustry_golden_glint_translucent", Mode.GLINT_TRANSLUCENT);
    public static final RenderType GOLDEN_ENTITY_GLINT_DIRECT =
            new GoldenGlintRenderType("createsandustry_golden_entity_glint_direct", Mode.ENTITY_GLINT_DIRECT);

    private GoldenGlintRenderType(String name, Mode mode) {
        super(name, DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1536, false, false,
                () -> setup(mode), () -> clear(mode));
    }

    private static void setup(Mode mode) {
        switch (mode) {
            case GLINT -> {
                RenderSystem.setShader(GameRenderer::getRendertypeGlintShader);
                RenderSystem.setShaderTexture(0, ItemRenderer.ENCHANTED_GLINT_ITEM);
                RenderStateShard.GLINT_TEXTURING.setupState.run();
            }
            case GLINT_TRANSLUCENT -> {
                RenderSystem.setShader(GameRenderer::getRendertypeGlintTranslucentShader);
                RenderSystem.setShaderTexture(0, ItemRenderer.ENCHANTED_GLINT_ITEM);
                RenderStateShard.GLINT_TEXTURING.setupState.run();
            }
            case ENTITY_GLINT_DIRECT -> {
                RenderSystem.setShader(GameRenderer::getRendertypeEntityGlintDirectShader);
                RenderSystem.setShaderTexture(0, ItemRenderer.ENCHANTED_GLINT_ENTITY);
                RenderStateShard.ENTITY_GLINT_TEXTURING.setupState.run();
            }
        }

        int color = ModConfigs.GOLDEN_SELECTION_GLINT_COLOR.get();
        float red = (float) ((color >> 16) & 0xFF) / 255.0F;
        float green = (float) ((color >> 8) & 0xFF) / 255.0F;
        float blue = (float) (color & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(red, green, blue, 1.0F);

        RenderStateShard.GLINT_TRANSPARENCY.setupState.run();
        RenderStateShard.NO_CULL.setupState.run();
        RenderStateShard.EQUAL_DEPTH_TEST.setupState.run();
        RenderStateShard.COLOR_WRITE.setupState.run();
        if (mode == Mode.GLINT_TRANSLUCENT) {
            RenderStateShard.ITEM_ENTITY_TARGET.setupState.run();
        }
    }

    private static void clear(Mode mode) {
        if (mode == Mode.GLINT_TRANSLUCENT) {
            RenderStateShard.ITEM_ENTITY_TARGET.clearState.run();
        }
        RenderStateShard.COLOR_WRITE.clearState.run();
        if (mode == Mode.ENTITY_GLINT_DIRECT) {
            RenderStateShard.ENTITY_GLINT_TEXTURING.clearState.run();
        } else {
            RenderStateShard.GLINT_TEXTURING.clearState.run();
        }
        RenderStateShard.EQUAL_DEPTH_TEST.clearState.run();
        RenderStateShard.NO_CULL.clearState.run();
        RenderStateShard.GLINT_TRANSPARENCY.clearState.run();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private enum Mode {
        GLINT,
        GLINT_TRANSLUCENT,
        ENTITY_GLINT_DIRECT
    }
}