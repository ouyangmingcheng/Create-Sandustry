package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.block.ModBlocks;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class FlorinolBatteryClientSetup {
    private FlorinolBatteryClientSetup() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FlorinolBatterySprites.init();
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.GOLDEN_OAK_SAPLING.get(), RenderType.cutout());
    }
}

