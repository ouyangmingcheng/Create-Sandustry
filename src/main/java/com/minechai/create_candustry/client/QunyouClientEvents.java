package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.entity.ModEntityTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class QunyouClientEvents {
    private QunyouClientEvents() {
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(QunyouModel.LAYER, () -> QunyouModel.createLayer());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.QUNYOU.get(), QunyouRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.QUNYOU_CLONE.get(), QunyouRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.QUNYOU_CARROT.get(), QunyouCarrotRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.QUNYOU_GOLDEN_CARROT.get(), QunyouCarrotRenderer::new);
    }
}