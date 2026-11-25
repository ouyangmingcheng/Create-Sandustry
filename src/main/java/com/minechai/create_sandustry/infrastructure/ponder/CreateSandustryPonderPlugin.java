package com.minechai.create_sandustry.infrastructure.ponder;

import com.minechai.create_sandustry.ponder.ModPonderScenes;
import com.minechai.create_sandustry.ponder.ModPonderTags;

import com.minechai.create_sandustry.CreateSandustry;
import com.simibubi.create.foundation.ponder.CreatePonderPlugin;

import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.api.registration.IndexExclusionHelper;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.api.registration.SharedTextRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers Create: Sandustry content with the standalone Ponder index.
 *
 * <p>The plugin is client-only. Scene and tag definitions live in
 * {@link ModPonderScenes} and {@link ModPonderTags} so they can grow without
 * changing the registration entry point.</p>
 */
public final class CreateSandustryPonderPlugin extends CreatePonderPlugin {
    private CreateSandustryPonderPlugin() {
    }

    public static void register() {
        PonderIndex.addPlugin(new CreateSandustryPonderPlugin());
    }

    @Override
    public String getModId() {
        return CreateSandustry.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        ModPonderScenes.register(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        ModPonderTags.register(helper);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        // Shared text will be registered here when Ponder scenes are added.
    }

    @Override
    public void onPonderLevelRestore(PonderLevel ponderLevel) {
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
    }
}