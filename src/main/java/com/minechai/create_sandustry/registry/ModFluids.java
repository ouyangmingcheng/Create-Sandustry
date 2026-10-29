package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, CreateSandustry.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CreateSandustry.MODID);

    public static final DeferredHolder<FluidType, FluidType> FLORINOL_TYPE = FLUID_TYPES.register(
            "florinol",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.createsandustry.florinol")
                    .fallDistanceModifier(0.0F)
                    .canConvertToSource(false)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .density(1000)
                    .viscosity(1000)
                    .temperature(300)));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Source> FLORINOL = FLUIDS.register(
            "florinol",
            () -> new BaseFlowingFluid.Source(createProperties()));

    public static final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> FLOWING_FLORINOL = FLUIDS.register(
            "flowing_florinol",
            () -> new BaseFlowingFluid.Flowing(createProperties()));

    private static BaseFlowingFluid.Properties createProperties() {
        return new BaseFlowingFluid.Properties(
                FLORINOL_TYPE::get,
                FLORINOL::get,
                FLOWING_FLORINOL::get)
                .bucket(ModItems.FLORINOL_BUCKET)
                .block(ModBlocks.FLORINOL)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(25)
                .explosionResistance(1.0F);
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    private ModFluids() {
    }
}
