package com.minechai.create_sandustry.mixin;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KineticBlockEntity.class)
public interface KineticStressAccessor {
    @Accessor("capacity")
    float createsandustry$getNetworkCapacity();
}
