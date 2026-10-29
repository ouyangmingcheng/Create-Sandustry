package com.minechai.create_sandustry.content.client.event;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModFluids;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = CreateSandustry.MODID, value = Dist.CLIENT)
public final class FluidClientExtensions {
    private static final ResourceLocation FLORINOL_STILL =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/florinol_still");
    private static final ResourceLocation FLORINOL_FLOW =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/florinol_flow");

    private FluidClientExtensions() {
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return FLORINOL_STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLORINOL_FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xFFFFFFFF;
            }

            @Override
            public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance,
                                           float darkenWorldAmount, Vector3f fluidFogColor) {
                return new Vector3f(0.045F, 0.008F, 0.060F);
            }

        }, ModFluids.FLORINOL_TYPE.get());
    }
}
