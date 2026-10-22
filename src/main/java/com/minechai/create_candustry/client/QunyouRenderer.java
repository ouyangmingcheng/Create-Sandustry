package com.minechai.create_candustry.client;

import com.minechai.create_candustry.entity.Qunyou;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** 群友渲染器：原版猪几何和原版猪纹理，仅使用 QunyouModel 的状态动画。 */
public final class QunyouRenderer<T extends Qunyou> extends MobRenderer<T, QunyouModel<T>> {
    private static final ResourceLocation PIG_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/pig/pig.png");

    public QunyouRenderer(EntityRendererProvider.Context context) {
        super(context, new QunyouModel<>(context.bakeLayer(QunyouModel.LAYER)), 0.7F);
        this.addLayer(new QunyouShieldLayer(this, context.getItemInHandRenderer()));
        this.addLayer(new QunyouEatingLayer(this, context.getItemInHandRenderer()));
        this.addLayer(new QunyouElytraLayer(this, context.getModelSet()));
    }

    @Override
    public ResourceLocation getTextureLocation(T entity) {
        return PIG_TEXTURE;
    }

    @Override
    protected boolean shouldShowName(T entity) {
        return false;
    }
}