package com.minechai.create_sandustry.content.sandcasing.client;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.encasing.CasingConnectivity;
import com.simibubi.create.content.decoration.encasing.EncasedCTBehaviour;
import com.simibubi.create.content.fluids.pipes.EncasedPipeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers Create's native connected-texture model wrapper and casing connectivity for sand casing. */
public final class SandCasingClient {
    private static final ResourceLocation SAND_CASING_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/sand_casing");
    private static final ResourceLocation SAND_CASING_CONNECTED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/sand_casing_connected");

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(SandCasingClient::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            CTSpriteShiftEntry shift = CTSpriteShifter.getCT(AllCTTypes.OMNIDIRECTIONAL,
                    SAND_CASING_TEXTURE, SAND_CASING_CONNECTED_TEXTURE);
            CasingConnectivity connectivity = CreateClient.CASING_CONNECTIVITY;

            Block casing = ModBlocks.SAND_CASING.get();
            Block encasedShaft = ModBlocks.SAND_ENCASED_SHAFT.get();
            Block encasedPipe = ModBlocks.SAND_ENCASED_FLUID_PIPE.get();

            connectivity.makeCasing(casing, shift);
            connectivity.make(encasedShaft, shift,
                    (state, face) -> face.getAxis() != state.getValue(EncasedShaftBlock.AXIS));
            connectivity.make(encasedPipe, shift,
                    (state, face) -> !state.getValue(EncasedPipeBlock.FACING_TO_PROPERTY_MAP.get(face)));

            ConnectedTextureBehaviour behaviour = new EncasedCTBehaviour(shift);
            registerModel(casing, behaviour);
            registerModel(encasedShaft, behaviour);
            registerModel(encasedPipe, behaviour);
        });
    }

    private static void registerModel(Block block, ConnectedTextureBehaviour behaviour) {
        CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(BuiltInRegistries.BLOCK.getKey(block), model -> new CTModel(model, behaviour));
    }

    private SandCasingClient() {
    }
}