package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minechai.create_sandustry.content.gravity.GravityChainLifecycle;
import com.simibubi.create.content.logistics.depot.SharedDepotBlockMethods;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SharedDepotBlockMethods.class)
public abstract class CreateGravityDepotCollectionMixin {
    @WrapMethod(method = "onLanded", remap = false)
    private static void createsandustry$prepareDepotInsertion(BlockGetter level, Entity entity, Operation<Void> original) {
        GravityChainLifecycle.collect(entity, () -> { original.call(level, entity); return null; });
    }
}
