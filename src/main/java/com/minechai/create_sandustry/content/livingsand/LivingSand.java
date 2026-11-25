package com.minechai.create_sandustry.content.livingsand;

import java.util.EnumSet;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.CommonHooks;

public class LivingSand extends Monster {
    private static float headAttachChance() {
        return ModConfigs.LIVING_SAND_HEAD_ATTACH_CHANCE.get().floatValue();
    }

    private static float activationDistance() {
        return ModConfigs.LIVING_SAND_ACTIVATION_DISTANCE.get().floatValue();
    }

    private static float deactivationDistance() {
        return ModConfigs.LIVING_SAND_DEACTIVATION_DISTANCE.get().floatValue();
    }

    private static int dormantCheckInterval() {
        return ModConfigs.LIVING_SAND_DORMANT_CHECK_INTERVAL.get();
    }

    private static double activeMovementSpeed() {
        return ModConfigs.LIVING_SAND_ACTIVE_MOVEMENT_SPEED.get();
    }

    private boolean wasOnGround;
    private boolean dormant = true;
    private int dormantCheckCooldown;

    public LivingSand(EntityType<? extends LivingSand> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new LivingSandMoveControl(this);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (ModConfigs.SPEC.isLoaded()) {
            this.getAttribute(Attributes.ATTACK_DAMAGE)
                    .setBaseValue(ModConfigs.LIVING_SAND_ATTACK_DAMAGE.get());
            this.getAttribute(Attributes.JUMP_STRENGTH)
                    .setBaseValue(ModConfigs.LIVING_SAND_JUMP_STRENGTH.get());
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D)
                .add(Attributes.JUMP_STRENGTH, 0.55D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LivingSandFloatGoal(this));
        this.goalSelector.addGoal(2, new LivingSandAttackGoal(this));
        this.goalSelector.addGoal(3, new LivingSandRandomDirectionGoal(this));
        this.goalSelector.addGoal(5, new LivingSandKeepOnJumpingGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                candidate -> !this.isDormant() && Math.abs(candidate.getY() - this.getY()) <= 4.0D));
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            this.updateDormantState();
        }

        super.tick();

        if (!this.level().isClientSide) {
            if (!this.isDormant() && this.onGround() && !this.wasOnGround) {
                this.playSound(SoundEvents.SAND_PLACE, this.getSoundVolume(), this.getSoundPitch());
                this.spawnLandingDust();
            }
            this.wasOnGround = this.onGround();
        }
    }

    private boolean isDormant() {
        return this.dormant;
    }

    /**
     * 复活时直接唤醒活沙，使其立即进入现有 AI 系统，不再等待激活距离判定。
     */
    public void awaken(LivingEntity target) {
        this.dormant = false;
        this.dormantCheckCooldown = 0;
        if (target != null) {
            this.setTarget(target);
        }
    }

    private void updateDormantState() {
        if (this.dormant) {
            if (--this.dormantCheckCooldown > 0) {
                return;
            }
            this.dormantCheckCooldown = dormantCheckInterval();

            Player player = this.level().getNearestPlayer(this, activationDistance());
            if (player != null && player.isAlive() && !player.isSpectator()) {
                this.dormant = false;
                this.setTarget(player);
            }
        } else {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive() || this.distanceToSqr(target) > (double) (deactivationDistance() * deactivationDistance())) {
                this.dormant = true;
                this.setTarget(null);
                this.getNavigation().stop();
                this.setSpeed(0.0F);
                this.setXxa(0.0F);
                this.setZza(0.0F);
                this.dormantCheckCooldown = 0;
            }
        }
    }

    private void spawnLandingDust() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int count = 8 + this.random.nextInt(8);
        BlockParticleOption particle = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
        serverLevel.sendParticles(
                particle,
                this.getX(),
                this.getY() + 0.05D,
                this.getZ(),
                count,
                0.25D,
                0.15D,
                0.25D,
                0.08D);
    }

    private int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    @Override
    protected float getSoundVolume() {
        return 0.8F;
    }

    private float getSoundPitch() {
        return 0.8F + this.random.nextFloat() * 0.4F;
    }

    private boolean doPlayJumpSound() {
        return true;
    }

    private SoundEvent getJumpSound() {
        return SoundEvents.SAND_PLACE;
    }

    @Override
    public void playerTouch(Player player) {
        if (!this.isDormant() && this.isEffectiveAi()) {
            this.dealDamage(player);
        }
    }

    protected void dealDamage(LivingEntity livingEntity) {
        if (this.isDormant() || !this.isAlive() || !this.isWithinMeleeAttackRange(livingEntity)
                || !this.hasLineOfSight(livingEntity)) {
            return;
        }

        DamageSource damageSource = this.damageSources().mobAttack(this);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (livingEntity.hurt(damageSource, damage)) {
            this.playSound(SoundEvents.SAND_BREAK, this.getSoundVolume(), this.getSoundPitch());
            if (livingEntity instanceof Player player) {
                this.tryAttachToPlayer(player);
            }
        }
    }

    private void tryAttachToPlayer(Player player) {
        ItemStack headStack = player.getItemBySlot(EquipmentSlot.HEAD);
        if (headStack.is(ModItems.LIVING_SAND.get())) {
            this.discard();
            return;
        }

        if (this.random.nextFloat() >= headAttachChance()) {
            return;
        }

        if (!headStack.isEmpty()) {
            ItemStack droppedHelmet = headStack.copy();
            ItemEntity dropped = player.drop(droppedHelmet, false, true);
            if (dropped == null) {
                return;
            }
            player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }

        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.LIVING_SAND.get()));
        this.discard();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.SAND_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SAND_BREAK;
    }

    @Override
    public void die(DamageSource damageSource) {
        if (CommonHooks.onLivingDeath(this, damageSource)) {
            return;
        }
        if (this.isRemoved() || this.dead) {
            return;
        }

        Entity killer = damageSource.getEntity();
        LivingEntity killCredit = this.getKillCredit();
        if (this.deathScore >= 0 && killCredit != null) {
            killCredit.awardKillScore(this, this.deathScore, damageSource);
        }
        if (this.isSleeping()) {
            this.stopSleeping();
        }

        this.dead = true;
        this.getCombatTracker().recheckStatus();

        if (this.level() instanceof ServerLevel serverLevel) {
            if (killer == null || killer.killedEntity(serverLevel, this)) {
                this.gameEvent(GameEvent.ENTITY_DIE);
                this.dropAllDeathLoot(serverLevel, damageSource);
                this.createWitherRose(killCredit);
            }

            serverLevel.levelEvent(
                    LevelEvent.PARTICLES_DESTROY_BLOCK,
                    this.blockPosition(),
                    Block.getId(Blocks.SAND.defaultBlockState()));
        }

        this.remove(Entity.RemovalReason.KILLED);
    }

    private static final class LivingSandAttackGoal extends Goal {
        private final LivingSand sand;
        private int growTiredTimer;

        private LivingSandAttackGoal(LivingSand sand) {
            this.sand = sand;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.sand.getTarget();
            return !this.sand.isDormant() && target != null && this.sand.canAttack(target)
                    && this.sand.getMoveControl() instanceof LivingSandMoveControl;
        }

        @Override
        public void start() {
            this.growTiredTimer = reducedTickDelay(300);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity target = this.sand.getTarget();
            return !this.sand.isDormant() && target != null && this.sand.canAttack(target)
                    && --this.growTiredTimer > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = this.sand.getTarget();
            if (target != null) {
                this.sand.lookAt(target, 10.0F, 10.0F);
            }
            if (this.sand.getMoveControl() instanceof LivingSandMoveControl moveControl) {
                moveControl.setDirection(this.sand.getYRot(), true);
            }
        }
    }

    private static final class LivingSandFloatGoal extends Goal {
        private final LivingSand sand;

        private LivingSandFloatGoal(LivingSand sand) {
            this.sand = sand;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
            sand.getNavigation().setCanFloat(true);
        }

        @Override
        public boolean canUse() {
            return !this.sand.isDormant()
                    && (this.sand.isInWater() || this.sand.isInLava())
                    && this.sand.getMoveControl() instanceof LivingSandMoveControl;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (this.sand.getRandom().nextFloat() < 0.8F) {
                this.sand.getJumpControl().jump();
            }
            if (this.sand.getMoveControl() instanceof LivingSandMoveControl moveControl) {
                moveControl.setWantedMovement(activeMovementSpeed());
            }
        }
    }

    private static final class LivingSandKeepOnJumpingGoal extends Goal {
        private final LivingSand sand;

        private LivingSandKeepOnJumpingGoal(LivingSand sand) {
            this.sand = sand;
            this.setFlags(EnumSet.of(Goal.Flag.JUMP, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !this.sand.isDormant() && !this.sand.isPassenger();
        }

        @Override
        public void tick() {
            if (this.sand.getMoveControl() instanceof LivingSandMoveControl moveControl) {
                double speed = this.sand.getTarget() != null ? activeMovementSpeed() : 1.0D;
                moveControl.setWantedMovement(speed);
            }
        }
    }

    private static final class LivingSandMoveControl extends MoveControl {
        private float yRot;
        private int jumpDelay;
        private final LivingSand sand;
        private boolean aggressive;

        private LivingSandMoveControl(LivingSand sand) {
            super(sand);
            this.sand = sand;
            this.yRot = 180.0F * sand.getYRot() / (float) Math.PI;
        }

        private void setDirection(float yRot, boolean aggressive) {
            this.yRot = yRot;
            this.aggressive = aggressive;
        }

        private void setWantedMovement(double speed) {
            this.speedModifier = speed;
            this.operation = MoveControl.Operation.MOVE_TO;
        }

        @Override
        public void tick() {
            if (this.sand.isDormant()) {
                this.mob.setSpeed(0.0F);
                this.mob.setXxa(0.0F);
                this.mob.setZza(0.0F);
                this.operation = MoveControl.Operation.WAIT;
                return;
            }

            this.mob.setYRot(this.rotlerp(this.mob.getYRot(), this.yRot, 90.0F));
            this.mob.yHeadRot = this.mob.getYRot();
            this.mob.yBodyRot = this.mob.getYRot();
            if (this.operation != MoveControl.Operation.MOVE_TO) {
                this.mob.setZza(0.0F);
                return;
            }

            this.operation = MoveControl.Operation.WAIT;
            if (this.mob.onGround()) {
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (this.jumpDelay-- <= 0) {
                    this.jumpDelay = this.sand.getJumpDelay();
                    if (this.aggressive) {
                        this.jumpDelay /= 3;
                    }
                    this.sand.getJumpControl().jump();
                    if (this.sand.doPlayJumpSound()) {
                        this.sand.playSound(this.sand.getJumpSound(), this.sand.getSoundVolume(), this.sand.getSoundPitch());
                    }
                } else {
                    this.sand.xxa = 0.0F;
                    this.sand.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            } else {
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            }
        }
    }

    private static final class LivingSandRandomDirectionGoal extends Goal {
        private final LivingSand sand;
        private float chosenDegrees;
        private int nextRandomizeTime;

        private LivingSandRandomDirectionGoal(LivingSand sand) {
            this.sand = sand;
            this.setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !this.sand.isDormant()
                    && this.sand.getTarget() == null
                    && (this.sand.onGround() || this.sand.isInWater() || this.sand.isInLava()
                            || this.sand.hasEffect(MobEffects.LEVITATION))
                    && this.sand.getMoveControl() instanceof LivingSandMoveControl;
        }

        @Override
        public void tick() {
            if (--this.nextRandomizeTime <= 0) {
                this.nextRandomizeTime = this.adjustedTickDelay(40 + this.sand.getRandom().nextInt(60));
                this.chosenDegrees = (float) this.sand.getRandom().nextInt(360);
            }
            if (this.sand.getMoveControl() instanceof LivingSandMoveControl moveControl) {
                moveControl.setDirection(this.chosenDegrees, false);
            }
        }
    }
}




