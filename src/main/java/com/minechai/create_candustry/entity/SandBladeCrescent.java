package com.minechai.create_candustry.entity;

import com.minechai.create_candustry.item.SandMasterSwordSandification;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * 锋利附魔剑气命中后生成的一道半月形剑气。
 *
 * <p>状态机由服务端驱动：生成前确认 8 格内存在敌对目标；生成后寻找最近目标并在连续碰撞检测中命中。
 * 目标失效后重新搜索，仍无有效目标时直接移除，不再执行盘旋上升效果。客户端只读取同步状态并渲染，
 * 不参与伤害或目标选择。</p>
 */
public class SandBladeCrescent extends Projectile {
    public enum State {
        SEEKING,
        HOMING
    }

    private static final int MAX_LIFETIME = 240;
    private static final double HOMING_SPEED = 0.85D;
    private static final float SEARCH_RANGE = 8.0F;
    public static final float DAMAGE = SandMasterSwordBeam.DAMAGE;
    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(SandBladeCrescent.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_VARIANT =
            SynchedEntityData.defineId(SandBladeCrescent.class, EntityDataSerializers.INT);
    @Nullable
    private LivingEntity target;
    private boolean hit;

    public SandBladeCrescent(EntityType<? extends SandBladeCrescent> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /** 由服务端原始剑气调用；一次严格生成两道，分别向两侧错开。 */
    public static void spawnPair(ServerLevel level, Vec3 position, @Nullable Entity owner, Vec3 travelDirection) {
        // 生成前先确认 8 格内存在有效敌怪；没有目标时不创建任何半月剑气。
        if (!hasTargetNear(level, position, owner)) {
            return;
        }
        Vec3 forward = travelDirection.lengthSqr() > 1.0E-6D ? travelDirection.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
        Vec3 side = perpendicular(forward);
        Vec3 offset = side.scale(0.35D);
        spawnOne(level, position.add(offset), owner, forward, 0);
        spawnOne(level, position.subtract(offset), owner, forward, 1);
    }

    private static void spawnOne(ServerLevel level, Vec3 position, @Nullable Entity owner, Vec3 direction, int variant) {
        SandBladeCrescent crescent = new SandBladeCrescent(ModEntityTypes.SAND_BLADE_CRESCENT.get(), level);
        crescent.setOwner(owner);
        crescent.setPos(position.x, position.y, position.z);
        crescent.setDeltaMovement(direction.scale(HOMING_SPEED));
        crescent.entityData.set(DATA_VARIANT, variant);
        crescent.setState(State.SEEKING);
        level.addFreshEntity(crescent);
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public State getState() {
        return switch (this.entityData.get(DATA_STATE)) {
            case 1 -> State.HOMING;
            default -> State.SEEKING;
        };
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > MAX_LIFETIME) {
            this.discard();
            return;
        }
        switch (this.getState()) {
            case SEEKING -> this.tickSeeking();
            case HOMING -> this.tickHoming();
        }
    }

    private void tickSeeking() {
        LivingEntity found = this.findTarget();
        if (found != null) {
            this.target = found;
            this.setState(State.HOMING);
            this.tickHoming();
        } else {
            this.discard();
        }
    }
    private void tickHoming() {
        if (!isValidEnemy(this.target, this.getOwner())) {
            this.target = null;
            this.setState(State.SEEKING);
            this.tickSeeking();
            return;
        }

        LivingEntity living = this.target;
        if (living.getBoundingBox().inflate(0.2D).intersects(this.getBoundingBox())) {
            this.hitTarget(living);
            return;
        }
        Vec3 targetPoint = living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        Vec3 movement = targetPoint.subtract(this.position());
        if (movement.lengthSqr() < 0.04D) {
            this.hitTarget(living);
            return;
        }

        Vec3 direction = movement.normalize();
        Vec3 start = this.position();
        // 接近目标时只走剩余距离，避免高速越过目标后重新修正成绕圈轨迹。
        double step = Math.min(HOMING_SPEED, movement.length());
        Vec3 end = start.add(direction.scale(step));
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 moveEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();

        if (living.getBoundingBox().inflate(0.35D).clip(start, moveEnd).isPresent()) {
            this.hitTarget(living);
            return;
        }

        this.setPos(moveEnd.x, moveEnd.y, moveEnd.z);
        this.setDeltaMovement(direction.scale(HOMING_SPEED));
        if (blockHit.getType() != HitResult.Type.MISS) {
            this.discard();
        }
    }

    @Nullable
    private LivingEntity findTarget() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(SEARCH_RANGE), candidate -> isValidEnemy(candidate, this.getOwner()));
        LivingEntity nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            double distance = this.distanceToSqr(candidate);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static boolean hasTargetNear(ServerLevel level, Vec3 position, @Nullable Entity owner) {
        return !level.getEntitiesOfClass(LivingEntity.class, new AABB(position, position).inflate(SEARCH_RANGE),
                candidate -> isValidEnemy(candidate, owner)).isEmpty();
    }

    private static boolean isValidEnemy(@Nullable LivingEntity candidate, @Nullable Entity owner) {
        return candidate != null
                && candidate != owner
                && candidate.isAlive()
                && !candidate.isRemoved()
                && !candidate.isInvulnerable()
                && (candidate instanceof Enemy || candidate.getType().is(Tags.EntityTypes.BOSSES));
    }

    private void hitTarget(LivingEntity living) {
        if (this.hit) {
            return;
        }
        this.hit = true;
        if (living.isAlive() && !living.isRemoved()) {
            DamageSource source = this.damageSources().source(SandMasterSwordBeam.DAMAGE_TYPE, this, this.getOwner());
            living.hurt(source, DAMAGE);
            SandMasterSwordSandification.apply(living, this.getOwner());
        }
        this.discard();
    }

    private static Vec3 perpendicular(Vec3 direction) {
        Vec3 axis = Math.abs(direction.y) > 0.92D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
        Vec3 result = new Vec3(
                direction.y * axis.z - direction.z * axis.y,
                direction.z * axis.x - direction.x * axis.z,
                direction.x * axis.y - direction.y * axis.x);
        return result.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : result.normalize();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STATE, (byte) 0);
        builder.define(DATA_VARIANT, 0);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag compound) {
    }
}
