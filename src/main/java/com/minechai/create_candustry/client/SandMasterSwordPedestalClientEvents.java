package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.block.entity.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class SandMasterSwordPedestalClientEvents {
    private SandMasterSwordPedestalClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SAND_MASTER_SWORD_PEDESTAL.get(),
                SandMasterSwordPedestalRenderer::new);
    }
}