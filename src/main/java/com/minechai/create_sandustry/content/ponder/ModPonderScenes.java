package com.minechai.create_sandustry.content.ponder;

import com.minechai.create_sandustry.content.ponder.scenes.FlorinolBatteryScenes;
import com.minechai.create_sandustry.content.ponder.scenes.FlorinolSoilScenes;
import com.minechai.create_sandustry.content.ponder.scenes.KineticPressScenes;
import com.minechai.create_sandustry.content.ponder.scenes.MoonCrystalScenes;
import com.minechai.create_sandustry.content.ponder.scenes.MoonhopScenes;
import com.minechai.create_sandustry.content.ponder.scenes.ThermalRelayScenes;

import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder scene registrations for Create: Sandustry.
 */
public final class ModPonderScenes {
    private ModPonderScenes() {
    }

    public static void register(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        com.minechai.create_sandustry.content.ponder.scenes.GoldenExperienceScenes.register(helper);
        ThermalRelayScenes.register(helper);
        FlorinolBatteryScenes.register(helper);
        FlorinolSoilScenes.register(helper);
        MoonCrystalScenes.register(helper);
        MoonhopScenes.register(helper);
        KineticPressScenes.register(helper);
    }
}
