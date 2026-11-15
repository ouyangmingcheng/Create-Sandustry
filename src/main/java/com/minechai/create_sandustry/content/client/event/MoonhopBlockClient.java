package com.minechai.create_sandustry.content.client.event;

import com.minechai.create_sandustry.registry.ModBlocks;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** 为 Alpha 180 的月跃石块启用半透明方块渲染。 */
@OnlyIn(Dist.CLIENT)
public final class MoonhopBlockClient {
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(MoonhopBlockClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(
                ModBlocks.MOONHOP_BLOCK.get(),
                RenderType.translucent()));
    }

    private MoonhopBlockClient() {
    }
}
