package com.minechai.create_sandustry.client;

import com.minechai.create_sandustry.foundation.render.GoldenGlintRenderBuffers;
import com.minechai.create_sandustry.content.goldenselection.client.GoldenSelectionEffectRenderer;
import com.minechai.create_sandustry.content.moonhop.client.MoonhopBlockClient;
import com.minechai.create_sandustry.content.moonhop.client.MoonhopBoardItemDecorator;
import com.minechai.create_sandustry.content.sandcasing.client.SandCasingClient;
import com.minechai.create_sandustry.content.sifting.client.SiftingBeltClientEvents;
import com.minechai.create_sandustry.content.thermalbuffer.client.ThermalBufferItemProperties;
import com.minechai.create_sandustry.content.sandheart.client.SandHeartBossOverlay;
import com.minechai.create_sandustry.content.sandmastersword.client.SandBladeCrescentRenderer;
import com.minechai.create_sandustry.content.sandmastersword.client.SandMasterSwordBeamRenderer;
import com.minechai.create_sandustry.infrastructure.ponder.CreateSandustryPonderPlugin;
import com.minechai.create_sandustry.registry.ModEntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

/** Client bootstrap, invoked only on the physical client. */
public final class SandustryClient {
    public static void register(IEventBus modEventBus) {
        SiftingBeltClientEvents.init();
        CreateSandustryPonderPlugin.register();
        modEventBus.addListener(GoldenGlintRenderBuffers::registerRenderBuffers);
        SandCasingClient.register(modEventBus);
        ThermalBufferItemProperties.register(modEventBus);
        GoldenSelectionEffectRenderer.register(modEventBus);
        MoonhopBoardItemDecorator.register(modEventBus);
        MoonhopBlockClient.register(modEventBus);
        // 剑气实体渲染器。
        modEventBus.addListener(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerEntityRenderer(ModEntityTypes.SAND_MASTER_SWORD_BEAM.get(),
                        SandMasterSwordBeamRenderer::new));
        // 锋利剑气生成的半月形剑气。
        modEventBus.addListener(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerEntityRenderer(ModEntityTypes.SAND_BLADE_CRESCENT.get(),
                        SandBladeCrescentRenderer::new));
        // 末影龙 boss 条上的“沙心挤占”叠加绘制（客户端）。
        NeoForge.EVENT_BUS.addListener(SandHeartBossOverlay::onBossBarProgress);
    }

    private SandustryClient() {}
}
