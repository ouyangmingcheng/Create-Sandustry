package com.minechai.create_sandustry.content.gravity;

import java.util.function.BiConsumer;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.content.moonhop.MoonhopBlock;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Observes vanilla ItemEntity ticks; never replaces movement or scans entities in the world. */
public final class GravityBouncePhysics {
    private static final double EPSILON = 1.0E-6;
    static final double VERTICAL_DRAG = 0.98;
    // Collision filter only. Never used to raise the calculated rebound velocity.
    static final double MINIMUM_IMPACT_SPEED = 0.08;
    // Independent of the micro-collision filter: count only a visible half-block ascent.
    static final double MINIMUM_COUNTED_BOUNCE_HEIGHT = 0.5;

    @FunctionalInterface
    interface SurfaceCoefficient {
        double get(Level level, BlockPos pos, BlockState state);
    }

    private GravityBouncePhysics() {}

    public static void onPreTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof ItemEntity entity) || entity.level().isClientSide) return;
        beforeTick(entity,
                (item, count) -> GravityActivationLogic.activate(item, count, GravityActivationSource.SLIME_BLOCK));
    }

    static void beforeTick(ItemEntity entity, BiConsumer<ItemEntity, Integer> atApex) {
        if (!eligible(entity)) {
            entity.removeData(ModAttachments.GRAVITY_BOUNCE);
            return;
        }
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        if (!tracker.followsSavedMotion(entity) || interrupted(entity)) GravityChainLifecycle.interrupt(entity);
        tracker.beforePosition = entity.position();
        tracker.beforeMotion = entity.getDeltaMovement();
        tracker.beforeGround = entity.onGround();
        // ItemEntity applies its public getGravity() immediately before moving, in dry air.
        tracker.impactVelocity = tracker.beforeMotion.y - entity.getGravity();
        double beforeNormal = tracker.beforeMotion.dot(tracker.flightNormal);
        double nextNormal = tracker.beforeMotion.add(0, -entity.getGravity(), 0).dot(tracker.flightNormal);
        if (!interrupted(entity) && tracker.phase() == GravityBounceTracker.Phase.RISING
                && beforeNormal > 0.0 && nextNormal <= 0.0) {
            // This position is the discrete maximum: gravity makes the next move downward.
            // Observe BEFORE move(), since a short flight can hit slime again in this very tick
            // and SlimeBlock would replace the negative velocity with another positive rebound.
            if (tracker.pendingRise()) {
                // A flight below the height gate ends a previously active chain exactly once.
                GravityChainLifecycle.interrupt(entity);
            } else {
                tracker.passApex();
                atApex.accept(entity, tracker.consecutiveBounces());
                if (!eligible(entity)) {
                    entity.removeData(ModAttachments.GRAVITY_BOUNCE);
                    return;
                }
            }
        }
        tracker.observedTick = entity.tickCount;
        tracker.observedPistonRevision = tracker.pistonRevision;
    }

    public static void onPostTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity entity) || entity.level().isClientSide) return;
        afterTick(entity, GravityBounceSurfaces::getBounceCoefficient, MINIMUM_IMPACT_SPEED);
    }

    /** A collision adapter reports a native launch; it never applies or counts another impulse. */
    public static void acceptPhysicalMoonhopBounce(ItemEntity entity, Vec3 normal) {
        if (!eligible(entity)) return;
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        tracker.physicalContactTick = entity.tickCount;
        tracker.physicalContactNormal = normal;
    }

    // GameTests drive the same vanilla tick with an isolated coefficient/apex callback.
    // No config, global debug switch or production outcome bypass is installed.
    static void afterTick(ItemEntity entity, SurfaceCoefficient surfaces, double minimumImpactSpeed) {
        if (!entity.hasData(ModAttachments.GRAVITY_BOUNCE)) return;
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        if (!eligible(entity)) {
            entity.removeData(ModAttachments.GRAVITY_BOUNCE);
            return;
        }
        if (tracker.observedTick != entity.tickCount) return;
        boolean pistonDuringTick = tracker.pistonRevision != tracker.observedPistonRevision;
        boolean physicalMoonhop = tracker.physicalContactTick == entity.tickCount;
        Vec3 contactNormal = tracker.physicalContactNormal;
        tracker.physicalContactTick = -1;
        tracker.physicalContactNormal = null;
        BlockPos support = entity.getOnPos();
        double coefficient = isTopContact(entity, tracker)
                ? surfaces.get(entity.level(), support, entity.level().getBlockState(support)) : 0.0;
        if (interrupted(entity) || entity.isSuppressingBounce()) {
            GravityChainLifecycle.interrupt(entity);
        } else if (physicalMoonhop) {
            // The Sable bridge already supplied the complete reflected world velocity. A tilted
            // top can be classified as a horizontal collision by Sable; it is not a disconnection.
            if (tracker.phase() == GravityBounceTracker.Phase.RISING) GravityChainLifecycle.interrupt(entity);
            tracker.startPhysicalBounce(entity, contactNormal);
        } else if (entity.horizontalCollision && !pistonDuringTick
                || entity.verticalCollision && !entity.verticalCollisionBelow) {
            GravityChainLifecycle.interrupt(entity);
        } else if (pistonDuringTick) {
            // Native piston motion was observed explicitly; never replace its supplied energy.
            confirmHeight(entity, tracker);
        } else if (Double.isFinite(coefficient) && coefficient > 0.0) {
            // A second landing before the first apex means that flight was interrupted.
            if (tracker.phase() == GravityBounceTracker.Phase.RISING) GravityChainLifecycle.interrupt(entity);
            boolean nativeMoonhop = entity.level().getBlockState(support).getBlock() instanceof MoonhopBlock;
            double rebound = Math.min(-tracker.impactVelocity * coefficient,
                    ModConfigs.GRAVITY_MAXIMUM_BOUNCE_VELOCITY.get());
            // SlimeBlock bounces inside Entity.move, BEFORE ItemEntity's vertical drag.
            // Post observes the end of that tick, so preserve that drag exactly once.
            // MoonhopBlock already bounced inside move(), including its minimum and ItemEntity drag.
            // Observe that flight; never apply a second rebound here.
            double afterDrag = nativeMoonhop ? Math.min(entity.getDeltaMovement().y,
                    Math.max(ModConfigs.GRAVITY_MAXIMUM_BOUNCE_VELOCITY.get(), MoonhopBlock.MINIMUM_BOUNCE_VELOCITY)
                            * VERTICAL_DRAG) : rebound * VERTICAL_DRAG;
            Vec3 motion = entity.getDeltaMovement();
            // Even a very slow first landing must retain MoonhopBlock's native takeoff.
            // The old impact/displacement filters remain specific to synthesized slime rebounds.
            boolean validLanding = nativeMoonhop ? tracker.impactVelocity < 0 && !entity.onGround() && motion.y > 0
                    : !tracker.beforeGround && tracker.impactVelocity <= -minimumImpactSpeed
                            && entity.getY() < tracker.beforePosition.y - EPSILON;
            if (validLanding
                    && afterDrag > entity.getGravity() + EPSILON) {
                // Retain the existing mechanism safety limit, even for a native rebound.
                if (!nativeMoonhop || afterDrag < motion.y) entity.setDeltaMovement(motion.x, afterDrag, motion.z);
                // A real rebound starts airborne; do not let ItemEntity throttle its first ascent.
                entity.setOnGround(false);
                tracker.startBounce(entity.getY());
            } else {
                // Dissipate an unresolvable contact instead of replaying vanilla resting jitter.
                // Only runs on a NEW downward collision, never on intervening grounded ticks.
                entity.setDeltaMovement(motion.x, 0.0, motion.z);
                GravityChainLifecycle.interrupt(entity);
            }
            entity.hasImpulse = true;
        } else if (entity.onGround() || !isUnmodifiedFlight(entity, tracker)) {
            GravityChainLifecycle.interrupt(entity);
        } else {
            confirmHeight(entity, tracker);
        }
        tracker.remember(entity);
    }

    private static void confirmHeight(ItemEntity entity, GravityBounceTracker tracker) {
        if (tracker.phase() == GravityBounceTracker.Phase.RISING && tracker.pendingRise()
                && entity.position().subtract(tracker.beforePosition).dot(tracker.flightNormal) > EPSILON
                && entity.position().dot(tracker.flightNormal) - tracker.bounceOriginY() >= MINIMUM_COUNTED_BOUNCE_HEIGHT) {
            tracker.confirmRise();
            ItemStack stack = entity.getItem().copy();
            GravityMechanismData.incrementTotal(stack);
            entity.setItem(stack);
        }
    }

    private static boolean eligible(ItemEntity entity) {
        return entity.isAlive() && GravityMechanismAssembly.isAwaitingActivation(entity.level(), entity.getItem());
    }

    private static boolean interrupted(ItemEntity entity) {
        return entity.isPassenger() || entity.isNoGravity() || entity.noPhysics
                || entity.isInWaterOrBubble() || entity.isInLava()
                || entity.level().containsAnyLiquid(entity.getBoundingBox());
    }

    private static boolean isTopContact(ItemEntity entity, GravityBounceTracker tracker) {
        // ItemEntity skips move() on most resting ticks: its old collision flags remain set.
        boolean moved = !tracker.beforeGround || tracker.beforeMotion.horizontalDistanceSqr() > 1.0E-5F
                || (entity.tickCount + entity.getId()) % 4 == 0;
        boolean moonhopTakeoff = entity.level().getBlockState(entity.getOnPos()).getBlock() instanceof MoonhopBlock
                && entity.getDeltaMovement().y > 0.0;
        if (!moved || tracker.impactVelocity >= -EPSILON
                || (!entity.onGround() && !moonhopTakeoff) || !entity.verticalCollisionBelow) return false;
        // Match Entity.move's selected support block, then verify its actual top face.
        BlockPos support = entity.getOnPos();
        AABB box = entity.getBoundingBox();
        double top = support.getY() + 1.0;
        return Math.abs(box.minY - top) < EPSILON && tracker.beforePosition.y >= top - EPSILON
                && box.maxX > support.getX() + EPSILON && box.minX < support.getX() + 1.0 - EPSILON
                && box.maxZ > support.getZ() + EPSILON && box.minZ < support.getZ() + 1.0 - EPSILON;
    }

    private static boolean isUnmodifiedFlight(ItemEntity entity, GravityBounceTracker tracker) {
        Vec3 movement = new Vec3(tracker.beforeMotion.x, tracker.impactVelocity, tracker.beforeMotion.z);
        // Entity.move deliberately skips setPos for squared displacement <= 1.0E-7.
        // A tiny natural turn at the apex is still an uninterrupted flight.
        Vec3 expectedPosition = movement.lengthSqr() > 1.0E-7
                ? tracker.beforePosition.add(movement) : tracker.beforePosition;
        Vec3 expectedMotion = movement.multiply((double)0.98F, 0.98, (double)0.98F);
        return expectedPosition.distanceToSqr(entity.position()) < 1.0E-10
                && expectedMotion.distanceToSqr(entity.getDeltaMovement()) < 1.0E-10;
    }
}
