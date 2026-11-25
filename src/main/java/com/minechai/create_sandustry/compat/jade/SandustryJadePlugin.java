package com.minechai.create_sandustry.compat.jade;

import com.minechai.create_sandustry.content.kineticpress.KineticPressBlock;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Discovered only by Jade; the base mod does not load or require Jade classes. */
@WailaPlugin
public final class SandustryJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        // Block-specific views precede the generic Container/capability views,
        // including on dedicated servers where extension priorities can tie.
        registration.registerEnergyStorage(KineticPressJadeProvider.Energy.INSTANCE, KineticPressBlock.class);
        registration.registerItemStorage(KineticPressJadeProvider.Items.INSTANCE, KineticPressBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerEnergyStorageClient(KineticPressJadeProvider.Energy.INSTANCE);
        registration.registerItemStorageClient(KineticPressJadeProvider.Items.INSTANCE);
    }
}
