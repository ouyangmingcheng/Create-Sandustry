package com.minechai.create_sandustry.content.qunyou;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 群友“猪导弹”召唤的黄金胡萝卜。
 *
 * <p>该实体继承普通胡萝卜的悬浮与物品渲染基础，但拥有独立注册类型、
 * 低速追踪、火焰拖尾和非破坏性小范围爆炸，不改变普通胡萝卜行为。</p>
 */
public class QunyouGoldenCarrotProjectile extends QunyouCarrotProjectile {
    private UUID lockedTarget;
    private Vec3 launchPosition = Vec3.ZERO;
    private boolean exploded;

    public QunyouGoldenCarrotProjectile(EntityType<? extends QunyouGoldenCarrotProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.GOLDEN_CARROT;
    }

    @Override
    public void launch(LivingEntity target) {
        if (this.launched || this.level().isClientSide || target == null) {
            return;
        }
        this.launched = true;
        this.hit = false;
        this.lockedTarget = target.getUUID();
        this.launchPosition = this.position();
        Vec3 direction = target.getEyePosition().subtract(this.position());
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0D, 0.0D, 1.0D);
        }
        this.shoot(direction.x, direction.y, direction.z, (float) goldenSpeed(), 0.0F);
        this.setNoGravity(false);
        this.hurtMarked = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.isRemoved() || this.hit || !this.launched) {
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof Qunyou qunyou) || qunyou.isRemoved() || !qunyou.isAlive()) {
            this.discard();
            return;
        }
        LivingEntity target = this.resolveLockedTarget(qunyou);
        if (target == null
                || this.position().distanceToSqr(this.launchPosition)
                        > ModConfigs.QUNYOU_GOLDEN_CARROT_MAX_TRACK_RANGE.get()
                                * ModConfigs.QUNYOU_GOLDEN_CARROT_MAX_TRACK_RANGE.get()) {
            this.discard();
            return;
        }

        double predictionTicks = ModConfigs.QUNYOU_GOLDEN_CARROT_PREDICTION_TICKS.get();
        Vec3 predicted = target.getEyePosition().add(target.getDeltaMovement().scale(predictionTicks));
        Vec3 desired = predicted.subtract(this.position());
        if (desired.lengthSqr() < 1.0E-8D) {
            return;
        }
        desired = desired.normalize();
        Vec3 current = this.getDeltaMovement();
        double turn = ModConfigs.QUNYOU_GOLDEN_CARROT_TURN_LERP.get();
        Vec3 direction = current.lengthSqr() < 1.0E-8D
                ? desired
                : current.normalize().scale(1.0D - turn).add(desired.scale(turn)).normalize();
        this.setDeltaMovement(direction.scale(goldenSpeed()));
        this.hurtMarked = true;
        this.spawnFlameTrail(direction);
    }

    private LivingEntity resolveLockedTarget(Qunyou owner) {
        if (this.lockedTarget == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.lockedTarget);
        return entity instanceof LivingEntity living && owner.isValidCombatTarget(living)
                && owner.isAllowedAttackTarget(living) ? living : null;
    }

    private double goldenSpeed() {
        return QunyouCarrotProjectile.ARROW_SPEED * ModConfigs.QUNYOU_GOLDEN_CARROT_SPEED_RATIO.get();
    }

    private void spawnFlameTrail(Vec3 direction) {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || this.tickCount % ModConfigs.QUNYOU_GOLDEN_CARROT_TRAIL_INTERVAL.get() != 0) {
            return;
        }
        Vec3 back = direction.normalize().scale(-0.30D);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX() + back.x, this.getY() + 0.15D + back.y, this.getZ() + back.z,
                3, 0.08D, 0.08D, 0.08D, 0.01D);
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                this.getX() + back.x, this.getY() + 0.12D + back.y, this.getZ() + back.z,
                1, 0.06D, 0.06D, 0.06D, 0.005D);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.hit || !this.launched || !(result.getEntity() instanceof LivingEntity living)) {
            return;
        }
        Entity owner = this.getOwner();
        if (!(owner instanceof Qunyou qunyou) || !qunyou.isValidCombatTarget(living)
                || !qunyou.isAllowedAttackTarget(living)) {
            this.discard();
            return;
        }
        this.hit = true;
        DamageSource source = this.damageSources().mobProjectile(this, qunyou);
        float totalDamage = (float) (QunyouCarrotProjectile.DAMAGE
                * ModConfigs.QUNYOU_GOLDEN_CARROT_DAMAGE_MULTIPLIER.get());
        living.hurt(source, totalDamage);
        this.detonate(living.position().add(0.0D, living.getBbHeight() * 0.5D, 0.0D), living, qunyou);
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (!this.launched || this.hit) {
            return;
        }
        Entity owner = this.getOwner();
        if (owner instanceof Qunyou qunyou && qunyou.isAlive() && !qunyou.isRemoved()) {
            this.detonate(result.getLocation(), null, qunyou);
        }
        this.discard();
    }

    private void detonate(Vec3 center, LivingEntity primary, Qunyou owner) {
        if (this.exploded || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.exploded = true;
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLDEN_CARROT)),
                center.x, center.y, center.z, 18, 0.35D, 0.35D, 0.35D, 0.08D);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                center.x, center.y, center.z, 16, 0.30D, 0.20D, 0.30D, 0.04D);
        serverLevel.playSound(null, BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 0.9F, 1.35F);

        if (!ModConfigs.QUNYOU_GOLDEN_CARROT_AFFECT_OTHER_TARGETS.get()) {
            return;
        }
        double radius = ModConfigs.QUNYOU_GOLDEN_CARROT_EXPLOSION_RADIUS.get();
        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity victim : serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                candidate -> candidate != primary && owner.isValidCombatTarget(candidate)
                        && owner.isAllowedAttackTarget(candidate))) {
            double distance = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D)
                    .distanceTo(center);
            if (distance > radius) {
                continue;
            }
            float damage = (float) (ModConfigs.QUNYOU_GOLDEN_CARROT_EXPLOSION_DAMAGE.get()
                    * (1.0D - distance / radius));
            if (damage > 0.0F) {
                DamageSource source = this.damageSources().mobProjectile(this, owner);
                victim.hurt(source, damage);
                Vec3 push = victim.position().subtract(center).multiply(1.0D, 0.0D, 1.0D);
                if (push.lengthSqr() > 1.0E-6D) {
                    push = push.normalize();
                    victim.push(push.x * ModConfigs.QUNYOU_GOLDEN_CARROT_EXPLOSION_KNOCKBACK.get(),
                            0.35D,
                            push.z * ModConfigs.QUNYOU_GOLDEN_CARROT_EXPLOSION_KNOCKBACK.get());
                    victim.hurtMarked = true;
                }
            }
        }
    }
}
