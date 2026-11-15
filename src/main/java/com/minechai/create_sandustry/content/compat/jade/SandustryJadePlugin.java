package com.minechai.create_sandustry.content.compat.jade;

import com.minechai.create_sandustry.content.block.machine.kinetic_press.KineticPressBlock;
import com.minechai.create_sandustry.content.block.machine.kinetic_press.KineticPressBlockEntity;

import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/** Discovered only by Jade; the base mod does not load or require Jade classes. */
@WailaPlugin
public final class SandustryJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(KineticPressJadeProvider.INSTANCE, KineticPressBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(KineticPressJadeProvider.INSTANCE, KineticPressBlock.class);
    }
}
