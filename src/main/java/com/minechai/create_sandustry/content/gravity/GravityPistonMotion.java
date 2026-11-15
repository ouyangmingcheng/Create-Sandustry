package com.minechai.create_sandustry.content.gravity;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.phys.Vec3;

/** Records actual vanilla moving-slime calls; never computes or writes a piston velocity. */
public final class GravityPistonMotion {
    private static final String MOVEMENT_ID = "createsandustry.gravity_piston_movement";
    private static final ThreadLocal<Deque<Context>> CONTEXTS = ThreadLocal.withInitial(ArrayDeque::new);

    private GravityPistonMotion() {}

    public static void begin(PistonMovingBlockEntity piston) {
        CONTEXTS.get().push(new Context(piston));
    }

    public static void beforeSlimeVelocity(Entity source) {
        Context context = CONTEXTS.get().peek();
        if (context == null || !eligible(source)) return;
        ItemEntity entity = (ItemEntity) source;
        GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
        if (!tracker.followsSavedMotion(entity)) GravityChainLifecycle.interrupt(entity);
        context.before.putIfAbsent(entity, entity.getDeltaMovement());
    }

    public static void afterPush(Entity source, Direction direction, Vec3 beforePosition) {
        Context context = CONTEXTS.get().peek();
        if (context == null || !eligible(source) || !context.before.containsKey(source)) return;
        ItemEntity entity = (ItemEntity) source;
        if (direction == Direction.UP && entity.getY() > beforePosition.y + 1e-6 && entity.getDeltaMovement().y > 0) {
            var data = context.piston.getPersistentData();
            if (!data.hasUUID(MOVEMENT_ID)) data.putUUID(MOVEMENT_ID, UUID.randomUUID());
            GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
            if (tracker.newPistonLaunch(data.getUUID(MOVEMENT_ID))) tracker.startBounce(entity.getY());
        }
    }

    public static void end() {
        Deque<Context> contexts = CONTEXTS.get();
        Context context = contexts.pop();
        for (Map.Entry<ItemEntity, Vec3> contact : context.before.entrySet()) {
            ItemEntity entity = contact.getKey();
            if (!eligible(entity)) continue;
            GravityBounceTracker tracker = entity.getData(ModAttachments.GRAVITY_BOUNCE);
            if (entity.verticalCollision && !entity.verticalCollisionBelow) GravityChainLifecycle.interrupt(entity);
            // A forced downward turn is not a gravity apex and must not roll an outcome.
            if (contact.getValue().y > 0 && entity.getDeltaMovement().y <= 0
                    && tracker.phase() == GravityBounceTracker.Phase.RISING && !tracker.pendingRise()) tracker.passApex();
            tracker.acceptPistonMotion(entity);
        }
        if (contexts.isEmpty()) CONTEXTS.remove();
    }

    private static boolean eligible(Entity source) {
        return source instanceof ItemEntity entity && !entity.level().isClientSide && entity.isAlive()
                && GravityMechanismAssembly.isAwaitingActivation(entity.level(), entity.getItem());
    }

    private static final class Context {
        final PistonMovingBlockEntity piston;
        final Map<ItemEntity, Vec3> before = new IdentityHashMap<>();

        Context(PistonMovingBlockEntity piston) { this.piston = piston; }
    }
}
