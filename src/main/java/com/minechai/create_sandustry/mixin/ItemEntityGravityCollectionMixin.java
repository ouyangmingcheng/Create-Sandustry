package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minechai.create_sandustry.content.gravity.GravityChainLifecycle;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.portal.DimensionTransition;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemEntity.class)
public abstract class ItemEntityGravityCollectionMixin {
    @WrapMethod(method = "playerTouch")
    private void createsandustry$preparePickup(Player player, Operation<Void> original) {
        GravityChainLifecycle.collect((Entity) (Object) this, () -> { original.call(player); return null; });
    }

    @WrapMethod(method = "changeDimension")
    private Entity createsandustry$prepareDimensionTransfer(DimensionTransition transition, Operation<Entity> original) {
        return GravityChainLifecycle.changeDimension((ItemEntity) (Object) this, () -> original.call(transition));
    }
}
