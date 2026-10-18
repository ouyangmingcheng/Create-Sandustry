package com.minechai.create_candustry.effect;

import com.minechai.create_candustry.CreateSandustry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMobEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, CreateSandustry.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> GOLDEN_SELECTION =
            MOB_EFFECTS.register("golden_selection",
                    () -> new GoldenSelectionEffect());

    /** 沙化之兆。 */
    public static final DeferredHolder<MobEffect, MobEffect> SAND_HARDCORE =
            MOB_EFFECTS.register("sand_hardcore",
                    () -> new SandHardcoreEffect());

    /** 沙化：吃下沙团后获得的负面状态，每 1 秒增加 1 颗沙心。 */
    public static final DeferredHolder<MobEffect, MobEffect> SANDIFICATION =
            MOB_EFFECTS.register("sandification",
                    () -> new SandificationEffect());

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    private ModMobEffects() {
    }
}
