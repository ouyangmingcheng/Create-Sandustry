package com.minechai.create_sandustry.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.minechai.create_sandustry.content.gravity.GravityPistonMotion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonGravityMotionMixin {
    @WrapMethod(method = "moveCollidedEntities")
    private static void createsandustry$observeSlimeMovement(Level level, BlockPos pos, float progress,
            PistonMovingBlockEntity piston, Operation<Void> original) {
        GravityPistonMotion.begin(piston);
        try {
            original.call(level, pos, progress, piston);
        } finally {
            GravityPistonMotion.end();
        }
    }

    @WrapOperation(method = "moveCollidedEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(DDD)V"))
    private static void createsandustry$observeNativeVelocity(Entity entity, double x, double y, double z,
            Operation<Void> original) {
        GravityPistonMotion.beforeSlimeVelocity(entity);
        original.call(entity, x, y, z);
    }

    @WrapOperation(method = "moveCollidedEntities", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonMovingBlockEntity;moveEntityByPiston(Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/Entity;DLnet/minecraft/core/Direction;)V"))
    private static void createsandustry$observeActualPush(Direction noClip, Entity entity, double distance,
            Direction direction, Operation<Void> original) {
        Vec3 before = entity.position();
        original.call(noClip, entity, distance, direction);
        GravityPistonMotion.afterPush(entity, direction, before);
    }
}
