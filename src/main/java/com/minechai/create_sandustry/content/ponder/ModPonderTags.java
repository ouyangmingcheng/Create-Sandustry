package com.minechai.create_sandustry.content.ponder;

import com.minechai.create_sandustry.content.ponder.scenes.ThermalRelayScenes;

import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Ponder tag registrations for Create: Sandustry.
 */
public final class ModPonderTags {
    private ModPonderTags() {
    }

    public static void register(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ThermalRelayScenes.registerTags(helper);
    }
}