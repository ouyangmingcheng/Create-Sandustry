package com.minechai.create_sandustry.content.client.event;

import com.minechai.create_sandustry.content.client.render.LivingSandRenderer;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModEntityTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 活沙实体的渲染器注册（原先与弗洛林电池储罐渲染混在同一个客户端事件类里，拆分后独立）。 */
@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class LivingSandClientEvents {
    private LivingSandClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.LIVING_SAND.get(), LivingSandRenderer::new);
    }
}