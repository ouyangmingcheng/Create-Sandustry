package com.minechai.create_sandustry.content.compat.sable;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.ArrayList;

import com.minechai.create_sandustry.content.gravity.GravityBouncePhysics;
import com.minechai.create_sandustry.content.moonhop.MoonhopBlock;
import com.mojang.logging.LogUtils;

import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision.CollisionInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.slf4j.Logger;

/** Loaded only by the optional Sable mixins. No entity tick/world scan is installed. */
public final class SableMoonhopBounce {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean DEBUG = Boolean.getBoolean("createsandustry.debugMoonhopSable");
    private static final double EPSILON = 1.0E-6;
    private static final ThreadLocal<Move> CURRENT = new ThreadLocal<>();

    private SableMoonhopBounce() {}

    /** Per-entity contact latch, released by separation, not a time based cooldown. */
    public static final class ContactState {
        private WeakReference<SubLevel> surface = new WeakReference<>(null);
        private Vec3 origin;
        private boolean armed = true;
        private Vec3 surfaceVelocity;
        private Vec3 normal;
        private Vec3 point;
        private int bounceCount;

        public Vec3 lastSurfaceVelocity() { return surfaceVelocity; }
        public Vec3 lastNormal() { return normal; }
        public Vec3 lastLocalPoint() { return point; }
        public int bounceCount() { return bounceCount; }
    }

    public interface ContactAccess { ContactState createsandustry$moonhopContact(); }

    public static final class Move {
        private final Move parent;
        private final Entity entity;
        private final ContactState state;
        private final boolean eligible;
        private final AABB initialBounds;
        private Vec3 incoming;
        private Vec3 unprobedMotion;
        private Contact contact;

        private Move(Entity entity, MoverType type, ContactState state) {
            parent = CURRENT.get();
            this.entity = entity;
            this.state = state;
            initialBounds = entity.getBoundingBox();
            eligible = type == MoverType.SELF && !entity.noPhysics && !entity.isPassenger()
                    && !entity.isNoGravity() && !entity.isSpectator()
                    && !(entity instanceof Player p && p.getAbilities().flying);
            SubLevel surface = state.surface.get();
            if (surface == null || surface.isRemoved()) state.armed = true;
            if (!state.armed && state.origin != null
                    && surface.logicalPose().transformPositionInverse(entity.position()).y - state.origin.y > 0.001)
                state.armed = true;
        }
    }

    private record Contact(SubLevel surface, BlockPos block, Vec3 point, Vec3 normal, Vec3 surfaceVelocity,
                           Vec3 incoming, double normalSpeed) {}

    public static Move begin(Entity entity, MoverType type, ContactState state) {
        Move move = new Move(entity, type, state);
        CURRENT.set(move);
        return move;
    }

    public static void incoming(Entity entity, Vec3 movement, Vec3 inherited) {
        Move move = CURRENT.get();
        if (move != null && move.entity == entity) {
            move.incoming = (move.unprobedMotion == null ? movement : move.unprobedMotion).add(inherited);
            move.unprobedMotion = null;
        }
    }

    /** Called inside Sable's SAT solver, AFTER it rejects internal/occluded contact faces. */
    public static void contact(Entity entity, SubLevel surface, Pose3dc pose, BlockPos block,
                               BlockState state, Vector3dc mtv, OrientedBoundingBox3d entityBox) {
        Move move = CURRENT.get();
        if (move == null || move.entity != entity || !move.eligible
                || !(state.getBlock() instanceof MoonhopBlock)) return;

        Vector3d localNormal = pose.transformNormalInverse(new Vector3d(mtv).normalize());
        // A side/bottom SAT contact is never a top landing, even on a rotated structure.
        if (localNormal.y < 0.6 || localNormal.y < Math.abs(localNormal.x)
                || localNormal.y < Math.abs(localNormal.z)) {
            debug(entity, block, "side/bottom contact");
            return;
        }
        Vector3d localCenter = pose.transformPositionInverse(new Vector3d(entityBox.getPosition()));
        if (localCenter.y < block.getY() + 1.0 - EPSILON) return;
        // A sideways entry at an edge can have an upward SAT MTV. Require the body
        // to have approached from outside the top plane, not from below its edge.
        if (Math.max(topGap(move.initialBounds, surface.lastPose(), block),
                topGap(move.initialBounds, surface.logicalPose(), block)) < -1.0E-4) {
            debug(entity, block, "entered from below top plane/side edge");
            return;
        }
        Vec3 normal = moj(pose.transformNormal(new Vector3d(0, 1, 0))).normalize();
        // Support point of the entity OBB towards the plane, rather than the entity centre.
        // This matters for angular velocity: omega x radius differs across the top face.
        Vector3d ownNormal = entityBox.getOrientation().transformInverse(new Vector3d(normal.x, normal.y, normal.z));
        Vector3d dimensions = entityBox.getDimensions();
        Vector3d support = new Vector3d(supportOffset(ownNormal.x, dimensions.x),
                supportOffset(ownNormal.y, dimensions.y), supportOffset(ownNormal.z, dimensions.z));
        entityBox.getOrientation().transform(support).add(entityBox.getPosition());
        Vector3d point = pose.transformPositionInverse(support);
        point.set(Math.clamp(point.x, block.getX(), block.getX() + 1.0), block.getY() + 1.0,
                Math.clamp(point.z, block.getZ(), block.getZ() + 1.0));
        Vec3 velocity = moj(SableCompanion.INSTANCE.getVelocity(entity.level(), surface, point, new Vector3d()))
                .scale(0.05); // Companion returns blocks/second; Entity uses blocks/tick.
        Vec3 incoming = move.incoming;
        if (incoming == null) return;
        double speed = incoming.subtract(velocity).dot(normal);
        if (speed >= -EPSILON) {
            debug(entity, block, "outgoing/resting contact, relative normal velocity=" + speed);
            return;
        }
        Contact contact = new Contact(surface, block.immutable(), moj(point), normal, velocity, incoming, speed);
        // Sable can visit this contact many times in its 10 substeps; choose ONE impulse.
        if (move.contact == null || speed < move.contact.normalSpeed) move.contact = contact;
    }

    public static void end(Move move, boolean completed) {
        try {
            if (completed) finish(move);
        } finally {
            if (move.parent == null) CURRENT.remove(); else CURRENT.set(move.parent);
        }
    }

    private static void finish(Move move) {
        Entity entity = move.entity;
        CollisionInfo info = ((EntityMovementExtension) entity).sable$getCollisionInfo();
        if (info != null && info.firstCollisions != null) {
            // The tag also enables Sable's horizontal-only living effect. Moonhop's own
            // top-contact rule replaces that effect, including when sneaking or touching a side.
            info.firstCollisions.entrySet().removeIf(e -> e.getValue().block().getBlock() instanceof MoonhopBlock);
        }
        if (!move.eligible) return;
        Contact contact = move.contact;
        if (contact == null) {
            move.state.armed = true;
            return;
        }
        ContactState latch = move.state;
        if (!latch.armed && latch.surface.get() == contact.surface) {
            debug(entity, contact.block, "continuous contact (not separated)");
            return;
        }
        if (entity.isSuppressingBounce() || entity.isInWaterOrBubble() || entity.isInLava()
                || entity.level().containsAnyLiquid(entity.getBoundingBox())
                || !hasClearance(entity, contact.normal)) {
            debug(entity, contact.block, "bounce suppressed/liquid/blocked");
            return;
        }
        if (info == null) return;
        // Sable normally applies this displacement later in LivingEntity.travel. Apply it once
        // here before detaching; items have already inherited it in Sable's Entity.move tail.
        if (entity instanceof LivingEntity living) {
            if (info.inheritedMotion != null) entity.setPos(entity.position().add(
                    ((EntityExtension) entity).sable$vanillaCollide(info.inheritedMotion)));
            ((LivingEntityMovementExtension) living).sable$getInheritedVelocity().zero();
        }
        info.inheritedMotion = null;
        info.trackingSubLevel = null;
        ((EntityMovementExtension) entity).sable$setTrackingSubLevel(null);
        double factor = entity instanceof LivingEntity ? 1.0 : 0.8;
        double rebound = Math.max(-contact.normalSpeed * factor, MoonhopBlock.MINIMUM_BOUNCE_VELOCITY);
        Vec3 reflected = contact.incoming.add(contact.normal.scale(rebound - contact.normalSpeed));
        entity.setDeltaMovement(reflected);
        entity.setOnGround(false);
        entity.fallDistance = 0;
        entity.hasImpulse = true;
        if (entity instanceof ServerPlayer) entity.hurtMarked = true; // authoritative vanilla velocity packet
        latch.surface = new WeakReference<>(contact.surface);
        latch.origin = contact.surface.logicalPose().transformPositionInverse(entity.position());
        latch.normal = contact.normal;
        latch.surfaceVelocity = contact.surfaceVelocity;
        latch.point = contact.point;
        latch.bounceCount++;
        latch.armed = false;
        if (entity instanceof ItemEntity item && !entity.level().isClientSide) {
            GravityBouncePhysics.acceptPhysicalMoonhopBounce(item, contact.normal);
        }
        debug(entity, contact.block, "bounce sublevel=" + contact.surface.getUniqueId() + " n=" + contact.normal
                + " relative=" + contact.normalSpeed + " surface/tick=" + contact.surfaceVelocity + " out=" + reflected);
    }

    /** ServerPlayer normally bypasses Sable's SAT solver. Opt in only near a moonhop plot block. */
    public static boolean needsServerCollision(Entity entity, Vec3 movement) {
        if (!(entity instanceof ServerPlayer)) return false;
        AABB swept = entity.getBoundingBox().minmax(entity.getBoundingBox().move(movement)).inflate(0.25);
        for (var surface : SableCompanion.INSTANCE.getAllIntersecting(entity.level(), new BoundingBox3d(swept))) {
            for (BlockPos pos : localBlocks((SubLevel) surface, swept)) {
                if (entity.level().getBlockState(pos).getBlock() instanceof MoonhopBlock) return true;
            }
        }
        return false;
    }

    private static Iterable<BlockPos> localBlocks(SubLevel surface, AABB worldBounds) {
        BoundingBox3d local = new BoundingBox3d(worldBounds);
        local.transformInverse(surface.logicalPose(), new org.joml.Matrix4d(), local);
        var bounds = surface.getPlot().getBoundingBox();
        int x0 = Math.max(bounds.minX(), (int) Math.floor(local.minX));
        int y0 = Math.max(bounds.minY(), (int) Math.floor(local.minY));
        int z0 = Math.max(bounds.minZ(), (int) Math.floor(local.minZ));
        int x1 = Math.min(bounds.maxX(), (int) Math.floor(local.maxX));
        int y1 = Math.min(bounds.maxY(), (int) Math.floor(local.maxY));
        int z1 = Math.min(bounds.maxZ(), (int) Math.floor(local.maxZ));
        return x0 > x1 || y0 > y1 || z0 > z1 ? List.of() : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1);
    }

    private static boolean hasClearance(Entity entity, Vec3 normal) {
        AABB probe = entity.getBoundingBox().move(normal.scale(0.02)).deflate(1.0E-5);
        if (!entity.level().noCollision(entity, probe)) return false;
        var sink = ((LevelExtension) entity.level()).sable$getJOMLSink();
        Vec3 center = probe.getCenter();
        var body = new OrientedBoundingBox3d(new Vector3d(center.x, center.y, center.z),
                new Vector3d(probe.getXsize(), probe.getYsize(), probe.getZsize()), new Quaterniond(), sink);
        var blockBox = new OrientedBoundingBox3d(sink);
        var mtv = new Vector3d();
        for (var access : SableCompanion.INSTANCE.getAllIntersecting(entity.level(), new BoundingBox3d(probe))) {
            SubLevel surface = (SubLevel) access;
            Pose3dc pose = surface.logicalPose();
            for (BlockPos pos : localBlocks(surface, probe)) {
                BlockState state = entity.level().getBlockState(pos);
                List<AABB> boxes = state.getCollisionShape(entity.level(), pos, CollisionContext.of(entity)).toAabbs();
                if (!state.getFluidState().isEmpty()) {
                    boxes = new ArrayList<>(boxes);
                    boxes.add(new AABB(0, 0, 0, 1, state.getFluidState().getHeight(entity.level(), pos), 1));
                }
                for (AABB box : boxes) {
                    Vec3 localCenter = box.getCenter().add(pos.getX(), pos.getY(), pos.getZ());
                    blockBox.setPosition(pose.transformPosition(new Vector3d(localCenter.x, localCenter.y, localCenter.z)));
                    blockBox.setDimensions(new Vector3d(box.getXsize(), box.getYsize(), box.getZsize()).mul(pose.scale()));
                    blockBox.setOrientation(pose.orientation());
                    OrientedBoundingBox3d.sat(body, blockBox, mtv);
                    if (mtv.lengthSquared() > EPSILON * EPSILON && mtv.x != Double.MAX_VALUE
                            && mtv.y != Double.MAX_VALUE && mtv.z != Double.MAX_VALUE) return false;
                }
            }
        }
        return true;
    }

    public static BlockPos landingBlock(Entity entity) {
        Move move = CURRENT.get();
        return move != null && move.entity == entity && move.contact != null ? move.contact.block : null;
    }

    public static Vec3 serverContactProbe(Entity entity, Vec3 motion) {
        // Movement packets contain the already clipped client position. A tiny extension lets
        // server SAT recognise an exactly touching landing instead of trusting a client impulse.
        if (!entity.level().isClientSide && motion.lengthSqr() > EPSILON * EPSILON
                && needsServerCollision(entity, motion)) {
            Move move = CURRENT.get();
            if (move != null && move.entity == entity) move.unprobedMotion = motion;
            return motion.add(motion.normalize().scale(1.0E-5));
        }
        return motion;
    }

    private static Vec3 moj(Vector3dc v) { return new Vec3(v.x(), v.y(), v.z()); }

    private static double supportOffset(double component, double length) {
        return Math.abs(component) < EPSILON ? 0 : -Math.copySign(length / 2, component);
    }

    private static double topGap(AABB bounds, Pose3dc pose, BlockPos block) {
        Vec3 normal = pose.transformNormal(new Vec3(0, 1, 0)).normalize();
        Vec3 plane = pose.transformPosition(new Vec3(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5));
        double radius = (Math.abs(normal.x) * bounds.getXsize() + Math.abs(normal.y) * bounds.getYsize()
                + Math.abs(normal.z) * bounds.getZsize()) / 2;
        return bounds.getCenter().subtract(plane).dot(normal) - radius;
    }

    private static void debug(Entity entity, BlockPos pos, String reason) {
        if (DEBUG) LOGGER.info("[Moonhop/Sable] {} entity={} tick={} block={} {}",
                entity.level().isClientSide ? "client" : "server", entity.getId(), entity.tickCount, pos, reason);
    }
}
