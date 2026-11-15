package com.minechai.create_sandustry.content.moonhop;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** SlimeBlock's 1.21.1 landing behaviour, with a minimum rebound and no piston adhesion. */
public class MoonhopBlock extends HalfTransparentBlock {
    public static final MapCodec<MoonhopBlock> CODEC = simpleCodec(MoonhopBlock::new);
    public static final double MINIMUM_BOUNCE_VELOCITY = 0.42D;

    public MoonhopBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<MoonhopBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return false;
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        // PistonStructureResolver checks BOTH directions, including when the other block is sticky.
        return false;
    }

    @Override
    public boolean isSlimeBlock(BlockState state) {
        // Also prevents slime's special moving-piston launch behaviour.
        return false;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (entity.isSuppressingBounce()) {
            super.fallOn(level, state, pos, entity, fallDistance);
        } else {
            entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
        }
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        Vec3 incoming = entity.getDeltaMovement();
        if (incoming.y >= 0.0 && entity.verticalCollisionBelow) {
            // The same landing cannot reflect an already positive velocity a second time.
            return;
        }
        if (!isTopLanding(level, entity) || entity.noPhysics || entity.isInWaterOrBubble()
                || entity.isInLava() || entity.level().containsAnyLiquid(entity.getBoundingBox())
                || !entity.level().noCollision(entity, entity.getBoundingBox().move(0, 1.0E-5, 0))) {
            // No upward room means no launch: avoid alternating floor/ceiling impulses in a tight gap.
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        boolean vanillaOnly = entity.isNoGravity() || entity.isPassenger();
        if (vanillaOnly && entity instanceof ItemEntity) {
            super.updateEntityAfterFallOn(level, entity);
            return;
        }
        if (incoming.y < 0.0) {
            // Entity.move calls this before clearing the incoming velocity and before item drag.
            double bounceFactor = entity instanceof LivingEntity ? 1.0 : 0.8;
            double bounceVelocity = Math.abs(incoming.y) * bounceFactor;
            if (!vanillaOnly) bounceVelocity = Math.max(bounceVelocity, MINIMUM_BOUNCE_VELOCITY);
            entity.setDeltaMovement(incoming.x, bounceVelocity, incoming.z);
            if (entity instanceof ItemEntity) {
                // ItemEntity otherwise keeps onGround=true and throttles move() to every fourth tick.
                entity.setOnGround(false);
                entity.hasImpulse = true;
            }
        }
    }

    private boolean isTopLanding(BlockGetter level, Entity entity) {
        if (!entity.onGround() || !entity.verticalCollisionBelow) return false;
        BlockPos support = entity.getOnPos();
        return level.getBlockState(support).getBlock() == this
                && Math.abs(entity.getBoundingBox().minY - support.getY() - 1.0) < 1.0E-6;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        double speed = Math.abs(entity.getDeltaMovement().y);
        if (speed < 0.1 && !entity.isSteppingCarefully()) {
            double drag = 0.4 + speed * 0.2;
            entity.setDeltaMovement(entity.getDeltaMovement().multiply(drag, 1.0, drag));
        }
        super.stepOn(level, pos, state, entity);
    }
}
