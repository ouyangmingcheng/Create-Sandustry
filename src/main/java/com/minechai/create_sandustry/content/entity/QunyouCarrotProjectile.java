package com.minechai.create_sandustry.content.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 群友召唤的胡萝卜弹射物。
 *
 * <p>悬浮阶段不参与伤害判定；发射后使用与原版箭相近的初速度、空气阻力和重力，
 * 只按发射瞬间计算一次弹道，不做逐 tick 追踪。每个弹射物最多对有效敌对生物造成一次伤害。</p>
 */
public class QunyouCarrotProjectile extends ThrowableItemProjectile {
    public static final float DAMAGE = 6.0F;

    /** 原版满蓄力弓的箭初速度：BowItem 中 power(1.0) * 3.0。 */
    protected static final double ARROW_SPEED = 3.0D;
    /** 与原版 AbstractArrow 默认重力一致。 */
    private static final double ARROW_GRAVITY = 0.05D;
    /** 原版弹射物每 tick 保留 0.99 速度，用于估算目标点下坠补偿。 */
    private static final double ARROW_DRAG = 0.99D;

    private static final int MAX_LIFETIME = 200;

    protected boolean launched;
    protected boolean hit;
    private boolean hasHoverPosition;
    private Vec3 hoverPosition = Vec3.ZERO;

    public QunyouCarrotProjectile(EntityType<? extends QunyouCarrotProjectile> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.CARROT;
    }

    /** 服务端设置悬浮位置；客户端通过普通实体位置与旋转同步看到。 */
    public void setHoverPosition(Vec3 position) {
        Vec3 travel = position.subtract(this.position());
        this.hoverPosition = position;
        this.hasHoverPosition = true;
        if (!this.launched) {
            if (travel.horizontalDistanceSqr() > 1.0E-8D) {
                float yaw = (float) (Mth.atan2(-travel.x, travel.z) * (180.0D / Math.PI));
                this.setYRot(yaw);
                this.yRotO = yaw;
                this.setXRot(0.0F);
                this.xRotO = 0.0F;
            }
            this.setPos(position.x, position.y, position.z);
            this.setDeltaMovement(Vec3.ZERO);
            this.setNoGravity(true);
        }
    }

    public boolean isLaunched() {
        return this.launched;
    }

    /**
     * 由群友在召唤阶段调用。瞄准点取目标上半身，并按预计飞行时间补偿重力下坠；
     * 方向只在此时计算一次，飞行过程中不再修正。
     */
    public void launch(LivingEntity target) {
        if (this.launched || this.level().isClientSide || target == null) {
            return;
        }

        this.launched = true;
        this.hit = false;

        Vec3 from = this.position();
        Vec3 targetPoint = target.getEyePosition().add(0.0D, -0.25D, 0.0D);
        double horizontalDistance = targetPoint.subtract(from).horizontalDistance();
        double estimatedTicks = horizontalDistance / (ARROW_SPEED * ARROW_DRAG);
        double dropCompensation = 0.5D * ARROW_GRAVITY * estimatedTicks * estimatedTicks;
        Vec3 aimPoint = targetPoint.add(0.0D, dropCompensation, 0.0D);
        Vec3 direction = aimPoint.subtract(from);

        if (direction.lengthSqr() < 1.0E-8D) {
            direction = this.getLookAngle();
        }
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }

        // Projectile#shoot 同步设置速度与实体 yaw/pitch；之后只由原版弹射物物理更新。
        this.shoot(direction.x, direction.y, direction.z, (float) ARROW_SPEED, 0.0F);
        this.setNoGravity(false);
        this.hurtMarked = true;
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
        if (this.launched) {
            return;
        }

        Entity owner = this.getOwner();
        if (!(owner instanceof Qunyou qunyou) || qunyou.isRemoved() || !qunyou.isAlive()) {
            this.discard();
            return;
        }
        if (this.hasHoverPosition) {
            this.setPos(this.hoverPosition.x, this.hoverPosition.y, this.hoverPosition.z);
        }
    }

    @Override
    protected double getDefaultGravity() {
        return ARROW_GRAVITY;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!this.launched || target == this.getOwner() || !super.canHitEntity(target)) {
            return false;
        }
        Entity owner = this.getOwner();
        return owner instanceof Qunyou qunyou
                && target instanceof LivingEntity living
                && qunyou.isValidCombatTarget(living)
                && qunyou.isAllowedAttackTarget(living);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.hit || !this.launched || !(result.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof Qunyou qunyou) || !qunyou.isValidCombatTarget(living)
                || !qunyou.isAllowedAttackTarget(living)) {
            return;
        }
        this.hit = true;
        DamageSource source = this.damageSources().mobProjectile(this, qunyou);
        living.hurt(source, DAMAGE);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (this.launched) {
            this.discard();
        }
    }
}