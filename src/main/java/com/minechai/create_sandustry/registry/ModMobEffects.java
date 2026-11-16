package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.content.sandification.SandificationEffect;
import com.minechai.create_sandustry.content.sandification.SandHardcoreEffect;
import com.minechai.create_sandustry.content.moonhop.MoonWorshipEffect;
import com.minechai.create_sandustry.content.goldenselection.GoldenSelectionEffect;
import com.minechai.create_sandustry.CreateSandustry;
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

    /** 月亮崇拜：持续 3 分钟的跳跃倒计时与死亡惩罚。 */
    public static final DeferredHolder<MobEffect, MobEffect> MOON_WORSHIP =
            MOB_EFFECTS.register("moon_worship",
                    () -> new MoonWorshipEffect());

    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }

    private ModMobEffects() {
    }
}
