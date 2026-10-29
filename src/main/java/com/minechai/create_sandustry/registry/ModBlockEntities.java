package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.block.entity.ThermalBufferBlockEntity;
import com.minechai.create_sandustry.content.block.entity.SandMasterSwordPedestalBlockEntity;
import com.minechai.create_sandustry.content.block.entity.LivingSandBlockEntity;
import com.minechai.create_sandustry.content.block.entity.FlorinolBatteryBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, com.minechai.create_sandustry.CreateSandustry.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LivingSandBlockEntity>> LIVING_SAND =
            BLOCK_ENTITY_TYPES.register(
                    "living_sand",
                    () -> BlockEntityType.Builder.of(LivingSandBlockEntity::new, ModBlocks.LIVING_SAND.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlorinolBatteryBlockEntity>> FLORINOL_BATTERY =
            BLOCK_ENTITY_TYPES.register(
                    "florinol_battery",
                    () -> BlockEntityType.Builder.of(FlorinolBatteryBlockEntity::new, ModBlocks.FLORINOL_BATTERY.get())
                            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SandMasterSwordPedestalBlockEntity>>
            SAND_MASTER_SWORD_PEDESTAL = BLOCK_ENTITY_TYPES.register(
                    "sand_master_sword_pedestal",
                    () -> BlockEntityType.Builder.of(SandMasterSwordPedestalBlockEntity::new,
                            ModBlocks.SAND_MASTER_SWORD_PEDESTAL.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ThermalBufferBlockEntity>> THERMAL_RELAY =
            BLOCK_ENTITY_TYPES.register(
                    "thermal_relay",
                    () -> BlockEntityType.Builder.of(ThermalBufferBlockEntity::new, ModBlocks.THERMAL_RELAY.get())
                            .build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    private ModBlockEntities() {
    }
}
