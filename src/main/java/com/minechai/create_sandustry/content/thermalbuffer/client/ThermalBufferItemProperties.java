package com.minechai.create_sandustry.content.thermalbuffer.client;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.thermalbuffer.ThermalBufferBlockEntity;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModDataComponents;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** 将物品内的连续热值映射为 0～3 级物品模型属性。 */
@OnlyIn(Dist.CLIENT)
public final class ThermalBufferItemProperties {
    public static final ResourceLocation THERMAL_LEVEL = ResourceLocation.fromNamespaceAndPath(
            CreateSandustry.MODID, "thermal_level");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ThermalBufferItemProperties::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.THERMAL_RELAY.get(),
                THERMAL_LEVEL,
                (stack, level, entity, seed) -> ThermalBufferBlockEntity.getRawLevel(
                        stack.getOrDefault(ModDataComponents.THERMAL_HEAT.get(), 0.0D))));
    }

    private ThermalBufferItemProperties() {
    }
}
