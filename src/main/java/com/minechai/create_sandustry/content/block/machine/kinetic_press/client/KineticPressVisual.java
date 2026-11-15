package com.minechai.create_sandustry.content.block.machine.kinetic_press.client;

import com.minechai.create_sandustry.content.block.machine.kinetic_press.KineticPressBlockEntity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Stateless render-state resolution. No historical stack or position is retained. */
@OnlyIn(Dist.CLIENT)
public final class KineticPressVisual {
    private KineticPressVisual() {
    }

    public static ItemStack getVisibleStack(KineticPressBlockEntity press) {
        if (!press.hasActiveAnimation()) {
            return ItemStack.EMPTY;
        }
        var animations = press.getAnimationSnapshots();
        return animations.isEmpty() ? ItemStack.EMPTY : animations.getFirst().stack();
    }
}