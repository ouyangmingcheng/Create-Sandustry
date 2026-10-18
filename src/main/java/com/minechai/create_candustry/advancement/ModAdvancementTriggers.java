package com.minechai.create_candustry.advancement;

import com.minechai.create_candustry.CreateSandustry;

import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModAdvancementTriggers {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES =
            DeferredRegister.create(Registries.TRIGGER_TYPE, CreateSandustry.MODID);

    public static final DeferredHolder<CriterionTrigger<?>, GoldenFriedRiceTrigger> GOLDEN_FRIED_RICE =
            TRIGGER_TYPES.register("golden_fried_rice", GoldenFriedRiceTrigger::new);

    public static void register(IEventBus eventBus) {
        TRIGGER_TYPES.register(eventBus);
    }

    private ModAdvancementTriggers() {
    }
}