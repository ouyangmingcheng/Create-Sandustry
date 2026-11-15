package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minechai.create_sandustry.content.gravity.GravityChainLifecycle;

import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(HopperBlockEntity.class)
public abstract class HopperGravityCollectionMixin {
    @WrapMethod(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z")
    private static boolean createsandustry$prepareHopperPickup(Container container, ItemEntity entity,
            Operation<Boolean> original) {
        return GravityChainLifecycle.collect(entity, () -> original.call(container, entity));
    }
}
