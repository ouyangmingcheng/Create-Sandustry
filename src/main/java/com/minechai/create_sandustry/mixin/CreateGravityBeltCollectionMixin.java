package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.minechai.create_sandustry.content.gravity.GravityChainLifecycle;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({BeltBlock.class, FunnelBlock.class})
public abstract class CreateGravityBeltCollectionMixin {
    @WrapMethod(method = "entityInside")
    private void createsandustry$prepareWorldInsertion(BlockState state, Level level, BlockPos pos,
            Entity entity, Operation<Void> original) {
        GravityChainLifecycle.collect(entity, () -> { original.call(state, level, pos, entity); return null; });
    }
}
