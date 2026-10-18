package com.minechai.create_candustry.entity;

import com.minechai.create_candustry.CreateSandustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CreateSandustry.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LivingSand>> LIVING_SAND =
            ENTITY_TYPES.register("living_sand", () -> EntityType.Builder
                    .of(LivingSand::new, MobCategory.MONSTER)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .build("living_sand"));

    /** 沙噬剑剑气：直线飞行的服务端权威弹射实体（不保存到磁盘）。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SandMasterSwordBeam>> SAND_MASTER_SWORD_BEAM =
            ENTITY_TYPES.register("sand_master_sword_beam", () -> EntityType.Builder
                    .<SandMasterSwordBeam>of(SandMasterSwordBeam::new, MobCategory.MISC)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .noSave()
                    .build("sand_master_sword_beam"));
    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LIVING_SAND.get(), LivingSand.createAttributes().build());
    }

    private ModEntityTypes() {
    }
}
