package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.block.entity.ModBlockEntities;
import com.minechai.create_candustry.entity.ModEntityTypes;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class FlorinolBatteryClientEvents {
    static {
        // Register Catnip's StitchedSprite records while the client mod is
        // loading, before the block atlas is stitched. This mirrors Create's
        // AllSpriteShifts class-initialization timing.
        FlorinolBatterySprites.init();
    }

    private FlorinolBatteryClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FLORINOL_BATTERY.get(), FlorinolBatteryRenderer::new);
        event.registerEntityRenderer(ModEntityTypes.LIVING_SAND.get(), LivingSandRenderer::new);
    }

    @SubscribeEvent
    public static void modifyBakingResult(ModelEvent.ModifyBakingResult event) {
        event.getModels().replaceAll((location, model) -> {
            // ModelResourceLocation#id is the block registry id
            // (createsandustry:florinol_battery), while #variant selects the
            // bottom/top/shape model. Matching the raw model path here was the
            // reason the previous CTModel wrapper was never applied.
            if (CreateSandustry.MODID.equals(location.id().getNamespace())
                    && "florinol_battery".equals(location.id().getPath())) {
                return FlorinolBatteryModel.standard(model);
            }
            return model;
        });
    }
}
