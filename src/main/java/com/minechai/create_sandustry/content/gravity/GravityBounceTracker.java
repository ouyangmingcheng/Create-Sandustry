package com.minechai.create_sandustry.content.gravity;

import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Server-only flight state. Only a tick's observation is transient; a saved flight resumes safely. */
public final class GravityBounceTracker {
    public enum Phase { FALLING, RISING, AFTER_APEX }

    public static final Codec<GravityBounceTracker> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(0, 2).fieldOf("phase").forGetter(t -> t.phase.ordinal()),
            Codec.intRange(0, 15).fieldOf("consecutive").forGetter(t -> t.consecutiveBounces),
            Vec3.CODEC.fieldOf("position").forGetter(t -> t.expectedPosition),
            Vec3.CODEC.fieldOf("motion").forGetter(t -> t.expectedMotion),
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(t -> t.dimension),
            Codec.BOOL.optionalFieldOf("pending_rise", false).forGetter(t -> t.pendingRise),
            Codec.DOUBLE.optionalFieldOf("bounce_origin_y").forGetter(t -> Optional.of(t.bounceOriginY)),
            UUIDUtil.CODEC.listOf().optionalFieldOf("piston_launches", List.of()).forGetter(t -> t.pistonLaunches),
            Vec3.CODEC.optionalFieldOf("flight_normal", new Vec3(0, 1, 0)).forGetter(t -> t.flightNormal)
    ).apply(instance, GravityBounceTracker::new));

    private Phase phase = Phase.FALLING;
    private int consecutiveBounces;
    private Vec3 expectedPosition = Vec3.ZERO;
    private Vec3 expectedMotion = Vec3.ZERO;
    private ResourceLocation dimension = ResourceLocation.withDefaultNamespace("overworld");
    private boolean initialized;
    private boolean pendingRise;
    private double bounceOriginY;
    private final List<UUID> pistonLaunches = new ArrayList<>();
    // These values must be freshly observed in Pre, never restored from disk.
    Vec3 beforePosition;
    Vec3 beforeMotion;
    double impactVelocity;
    boolean beforeGround;
    int observedTick = -1;
    boolean transferring;
    long pistonRevision;
    long observedPistonRevision;
    Vec3 flightNormal = new Vec3(0, 1, 0);
    Vec3 physicalContactNormal;
    int physicalContactTick = -1;

    public GravityBounceTracker() {}

    private GravityBounceTracker(int phase, int consecutive, Vec3 position, Vec3 motion,
            ResourceLocation dimension, boolean pendingRise, Optional<Double> bounceOriginY, List<UUID> pistonLaunches,
            Vec3 flightNormal) {
        this.phase = Phase.values()[phase];
        this.consecutiveBounces = consecutive;
        this.expectedPosition = position;
        this.expectedMotion = motion;
        this.dimension = dimension;
        this.pendingRise = pendingRise;
        this.bounceOriginY = bounceOriginY.orElse(position.y);
        this.initialized = true;
        this.flightNormal = flightNormal.lengthSqr() > 1.0E-6 ? flightNormal.normalize() : new Vec3(0, 1, 0);
        this.pistonLaunches.addAll(pistonLaunches.subList(Math.max(0, pistonLaunches.size() - 64), pistonLaunches.size()));
        // Old saves did not record a takeoff height: retain stack wear, but restart continuity.
        if (bounceOriginY.isEmpty()) reset();
    }

    public Phase phase() { return phase; }
    public int consecutiveBounces() { return consecutiveBounces; }
    boolean pendingRise() { return pendingRise; }
    double bounceOriginY() { return bounceOriginY; }

    boolean followsSavedMotion(ItemEntity entity) {
        return !initialized || dimension.equals(entity.level().dimension().location())
                && expectedPosition.distanceToSqr(entity.position()) < 1.0E-10
                && expectedMotion.distanceToSqr(entity.getDeltaMovement()) < 1.0E-10;
    }

    void remember(ItemEntity entity) {
        saveExpectedMotion(entity);
        observedTick = -1;
    }

    void acceptPistonMotion(ItemEntity entity) {
        saveExpectedMotion(entity);
        pistonRevision++;
    }

    private void saveExpectedMotion(ItemEntity entity) {
        expectedPosition = entity.position();
        expectedMotion = entity.getDeltaMovement();
        dimension = entity.level().dimension().location();
        initialized = true;
    }

    boolean newPistonLaunch(UUID movement) {
        if (pistonLaunches.contains(movement)) return false;
        if (pistonLaunches.size() == 64) pistonLaunches.removeFirst();
        pistonLaunches.add(movement);
        return true;
    }

    void startBounce(double originY) {
        flightNormal = new Vec3(0, 1, 0);
        phase = Phase.RISING;
        pendingRise = true;
        bounceOriginY = originY;
    }

    void startPhysicalBounce(ItemEntity entity, Vec3 normal) {
        startBounce(entity.position().dot(normal));
        flightNormal = normal;
    }

    void confirmRise() {
        consecutiveBounces = Math.min(15, consecutiveBounces + 1);
        pendingRise = false;
    }

    void passApex() { phase = Phase.AFTER_APEX; }

    public void reset() {
        phase = Phase.FALLING;
        consecutiveBounces = 0;
        pendingRise = false;
        observedTick = -1;
        physicalContactTick = -1;
        physicalContactNormal = null;
        flightNormal = new Vec3(0, 1, 0);
    }
}
