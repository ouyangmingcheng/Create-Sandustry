package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minechai.create_sandustry.content.gravity.GravityChainLifecycle;
import com.simibubi.create.content.logistics.chute.AbstractChuteBlock;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractChuteBlock.class)
public abstract class CreateGravityChuteCollectionMixin {
    @WrapMethod(method = "updateEntityAfterFallOn")
    private void createsandustry$prepareChuteInsertion(BlockGetter level, Entity entity, Operation<Void> original) {
        GravityChainLifecycle.collect(entity, () -> { original.call(level, entity); return null; });
    }
}
