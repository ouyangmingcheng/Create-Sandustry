package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.minechai.create_sandustry.content.gravity.GravityChainLifecycle;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChuteBlockEntity.class)
public abstract class CreateGravityChuteSuctionMixin {
    @WrapOperation(method = "findEntities", remap = false, at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/chute/ChuteBlockEntity;setItem(Lnet/minecraft/world/item/ItemStack;F)V"))
    private void createsandustry$prepareAcceptedSuction(ChuteBlockEntity chute, ItemStack stack, float offset,
            Operation<Void> original, @Local ItemEntity entity) {
        original.call(chute, GravityChainLifecycle.collectedCopy(entity, stack), offset);
    }
}
