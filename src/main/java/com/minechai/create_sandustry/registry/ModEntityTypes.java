package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.entity.SandMasterSwordBeam;
import com.minechai.create_sandustry.content.entity.SandBladeCrescent;
import com.minechai.create_sandustry.content.entity.QunyouGoldenCarrotProjectile;
import com.minechai.create_sandustry.content.entity.QunyouClone;
import com.minechai.create_sandustry.content.entity.QunyouCarrotProjectile;
import com.minechai.create_sandustry.content.entity.Qunyou;
import com.minechai.create_sandustry.content.entity.LivingSand;
import com.minechai.create_sandustry.CreateSandustry;

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

    /** 半月形剑气：锋利原始剑气命中后生成两道，由服务端追踪目标或盘旋消散。 */
    public static final DeferredHolder<EntityType<?>, EntityType<SandBladeCrescent>> SAND_BLADE_CRESCENT =
            ENTITY_TYPES.register("sand_blade_crescent", () -> EntityType.Builder
                    .<SandBladeCrescent>of(SandBladeCrescent::new, MobCategory.MISC)
                    .sized(0.8F, 0.8F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .build("sand_blade_crescent"));

    /** 独立 Boss「群友」：原版猪外观，由沙金工艺自己的状态机驱动。 */
    public static final DeferredHolder<EntityType<?>, EntityType<Qunyou>> QUNYOU =
            ENTITY_TYPES.register("qunyou", () -> EntityType.Builder
                    .of(Qunyou::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("qunyou"));

    /** 猪分身：复用群友状态机，但使用独立实体类型和 20 点生命属性。 */
    public static final DeferredHolder<EntityType<?>, EntityType<QunyouClone>> QUNYOU_CLONE =
            ENTITY_TYPES.register("qunyou_clone", () -> EntityType.Builder
                    .of(QunyouClone::new, MobCategory.MONSTER)
                    .sized(0.9F, 0.9F)
                    .clientTrackingRange(12)
                    .updateInterval(1)
                    .build("qunyou_clone"));

    /** 群友召唤的胡萝卜弹射物，不保存到区块。 */
    public static final DeferredHolder<EntityType<?>, EntityType<QunyouCarrotProjectile>> QUNYOU_CARROT =
            ENTITY_TYPES.register("qunyou_carrot", () -> EntityType.Builder
                    .<QunyouCarrotProjectile>of(QunyouCarrotProjectile::new, MobCategory.MISC)
                    .sized(0.35F, 0.35F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .noSave()
                    .build("qunyou_carrot"));
    /** 群友“猪导弹”的黄金胡萝卜追踪弹，不保存到区块。 */
    public static final DeferredHolder<EntityType<?>, EntityType<QunyouGoldenCarrotProjectile>> QUNYOU_GOLDEN_CARROT =
            ENTITY_TYPES.register("qunyou_golden_carrot", () -> EntityType.Builder
                    .<QunyouGoldenCarrotProjectile>of(QunyouGoldenCarrotProjectile::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .noSave()
                    .build("qunyou_golden_carrot"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(LIVING_SAND.get(), LivingSand.createAttributes().build());
        event.put(QUNYOU.get(), Qunyou.createAttributes().build());
        event.put(QUNYOU_CLONE.get(), Qunyou.createCloneAttributes().build());
    }

    private ModEntityTypes() {
    }
}
