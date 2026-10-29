package com.minechai.create_sandustry.content.entity;

import com.minechai.create_sandustry.registry.ModEntityTypes;
import com.minechai.create_sandustry.config.ModConfigs;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCures;

/**
 * 沙金工艺独立 Boss「群友」。
 *
 * <p>模型与纹理使用原版猪；所有行为由本类自己的服务端状态机驱动。状态互斥，客户端只读取
 * 同步状态和状态起始游戏刻来播放动画，不参与命中、概率、计时或移动决策。</p>
 */
public class Qunyou extends Monster {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PIG_CLONE_ACTIVE_LIMIT = 4;
    public static final byte IDLE = 0;
    public static final byte MELEE = 1;
    public static final byte CHARGE_PREPARE = 2;
    public static final byte CHARGE = 3;
    public static final byte DODGE = 4;
    public static final byte RETREAT = 5;
    public static final byte INVERTED = 6;
    public static final byte SLEEPING = 7;
    public static final byte FLIP = 8;
    public static final byte SUMMON = 9;
    public static final byte DEFENDING = 10;
    public static final byte JUMPING = 11;
    public static final byte PHASE3_PENDING = 12;
    public static final byte ASCENDING = 13;
    public static final byte GLIDING = 14;
    public static final byte TAKEOFF = 15;
    public static final byte DESCENDING_ATTACK = 16;
    public static final byte PHASE3_GROUND_WAIT = 17;
    /** 第三阶段落地三连冲撞：两击之间的重新瞄准阶段。 */
    public static final byte PHASE3_COMBO_AIM = 18;
    /** 第三阶段落地三连冲撞：单次横向烟花冲撞。 */
    public static final byte PHASE3_COMBO_CHARGE = 19;
    /** 猪弹射：蓄力。 */
    public static final byte PHASE3_EJECTION_CHARGE = 50;
    /** 猪弹射：多段烟花水平冲撞。 */
    public static final byte PHASE3_EJECTION_RUSH = 21;
    /** 猪弹射：命中玩家后的短暂追击恢复。 */
    public static final byte PHASE3_EJECTION_RECOVER = 22;
    /** 猪弹射：未命中后的斜向升空。 */
    public static final byte PHASE3_EJECTION_ASCEND = 23;
    /** 猪撤退：蓄力。 */
    public static final byte PHASE3_PIG_RETREAT_CHARGE = 24;
    /** 猪撤退：水平烟花逃离。 */
    public static final byte PHASE3_PIG_RETREAT_HORIZONTAL = 25;
    /** 猪撤退：斜向升空。 */
    public static final byte PHASE3_PIG_RETREAT_ASCEND = 26;
    /** 猪机动：空中滚转与爬升。 */
    public static final byte PHASE3_AIR_MANEUVER = 27;

    public static final double MAX_HEALTH = 250.0D;
    /** 严格低于该生命值才进入二阶段。 */
    public static final double SECOND_PHASE_HEALTH_THRESHOLD = 150.0D;
    public static final double THIRD_PHASE_HEALTH_THRESHOLD = 50.0D;
    public static final double BASE_ATTACK_DAMAGE = 3.0D;
    /** 原版猪的移动速度属性基准。 */
    public static final double PIG_MOVEMENT_SPEED = 0.25D;
    /** 冲撞是猪正常速度属性的两倍，而不是每 tick 叠加。 */
    public static final double CHARGE_SPEED = PIG_MOVEMENT_SPEED * 2.0D;

    public static final int MELEE_DURATION = 16;
    public static final int MELEE_IMPACT_TICK = 8;
    public static final int CHARGE_PREPARE_DURATION = 20;
    public static final int CHARGE_DURATION = 30;
    public static final int DODGE_DURATION = 12;
    public static final int RETREAT_DURATION = 80;
    public static final int INVERTED_DURATION = 30;
    public static final int SLEEP_DURATION = 200;
    public static final int FLIP_DURATION = 20;
    public static final int SUMMON_HOVER_TICKS = 20;
    public static final int SUMMON_LAUNCH_INTERVAL = 10;
    public static final int SUMMON_END_DELAY = 10;
    public static final int DEFENSE_DURATION = 60;
    public static final int PHASE_TRANSITION_DURATION = 20;
    public static final int SHIELD_BREAK_FEEDBACK_DURATION = 12;
    public static final int EAT_DURATION = 32;
    public static final int JUMP_DURATION = 20;
    public static final double JUMP_TRIGGER_HEIGHT = 3.0D;
    public static final double JUMP_GRAVITY = 0.08D;
    public static final double JUMP_HEIGHT_MARGIN = 1.0D;
    public static final double JUMP_HORIZONTAL_SPEED = 0.25D;
    public static final int EAT_REGENERATION_DURATION = 200;
    public static final int EAT_STRENGTH_DURATION = 200;
    /** Minecraft 效果等级从 0 开始计数：amplifier 9 对应生命恢复 X。 */
    public static final int SLEEP_REGENERATION_AMPLIFIER = 9;
    public static final int TOTEM_REGENERATION_DURATION = 900;
    public static final int TOTEM_ABSORPTION_DURATION = 100;
    public static final int TOTEM_FIRE_RESISTANCE_DURATION = 800;

    public static final float RETREAT_EAT_CHANCE = 0.50F;
    public static final float DEFENSE_EAT_CHANCE = 0.60F;
    /** 防御时未触发进食后，尝试进行特殊决策的概率。 */
    public static final float DEFENSE_SUMMON_CHANCE = 0.35F;

    public static final int PHASE_ONE_SUMMON_MIN = 1;
    public static final int PHASE_ONE_SUMMON_MAX = 3;
    public static final int PHASE_TWO_SUMMON_MIN = 5;
    public static final int PHASE_TWO_SUMMON_MAX = 8;

    public static final int MELEE_COOLDOWN = 30;
    public static final int CHARGE_COOLDOWN = 80;
    public static final int DODGE_COOLDOWN = 80;
    public static final int DEFENSE_COOLDOWN = 160;
    public static final int SPECIAL_DECISION_COOLDOWN = 150;
    public static final int ACTION_COOLDOWN = 10;
    public static final int JUMP_COOLDOWN = 40;
    public static final int ASCEND_DURATION = 80;
    public static final double ASCEND_HEIGHT = 8.0D;
    public static final float ASCEND_SPIN_DEGREES = 9.0F;
    public static final double GLIDE_MIN_SPEED = 0.45D;
    public static final double GLIDE_MAX_SPEED = 1.60D;
    public static final float GLIDE_TURN_LERP = 0.12F;
    public static final double GLIDE_HEIGHT_LIMIT = 14.0D;
    /** 普通第三阶段沿用原起飞高度。 */
    public static final double TAKEOFF_HEIGHT = 20.0D;
    public static final double TAKEOFF_STEP = 0.50D;
    public static final int TAKEOFF_MAX_DURATION = 60;
    /** 严格低于该生命值时才使用“毁灭打击”。 */
    public static final float DOOM_STRIKE_HEALTH_THRESHOLD = 20.0F;
    public static final double DOOM_STRIKE_TAKEOFF_HEIGHT = 100.0D;
    /** 100 格按每 tick 0.5 格上升需要 200 tick，额外预留碰撞与状态延迟。 */
    public static final int DOOM_STRIKE_MAX_DURATION = 220;
    /** 毁灭打击起飞的蓄力、爆发和垂直速度参数。 */
    public static final int DOOM_STRIKE_TAKEOFF_CHARGE_TICKS = 8;
    public static final double DOOM_STRIKE_TAKEOFF_INITIAL_SPEED = 1.15D;
    public static final double DOOM_STRIKE_TAKEOFF_MAX_SPEED = 2.40D;
    public static final double DOOM_STRIKE_TAKEOFF_ACCELERATION = 0.06D;
    public static final double DOOM_STRIKE_TAKEOFF_DECELERATION = 0.035D;
    public static final double DOOM_STRIKE_TAKEOFF_DECELERATION_PROGRESS = 0.62D;
    public static final int DOOM_STRIKE_TAKEOFF_BURST_PARTICLES = 34;
    /** 毁灭打击按高度触发的三段烟花加速。 */
    public static final int DOOM_STRIKE_FIREWORK_STAGE_COUNT = 3;
    public static final int DOOM_STRIKE_FIREWORK_BOOST_TICKS = 12;
    public static final double DOOM_STRIKE_FIREWORK_BOOST_MULTIPLIER = 1.35D;
    public static final int DOOM_STRIKE_FIREWORK_PARTICLES_PER_STAGE = 24;
    public static final double DESCEND_INITIAL_SPEED = 0.90D;
    public static final double DESCEND_MAX_SPEED = 1.80D;
    public static final double DESCEND_ACCELERATION = 0.08D;
    public static final float DESCEND_TURN_LERP = 0.14F;
    public static final int DESCEND_MAX_DURATION = 80;
    public static final float DESCEND_BASE_DAMAGE = 6.0F;
    public static final float DESCEND_DAMAGE_PER_BLOCK = 1.0F;
    public static final float DESCEND_MAX_DAMAGE_BONUS = 18.0F;
    public static final float DESCEND_AOE_MULTIPLIER = 0.5F;
    public static final double DESCEND_AOE_RADIUS = 3.5D;
    public static final double DESCEND_KNOCKBACK = 0.7D;
    /** 毁灭打击专用的高速俯冲参数；普通降临仍使用上面的原参数。 */
    public static final double DOOM_STRIKE_DESCEND_INITIAL_SPEED = 1.15D;
    public static final double DOOM_STRIKE_DESCEND_MAX_SPEED = 3.20D;
    public static final double DOOM_STRIKE_DESCEND_ACCELERATION = 0.13D;
    public static final float DOOM_STRIKE_DESCEND_TURN_LERP = 0.06F;
    public static final int TAKEOFF_COOLDOWN = 400;
    /** 第三阶段落地三连冲撞的独立冷却，与逃离和起飞互不影响。 */
    public static final int PHASE3_COMBO_COOLDOWN = 160;

    /** 每次三连冲撞复合动作严格包含三次独立冲撞。 */
    public static final int PHASE3_COMBO_STRIKE_COUNT = 3;
    /** 每击之间的可见重新瞄准时间。 */
    public static final int PHASE3_COMBO_AIM_DURATION = 10;
    /** 单次冲撞最大持续时间，配合最大位移防止无限冲刺。 */
    public static final int PHASE3_COMBO_CHARGE_MAX_DURATION = 14;
    /** 烟花助推开始时的瞬时冲撞速度。 */
    public static final double PHASE3_COMBO_INITIAL_SPEED = 1.15D;
    /** 烟花助推结束后维持的冲撞速度。 */
    public static final double PHASE3_COMBO_CRUISE_SPEED = 0.72D;
    /** 瞬时助推持续的游戏刻数。 */
    public static final int PHASE3_COMBO_BOOST_TICKS = 4;
    /** 单次冲撞的最大位移。 */
    public static final double PHASE3_COMBO_MAX_DISTANCE = 14.0D;
    /** 飞行中允许朝目标修正方向的最大插值比例。 */
    public static final float PHASE3_COMBO_TURN_LERP = 0.10F;
    public static final double PHASE3_COMBO_KNOCKBACK = 1.15D;
    public static final double PHASE3_COMBO_UPWARD = 0.22D;
    public static final int PHASE3_COMBO_TRAIL_INTERVAL = 2;
    public static final int PHASE3_COMBO_PARTICLE_COUNT = 24;

    /** 倒立和召唤分别独立抽取，不能把两个概率相加当作一个概率。 */
    public static final float INVERTED_CHANCE = 0.25F;
    public static final float SUMMON_CHANCE = 0.35F;
    /** 满血时不会由倒立转入睡眠。 */
    public static final float SLEEP_CHANCE_AT_FULL_HEALTH = 0.0F;
    /** 生命值降至触发生命值时达到的睡眠概率上限。 */
    public static final float SLEEP_CHANCE_AT_THRESHOLD_HEALTH = 0.80F;
    /** 睡眠概率从满血开始线性提高，并在该生命值及以下保持上限。 */
    public static final float SLEEP_CHANCE_THRESHOLD_HEALTH = 150.0F;
    /** 二阶段冲撞触发烟花推进的概率；每次冲撞只判定一次。 */
    public static final float PHASE_TWO_CHARGE_BOOST_CHANCE = 0.8F;
    public static final int CHARGE_BOOST_DURATION_TICKS = 4;
    public static final double CHARGE_BOOST_SPEED_MULTIPLIER = 2.5D;
    public static final int CHARGE_BOOST_PARTICLE_COUNT = 32;

    private static final double SPECIAL_DISTANCE_SQR = 8.0D * 8.0D;
    private static final double RETREAT_HEALTH_RATIO = 0.4D;
    private static final double RETREAT_SAFE_DISTANCE_SQR = 12.0D * 12.0D;
    private static final double DODGE_STEP = 0.22D;
    private static final double RETREAT_PATH_SPEED = 1.5D;
    private static final double CHARGE_KNOCKBACK = 1.2D;
    private static final double CHARGE_UPWARD = 0.55D;
    private static final double DEFENSE_TRIGGER_RANGE_SQR = 12.0D * 12.0D;
    private static final double RANGED_DODGE_RANGE_SQR = 32.0D * 32.0D;
    private static final double RANGED_AIM_DOT_THRESHOLD = 0.985D;

    private static final EntityDataAccessor<Byte> DATA_STATE =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BYTE);
    /** 上一个服务端状态，仅用于客户端姿态过渡，不参与任何机制判定。 */
    private static final EntityDataAccessor<Byte> DATA_PREVIOUS_STATE =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Long> DATA_STATE_START =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_DODGE_RIGHT =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SECOND_PHASE =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DATA_PHASE_START =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_CHARGE_BOOST =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Long> DATA_SHIELD_BREAK_START =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> DATA_EATING =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_THIRD_PHASE =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TAKEOFF_READY =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    /** 同步毁灭打击模式，供客户端独立计算高速俯冲姿态。 */
    private static final EntityDataAccessor<Boolean> DATA_DOOM_STRIKE =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_TAKEOFF_PROGRESS =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_AIR_EATING =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BOOLEAN);
    /** 当前三连击已完成的次数；客户端用于衔接每一击的姿态与鞘翅姿态。 */
    private static final EntityDataAccessor<Byte> DATA_PHASE3_COMBO_STRIKE =
            SynchedEntityData.defineId(Qunyou.class, EntityDataSerializers.BYTE);

    private final ServerBossEvent bossEvent =
            new ServerBossEvent(this.getDisplayName(), BossBarColor.RED, BossBarOverlay.NOTCHED_10);
    private final Set<UUID> chargeVictims = new HashSet<>();
    /** 每一次三连击中的单次冲撞独立使用一份命中记录。 */
    private final Set<UUID> phase3ComboVictims = new HashSet<>();
    private final Set<UUID> pigEjectionVictims = new HashSet<>();
    private final Set<UUID> activePigCloneIds = new HashSet<>();
    private final List<QunyouCarrotProjectile> summonedCarrots = new ArrayList<>();
    private final List<QunyouGoldenCarrotProjectile> goldenCarrots = new ArrayList<>();

    private int stateCooldown;
    private int meleeCooldown;
    private int chargeCooldown;
    private int dodgeCooldown;
    private int defenseCooldown;
    private int jumpCooldown;
    private int specialCooldown;
    private int chargeBoostTicksRemaining;
    private int jumpMaxTicks;
    private int takeoffCooldown;
    private int phase3ComboCooldown;
    private int phase3ComboChargesCompleted;
    private double phase3ComboChargeDistance;
    private int pigEjectionCooldown;
    private int pigEjectionChainHits;
    private UUID pigEjectionChainTarget;
    private int pigEjectionBoostStage;
    private int pigEjectionBoostTicker;
    private double pigEjectionSpeed;
    private double pigEjectionDistance;
    private double pigEjectionAscendStartY;
    private Vec3 pigEjectionDirection = Vec3.ZERO;
    private int pigMissileCooldown;
    private Vec3 pigRetreatDirection = Vec3.ZERO;
    private double pigRetreatSpeed;
    private double pigRetreatDistance;
    private double pigRetreatAscendStartY;
    private int pigManeuverCooldown;
    private int pigManeuverCheckTicker;
    private double pigManeuverStartY;
    private int airEatingCooldown;
    private int airEatingCheckTicker;
    private boolean airEatingActive;
    private boolean airEatingRewardGranted;
    private int airEatingTicks;
    private int pigMissileLaunchTicker;
    private int pigMissileLaunched;
    private int pigMissileCount;
    private UUID pigMissileTarget;
    private boolean pigMissileSequenceActive;
    private UUID cloneOwnerUuid;
    private int takeoffFireworkStage;
    private int takeoffFireworkBoostTicks;
    private double takeoffVerticalSpeed;
    private boolean takeoffBurstTriggered;
    /** 本次起飞是否锁定为生命值低于 10 的毁灭打击。 */
    private boolean doomStrikeActive;
    private boolean undyingTotemUsed;
    private double jumpTargetY;
    private boolean secondPhase;
    private boolean thirdPhase;
    private boolean phase3Pending;
    private double ascendStartY;
    private double takeoffStartY;
    /** 第三阶段空中战斗锁定的有效目标，避免升到跟随范围外后脱战。 */
    private LivingEntity phase3AirTarget;
    private double descentDistance;
    private int descentTicks;
    private boolean descendingImpactResolved;
    private Vec3 descendDirection = Vec3.ZERO;
    private boolean eatingActive;
    private boolean eatingRewardGranted;
    private int eatingTicks;
    private boolean nextDodgeRight;
    private Vec3 chargeDirection = Vec3.ZERO;
    private Vec3 dodgeDirection = Vec3.ZERO;
    private Vec3 retreatDestination;
    private int summonCount;
    private int summonLaunched;
    private boolean defenseSummonActive;
    private int defenseSummonCount;
    private int defenseSummonLaunched;
    private int defenseSummonTicks;
    private boolean meleeHit;

    public Qunyou(EntityType<? extends Qunyou> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.xpReward = 50;
        this.bossEvent.setDarkenScreen(false);
        this.entityData.set(DATA_STATE_START, level.getGameTime());
    }

    public static Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, PIG_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** 猪分身独立属性：普通猪生命值，其余战斗属性沿用群友。 */
    public static Builder createCloneAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, PIG_MOVEMENT_SPEED)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    public boolean isClone() {
        return false;
    }

    public int getCombatPhase() {
        return this.thirdPhase ? 3 : this.secondPhase ? 2 : 1;
    }

    /** 分身生成时初始化阶段；后续阶段由主群友在每个服务端 tick 实时同步。 */
    public void configureClonePhase(int phase) {
        this.secondPhase = phase >= 2;
        this.thirdPhase = phase >= 3;
        this.phase3Pending = phase >= 3;
        this.entityData.set(DATA_SECOND_PHASE, this.secondPhase);
        this.entityData.set(DATA_THIRD_PHASE, this.thirdPhase);
        this.entityData.set(DATA_PHASE_START, this.level().getGameTime());
        this.entityData.set(DATA_STATE, IDLE);
        this.entityData.set(DATA_STATE_START, this.level().getGameTime());
    }

    public void configureCloneOwner(UUID ownerUuid) {
        this.cloneOwnerUuid = ownerUuid;
    }

    /**
     * 运行时同步本体阶段。与生成初始化分开，不重置当前动作、生命值或任何技能冷却。
     * 第三阶段初始化标记只在首次同步进入第三阶段时置位。
     */
    private void applyCloneCombatPhase(int phase) {
        boolean nextSecondPhase = phase >= 2;
        boolean nextThirdPhase = phase >= 3;
        boolean phaseChanged = this.secondPhase != nextSecondPhase || this.thirdPhase != nextThirdPhase;
        boolean enteringThirdPhase = !this.thirdPhase && nextThirdPhase;
        this.secondPhase = nextSecondPhase;
        this.thirdPhase = nextThirdPhase;
        if (enteringThirdPhase) {
            this.phase3Pending = true;
        } else if (!nextThirdPhase) {
            this.phase3Pending = false;
        }
        this.entityData.set(DATA_SECOND_PHASE, this.secondPhase);
        this.entityData.set(DATA_THIRD_PHASE, this.thirdPhase);
        if (phaseChanged) {
            this.entityData.set(DATA_PHASE_START, this.level().getGameTime());
        }
    }

    /** 只接受仍加载、存活且确实为主群友的关联实体。 */
    private Qunyou resolveValidCloneOwner() {
        if (!this.isClone() || this.cloneOwnerUuid == null
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.cloneOwnerUuid);
        if (entity instanceof Qunyou owner && owner != this && !owner.isClone()
                && owner.level() == this.level() && owner.isAlive() && !owner.isRemoved()) {
            return owner;
        }
        return null;
    }

    /**
     * 分身的目标只允许来自本体。即使原版伤害流程或其他 Goal 尝试写入攻击者，
     * 非本体当前目标也会在这里被拒绝。
     */
    @Override
    public void setTarget(LivingEntity target) {
        if (this.isClone() && target != null) {
            Qunyou owner = this.resolveValidCloneOwner();
            LivingEntity ownerTarget = owner == null ? null : owner.getTarget();
            if (ownerTarget == null || !ownerTarget.getUUID().equals(target.getUUID())) {
                return;
            }
        }
        super.setTarget(target);
    }

    /** 分身阶段与目标的唯一同步入口，在每个服务端 tick 的自定义 AI 决策前执行。 */
    private void syncCloneFromOwner() {
        if (!this.isClone() || this.level().isClientSide) {
            return;
        }
        Qunyou owner = this.resolveValidCloneOwner();
        LivingEntity synchronizedTarget = null;
        if (owner != null) {
            this.applyCloneCombatPhase(owner.getCombatPhase());
            LivingEntity ownerTarget = owner.getTarget();
            if (owner.isValidCombatTarget(ownerTarget)) {
                synchronizedTarget = ownerTarget;
            }
        }

        LivingEntity previousTarget = this.getTarget();
        boolean targetChanged = previousTarget == null
                ? synchronizedTarget != null
                : synchronizedTarget == null || !previousTarget.getUUID().equals(synchronizedTarget.getUUID());
        // 绕过本类限制直接写入服务端同步值，保证旧目标在丢失本体时也一定被清除。
        super.setTarget(synchronizedTarget);
        this.phase3AirTarget = synchronizedTarget;
        if (targetChanged) {
            this.pigEjectionChainHits = 0;
            this.pigEjectionChainTarget = null;
            this.pigMissileTarget = null;
            this.clearPigMissileSequence();
            if (this.getState() == CHARGE) {
                this.chargeVictims.clear();
                if (synchronizedTarget != null) {
                    this.chargeDirection = horizontalDirection(synchronizedTarget);
                }
            }
            if (this.getState() == PHASE3_COMBO_CHARGE) {
                this.phase3ComboVictims.clear();
            }
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, IDLE);
        builder.define(DATA_PREVIOUS_STATE, IDLE);
        builder.define(DATA_STATE_START, 0L);
        builder.define(DATA_DODGE_RIGHT, false);
        builder.define(DATA_SECOND_PHASE, false);
        builder.define(DATA_PHASE_START, 0L);
        builder.define(DATA_CHARGE_BOOST, false);
        builder.define(DATA_SHIELD_BREAK_START, 0L);
        builder.define(DATA_EATING, false);
        builder.define(DATA_THIRD_PHASE, false);
        builder.define(DATA_TAKEOFF_READY, true);
        builder.define(DATA_DOOM_STRIKE, false);
        builder.define(DATA_TAKEOFF_PROGRESS, 0.0F);
        builder.define(DATA_AIR_EATING, false);
        builder.define(DATA_PHASE3_COMBO_STRIKE, (byte) 0);
    }

    @Override
    protected void registerGoals() {
        if (!this.isClone()) {
            this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class,
                    10, true, false, this::isValidCombatTarget));
        }
    }

    public byte getState() {
        return this.entityData.get(DATA_STATE);
    }

    public byte getPreviousState() {
        return this.entityData.get(DATA_PREVIOUS_STATE);
    }

    public float getStateTicks(float partialTick) {
        return Math.max(0.0F, this.level().getGameTime() - this.entityData.get(DATA_STATE_START) + partialTick);
    }

    public boolean isDodgeRight() {
        return this.entityData.get(DATA_DODGE_RIGHT);
    }

    public boolean isSecondPhase() {
        return this.entityData.get(DATA_SECOND_PHASE);
    }

    public boolean isChargeBoostActive() {
        return this.entityData.get(DATA_CHARGE_BOOST);
    }

    public boolean isDefending() {
        return this.getState() == DEFENDING;
    }

    public boolean isEating() {
        return this.entityData.get(DATA_EATING) || this.entityData.get(DATA_AIR_EATING);
    }

    public boolean isAirEating() {
        return this.entityData.get(DATA_AIR_EATING);
    }

    public boolean isThirdPhase() {
        return this.entityData.get(DATA_THIRD_PHASE);
    }

    public boolean isTakeoffReady() {
        return this.entityData.get(DATA_TAKEOFF_READY);
    }

    public boolean isDoomStrikeActive() {
        return this.entityData.get(DATA_DOOM_STRIKE);
    }

    private void setDoomStrikeActive(boolean active) {
        this.doomStrikeActive = active;
        this.entityData.set(DATA_DOOM_STRIKE, active);
    }

    public float getTakeoffProgress() {
        return this.entityData.get(DATA_TAKEOFF_PROGRESS);
    }

    public int getPhase3ComboStrike() {
        return this.entityData.get(DATA_PHASE3_COMBO_STRIKE);
    }

    public int getPhaseTicks(float partialTick) {
        long start = this.entityData.get(DATA_PHASE_START);
        return start <= 0L ? -1 : (int) Math.max(0L, this.level().getGameTime() - start + (long) partialTick);
    }

    public int getShieldBreakTicks(float partialTick) {
        long start = this.entityData.get(DATA_SHIELD_BREAK_START);
        return start <= 0L ? -1 : (int) Math.max(0L, this.level().getGameTime() - start + (long) partialTick);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
            return;
        }

        if (!this.isAlive() || this.isRemoved()) {
            this.clearAllSummons();
            return;
        }

        if (this.isClone()) {
            this.syncCloneFromOwner();
        }
        this.updateBossBar();
        this.updatePhase();
        this.tickCooldowns();
        this.tickEatingAction();
        if (this.phase3Pending && !this.eatingActive && this.getState() == IDLE) {
            this.enterState(PHASE3_PENDING);
            return;
        }
        LivingEntity target = this.getTarget();
        switch (this.getState()) {
            case MELEE -> this.tickMelee(target);
            case CHARGE_PREPARE -> this.tickChargePrepare(target);
            case CHARGE -> this.tickCharge(target);
            case DODGE -> this.tickDodge(target);
            case RETREAT -> this.tickRetreat(target);
            case INVERTED -> this.tickInverted();
            case SLEEPING -> this.tickSleeping();
            case FLIP -> this.tickFlip();
            case SUMMON -> this.tickSummon(target);
            case DEFENDING -> this.tickDefending(target);
            case JUMPING -> this.tickJumping(target);
            case PHASE3_PENDING -> this.beginAscending();
            case ASCENDING -> this.tickAscending(target);
            case GLIDING -> this.tickGliding(target);
            case TAKEOFF -> this.tickTakeoff(target);
            case DESCENDING_ATTACK -> this.tickDescendingAttack(target);
            case PHASE3_GROUND_WAIT -> this.tickPhase3GroundWait(target);
            case PHASE3_COMBO_AIM -> this.tickPhase3ComboAim(target);
            case PHASE3_COMBO_CHARGE -> this.tickPhase3ComboCharge(target);
            case PHASE3_EJECTION_CHARGE -> this.tickPigEjectionCharge(target);
            case PHASE3_EJECTION_RUSH -> this.tickPigEjectionRush(target);
            case PHASE3_EJECTION_RECOVER -> this.tickPigEjectionRecover(target);
            case PHASE3_EJECTION_ASCEND -> this.tickPigEjectionAscend(target);
            case PHASE3_PIG_RETREAT_CHARGE -> this.tickPigRetreatCharge(target);
            case PHASE3_PIG_RETREAT_HORIZONTAL -> this.tickPigRetreatHorizontal(target);
            case PHASE3_PIG_RETREAT_ASCEND -> this.tickPigRetreatAscend(target);
            case PHASE3_AIR_MANEUVER -> this.tickPigManeuver(target);
            default -> this.tickIdle(target);
        }
    }

    private void tickCooldowns() {
        this.stateCooldown = Math.max(0, this.stateCooldown - 1);
        this.meleeCooldown = Math.max(0, this.meleeCooldown - 1);
        this.chargeCooldown = Math.max(0, this.chargeCooldown - 1);
        this.dodgeCooldown = Math.max(0, this.dodgeCooldown - 1);
        this.defenseCooldown = Math.max(0, this.defenseCooldown - 1);
        this.jumpCooldown = Math.max(0, this.jumpCooldown - 1);
        this.phase3ComboCooldown = Math.max(0, this.phase3ComboCooldown - 1);
        this.pigEjectionCooldown = Math.max(0, this.pigEjectionCooldown - 1);
        this.pigMissileCooldown = Math.max(0, this.pigMissileCooldown - 1);
        this.pigManeuverCooldown = Math.max(0, this.pigManeuverCooldown - 1);
        this.airEatingCooldown = Math.max(0, this.airEatingCooldown - 1);
        if (this.takeoffCooldown > 0) {
            this.takeoffCooldown--;
            if (this.takeoffCooldown == 0) {
                this.entityData.set(DATA_TAKEOFF_READY, true);
            }
        }
        this.specialCooldown = Math.max(0, this.specialCooldown - 1);
    }

    private void tickIdle(LivingEntity target) {
        if (this.phase3Pending) {
            if (!this.eatingActive) {
                this.enterState(PHASE3_PENDING);
            }
            return;
        }
        if (this.thirdPhase) {
            this.resumePhase3AirCombat(target);
            return;
        }
        if (!this.isValidCombatTarget(target)) {
            this.setTarget(null);
            this.getNavigation().stop();
            return;
        }
        this.faceTarget(target);
        if (this.tryDodgeAimedRangedWeapon()) {
            return;
        }
        double distanceSqr = this.distanceToSqr(target);

        // 只有目标稳定站在地面上且高出至少两格时才起跳，避免把被击飞的目标当作高台目标。
        if (this.jumpCooldown == 0 && this.onGround() && target.onGround()
                && distanceSqr <= 256.0D && target.getY() - this.getY() >= JUMP_TRIGGER_HEIGHT) {
            this.beginJump(target);
            return;
        }

        if (distanceSqr > SPECIAL_DISTANCE_SQR) {
            if (this.specialCooldown == 0) {
                this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
                // 两个独立抽取；两者同时触发时召唤优先，但不会把概率相加。
                boolean summon = this.random.nextFloat() < SUMMON_CHANCE;
                boolean inverted = this.random.nextFloat() < INVERTED_CHANCE;
                if (summon) {
                    this.beginSummon(target);
                    return;
                }
                if (inverted) {
                    this.enterState(INVERTED);
                    return;
                }
            }
            this.getNavigation().moveTo(target, 1.0D);
            return;
        }

        if (this.stateCooldown > 0) {
            if (distanceSqr > 9.0D) {
                this.getNavigation().moveTo(target, 1.0D);
            } else {
                this.getNavigation().stop();
            }
            return;
        }

        if (this.isSecondPhase() && this.defenseCooldown == 0
                && distanceSqr <= DEFENSE_TRIGGER_RANGE_SQR) {
            this.beginDefense(target);
            return;
        }

        if (this.getHealth() < this.getMaxHealth() * RETREAT_HEALTH_RATIO
                && distanceSqr < 16.0D) {
            this.beginEscape(target);
            return;
        }

        if (distanceSqr <= 9.0D && this.meleeCooldown == 0) {
            this.enterState(MELEE);
            return;
        }

        if (distanceSqr > 9.0D && this.chargeCooldown == 0) {
            this.beginChargePrepare(target);
            return;
        }

        if (distanceSqr > 3.2D) {
            this.getNavigation().moveTo(target, 1.0D);
        } else {
            this.getNavigation().stop();
        }
    }

    private void tickMelee(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.meleeCooldown = MELEE_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
            return;
        }
        this.faceTarget(target);
        int ticks = (int) this.getStateTicks(0.0F);
        if (!this.meleeHit && ticks >= MELEE_IMPACT_TICK) {
            this.meleeHit = true;
            if (this.distanceToSqr(target) <= 9.0D && this.hasLineOfSight(target)) {
                target.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            }
        }
        if (ticks >= MELEE_DURATION) {
            this.meleeCooldown = MELEE_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
        }
    }

    private void beginChargePrepare(LivingEntity target) {
        this.chargeDirection = horizontalDirection(target);
        this.chargeVictims.clear();
        this.enterState(CHARGE_PREPARE);
    }

    private void tickChargePrepare(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.chargeCooldown = CHARGE_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
            return;
        }
        this.faceTarget(target);
        this.chargeDirection = horizontalDirection(target);
        if (this.getStateTicks(0.0F) >= CHARGE_PREPARE_DURATION) {
            this.beginCharge();
        }
    }

    private void beginCharge() {
        this.chargeVictims.clear();
        this.chargeBoostTicksRemaining = 0;
        this.enterState(CHARGE);
        if (this.isSecondPhase() && this.random.nextFloat() < PHASE_TWO_CHARGE_BOOST_CHANCE) {
            this.chargeBoostTicksRemaining = CHARGE_BOOST_DURATION_TICKS;
            this.entityData.set(DATA_CHARGE_BOOST, true);
            this.spawnChargeBoostEffects(CHARGE_BOOST_PARTICLE_COUNT, true);
        }
    }

    private void tickCharge(LivingEntity target) {
        if (!this.isValidCombatTarget(target) || this.chargeDirection.lengthSqr() < 1.0E-6D) {
            this.chargeCooldown = CHARGE_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
            return;
        }

        this.faceDirection(this.chargeDirection);
        double movementSpeed = CHARGE_SPEED * (this.chargeBoostTicksRemaining > 0
                ? CHARGE_BOOST_SPEED_MULTIPLIER : 1.0D);
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = center.add(this.chargeDirection.scale(movementSpeed));
        BlockHitResult blockHit = this.level().clip(new ClipContext(center, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 step = blockHit.getType() == HitResult.Type.MISS
                ? this.chargeDirection.scale(movementSpeed)
                : blockHit.getLocation().subtract(center);
        AABB sweep = this.getBoundingBox().expandTowards(step).inflate(0.25D);
        for (LivingEntity victim : this.level().getEntitiesOfClass(
                LivingEntity.class, sweep,
                candidate -> this.isValidCombatTarget(candidate) && this.isAllowedAttackTarget(candidate))) {
            if (victim.getBoundingBox().inflate(0.2D).intersects(sweep)
                    && this.chargeVictims.add(victim.getUUID())) {
                victim.hurt(this.damageSources().mobAttack(this),
                        (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                victim.setDeltaMovement(victim.getDeltaMovement().add(
                        this.chargeDirection.x * CHARGE_KNOCKBACK,
                        CHARGE_UPWARD,
                        this.chargeDirection.z * CHARGE_KNOCKBACK));
                victim.hurtMarked = true;
                this.chargeCooldown = CHARGE_COOLDOWN;
                this.finishState(ACTION_COOLDOWN);
                return;
            }
        }

        this.move(MoverType.SELF, step);
        if (blockHit.getType() != HitResult.Type.MISS || this.horizontalCollision
                || this.getStateTicks(0.0F) >= CHARGE_DURATION) {
            this.chargeCooldown = CHARGE_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
            return;
        }
        if (this.chargeBoostTicksRemaining > 0) {
            this.spawnChargeBoostEffects(4, false);
            if (--this.chargeBoostTicksRemaining == 0) {
                this.entityData.set(DATA_CHARGE_BOOST, false);
            }
        }
    }

    private void beginJump(LivingEntity target) {
        Vec3 direction = horizontalDirection(target);
        this.jumpTargetY = target.getY() + JUMP_HEIGHT_MARGIN;
        double rise = Math.max(1.0D, this.jumpTargetY - this.getY());
        double verticalSpeed = Math.sqrt(2.0D * JUMP_GRAVITY * (rise + 0.75D)) + 0.05D;
        this.jumpMaxTicks = Math.max(JUMP_DURATION, (int) Math.ceil(2.0D * verticalSpeed / JUMP_GRAVITY) + 8);
        this.enterState(JUMPING);
        this.jumpCooldown = JUMP_COOLDOWN;
        this.setDeltaMovement(direction.x * JUMP_HORIZONTAL_SPEED, verticalSpeed,
                direction.z * JUMP_HORIZONTAL_SPEED);
        this.hasImpulse = true;
        this.level().playSound(null, this.blockPosition(), SoundEvents.PIG_STEP, SoundSource.HOSTILE,
                0.9F, 1.2F);
    }

    private void tickJumping(LivingEntity target) {
        if (this.isValidCombatTarget(target)) {
            this.faceTarget(target);
        }
        int ticks = (int) this.getStateTicks(0.0F);
        if (this.getY() >= this.jumpTargetY) {
            if (this.phase3Pending) {
                this.finishState(ACTION_COOLDOWN);
            } else if (this.isValidCombatTarget(target)) {
                this.chargeDirection = horizontalDirection(target);
                this.beginCharge();
            } else {
                this.finishState(ACTION_COOLDOWN);
            }
            return;
        }
        if (ticks >= this.jumpMaxTicks || (ticks >= 4 && this.onGround())) {
            this.finishState(ACTION_COOLDOWN);
        }
    }

    private void tickDodge(LivingEntity target) {
        if (this.getStateTicks(0.0F) >= DODGE_DURATION || this.horizontalCollision) {
            this.dodgeCooldown = DODGE_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
            return;
        }
        if (this.isValidCombatTarget(target)) {
            this.faceTarget(target);
        }
        this.move(MoverType.SELF, this.dodgeDirection.scale(DODGE_STEP));
    }

    /**
     * 所有阶段共用的逃离入口。第三阶段可替换为猪撤退，否则进入真实 RETREAT；
     * 真实 RETREAT 启动后统一进行猪分身判定，且不再使用逃离冷却。
     */
    private void beginEscape(LivingEntity target) {
        if (this.getState() == RETREAT || this.getState() == PHASE3_PIG_RETREAT_CHARGE
                || this.getState() == PHASE3_PIG_RETREAT_HORIZONTAL
                || this.getState() == PHASE3_PIG_RETREAT_ASCEND) {
            return;
        }
        if (this.thirdPhase && this.onGround() && this.tryBeginPigRetreat(target)) {
            return;
        }
        this.beginRetreat(target);
        if (this.getState() == RETREAT) {
            this.trySummonPigClones();
        }
    }

    private void beginRetreat(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            return;
        }
        this.retreatDestination = null;
        this.enterState(RETREAT);
        this.updateRetreatDestination(target);
        if (this.random.nextFloat() < RETREAT_EAT_CHANCE) {
            this.beginEating(target);
        }
    }

    private void tickRetreat(LivingEntity target) {
        if (!this.isValidCombatTarget(target) || this.getStateTicks(0.0F) >= RETREAT_DURATION
                || this.distanceToSqr(target) >= RETREAT_SAFE_DISTANCE_SQR) {
            this.finishState(ACTION_COOLDOWN);
            return;
        }
        if ((int) this.getStateTicks(0.0F) % 10 == 0 || this.retreatDestination == null) {
            this.updateRetreatDestination(target);
        }
        if (this.retreatDestination != null) {
            this.getNavigation().moveTo(this.retreatDestination.x, this.retreatDestination.y, this.retreatDestination.z,
                    RETREAT_PATH_SPEED);
        }
    }

    private void updateRetreatDestination(LivingEntity target) {
        Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 1.0E-6D) {
            away = new Vec3(-Mth.sin(this.getYRot() * Mth.DEG_TO_RAD), 0.0D,
                    Mth.cos(this.getYRot() * Mth.DEG_TO_RAD));
        }
        this.retreatDestination = this.position().add(away.normalize().scale(8.0D));
    }

    private void beginSummon(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
            return;
        }
        boolean advancedPhase = this.isSecondPhase() || this.isThirdPhase();
        int minCount = advancedPhase ? PHASE_TWO_SUMMON_MIN : PHASE_ONE_SUMMON_MIN;
        int maxCount = advancedPhase ? PHASE_TWO_SUMMON_MAX : PHASE_ONE_SUMMON_MAX;
        this.summonCount = minCount + this.random.nextInt(maxCount - minCount + 1);
        this.summonLaunched = 0;
        this.clearAllSummons();
        this.enterState(SUMMON);
        for (int i = 0; i < this.summonCount; i++) {
            Vec3 position = this.hoverPosition(i, this.summonCount, 0);
            QunyouCarrotProjectile carrot = new QunyouCarrotProjectile(
                    ModEntityTypes.QUNYOU_CARROT.get(), this.level());
            carrot.setOwner(this);
            carrot.setHoverPosition(position);
            this.level().addFreshEntity(carrot);
            this.summonedCarrots.add(carrot);
        }
    }

    private void tickSummon(LivingEntity target) {
        int ticks = (int) this.getStateTicks(0.0F);
        if (!this.isValidCombatTarget(target)) {
            this.clearAllSummons();
            this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
            return;
        }

        this.faceTarget(target);
        for (int i = 0; i < this.summonedCarrots.size(); i++) {
            QunyouCarrotProjectile carrot = this.summonedCarrots.get(i);
            if (!carrot.isLaunched() && !carrot.isRemoved()) {
                carrot.setHoverPosition(this.hoverPosition(i, this.summonCount, ticks));
            }
        }

        if (ticks >= SUMMON_HOVER_TICKS) {
            int due = (ticks - SUMMON_HOVER_TICKS) / SUMMON_LAUNCH_INTERVAL;
            while (this.summonLaunched <= due && this.summonLaunched < this.summonedCarrots.size()) {
                QunyouCarrotProjectile carrot = this.summonedCarrots.get(this.summonLaunched++);
                if (!carrot.isRemoved()) {
                    carrot.launch(target);
                }
            }
        }

        int duration = SUMMON_HOVER_TICKS + Math.max(0, this.summonCount - 1) * SUMMON_LAUNCH_INTERVAL
                + SUMMON_END_DELAY;
        if (ticks >= duration) {
            this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
            this.releaseHoveringCarrots();
            this.finishState(ACTION_COOLDOWN);
        }
    }

    private void updatePhase() {
        if (this.isClone()) {
            return;
        }
        if (!this.secondPhase && this.getHealth() < SECOND_PHASE_HEALTH_THRESHOLD) {
            this.secondPhase = true;
            this.entityData.set(DATA_SECOND_PHASE, true);
            this.entityData.set(DATA_PHASE_START, this.level().getGameTime());
            this.level().playSound(null, this.blockPosition(), SoundEvents.PIG_AMBIENT, SoundSource.HOSTILE,
                    1.0F, 0.75F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.8D, this.getZ(),
                        24, 0.5D, 0.5D, 0.5D, 0.15D);
            }
        }

        if (!this.thirdPhase && this.getHealth() < THIRD_PHASE_HEALTH_THRESHOLD) {
            this.thirdPhase = true;
            this.phase3Pending = true;
            this.entityData.set(DATA_THIRD_PHASE, true);
            this.level().playSound(null, this.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE,
                    1.0F, 1.25F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 1.0D, this.getZ(),
                        32, 0.8D, 0.8D, 0.8D, 0.12D);
            }
        }
    }
    private void spawnChargeBoostEffects(int particleCount, boolean playSound) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.getX() + this.chargeDirection.x * 0.4D,
                this.getY() + 0.6D,
                this.getZ() + this.chargeDirection.z * 0.4D,
                particleCount,
                -this.chargeDirection.x * 0.25D, 0.12D, -this.chargeDirection.z * 0.25D, 0.16D);
        if (playSound) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                    SoundSource.HOSTILE, 1.0F, 0.9F);
        }
    }

    private void beginDefense(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.defenseCooldown = DEFENSE_COOLDOWN;
            return;
        }

        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
        this.setItemSlot(EquipmentSlot.OFFHAND, shield);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
        this.startUsingItem(InteractionHand.OFF_HAND);
        if (!this.isUsingItem()) {
            this.clearDefenseResources();
            this.defenseCooldown = DEFENSE_COOLDOWN;
            return;
        }

        this.entityData.set(DATA_SHIELD_BREAK_START, 0L);
        this.enterState(DEFENDING);
        this.faceTarget(target);
        this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE,
                0.8F, 0.9F);
        if (this.random.nextFloat() < DEFENSE_EAT_CHANCE) {
            this.beginEating(target);
        } else if (this.specialCooldown == 0
                && this.random.nextFloat() < DEFENSE_SUMMON_CHANCE) {
            // 进食与特殊决策互斥：两者都占用嘴部/头部动作，不叠加播放。
            this.beginDefenseSummon(target);
        }
    }

    private void tickDefending(LivingEntity target) {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (this.isValidCombatTarget(target)) {
            this.faceTarget(target);
        }
        this.tickDefenseSummon(target);
        if (!this.isUsingItem()) {
            this.endDefense(false);
            return;
        }
        if (this.getStateTicks(0.0F) >= DEFENSE_DURATION) {
            this.endDefense(false);
        }
    }

    /** 防御覆盖动作：保持举盾状态，同时召唤并依次发射胡萝卜。 */
    private void beginDefenseSummon(LivingEntity target) {
        if (this.getState() != DEFENDING || this.eatingActive || !this.isValidCombatTarget(target)) {
            return;
        }
        this.clearAllSummons();
        boolean advancedPhase = this.isSecondPhase() || this.isThirdPhase();
        int minCount = advancedPhase ? PHASE_TWO_SUMMON_MIN : PHASE_ONE_SUMMON_MIN;
        int maxCount = advancedPhase ? PHASE_TWO_SUMMON_MAX : PHASE_ONE_SUMMON_MAX;
        this.defenseSummonCount = minCount + this.random.nextInt(maxCount - minCount + 1);
        this.defenseSummonLaunched = 0;
        this.defenseSummonTicks = 0;
        this.defenseSummonActive = true;
        this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
        for (int i = 0; i < this.defenseSummonCount; i++) {
            Vec3 position = this.hoverPosition(i, this.defenseSummonCount, 0);
            QunyouCarrotProjectile carrot = new QunyouCarrotProjectile(
                    ModEntityTypes.QUNYOU_CARROT.get(), this.level());
            carrot.setOwner(this);
            carrot.setHoverPosition(position);
            this.level().addFreshEntity(carrot);
            this.summonedCarrots.add(carrot);
        }
    }

    private void tickDefenseSummon(LivingEntity target) {
        if (!this.defenseSummonActive) {
            return;
        }
        if (this.getState() != DEFENDING || !this.isValidCombatTarget(target)) {
            this.cancelDefenseSummon(true);
            return;
        }
        this.defenseSummonTicks++;
        for (int i = 0; i < this.summonedCarrots.size(); i++) {
            QunyouCarrotProjectile carrot = this.summonedCarrots.get(i);
            if (!carrot.isLaunched() && !carrot.isRemoved()) {
                carrot.setHoverPosition(this.hoverPosition(i, this.defenseSummonCount,
                        this.defenseSummonTicks));
            }
        }
        if (this.defenseSummonTicks >= SUMMON_HOVER_TICKS) {
            int due = (this.defenseSummonTicks - SUMMON_HOVER_TICKS) / SUMMON_LAUNCH_INTERVAL;
            while (this.defenseSummonLaunched <= due
                    && this.defenseSummonLaunched < this.summonedCarrots.size()) {
                QunyouCarrotProjectile carrot = this.summonedCarrots.get(this.defenseSummonLaunched++);
                if (!carrot.isRemoved()) {
                    carrot.launch(target);
                }
            }
        }
        int duration = SUMMON_HOVER_TICKS
                + Math.max(0, this.defenseSummonCount - 1) * SUMMON_LAUNCH_INTERVAL
                + SUMMON_END_DELAY;
        if (this.defenseSummonTicks >= duration) {
            this.defenseSummonActive = false;
            this.summonedCarrots.clear();
        }
    }

    private void cancelDefenseSummon(boolean dropUnlaunched) {
        if (!this.defenseSummonActive) {
            return;
        }
        if (dropUnlaunched) {
            this.dropHoveringCarrots();
        } else {
            this.releaseHoveringCarrots();
        }
        this.defenseSummonActive = false;
        this.defenseSummonTicks = 0;
        this.defenseSummonLaunched = 0;
        this.defenseSummonCount = 0;
    }

    private void endDefense(boolean broken) {
        if (this.getState() != DEFENDING) {
            return;
        }
        this.cancelDefenseSummon(true);
        this.clearDefenseResources();
        this.defenseCooldown = DEFENSE_COOLDOWN;
        if (broken) {
            this.entityData.set(DATA_SHIELD_BREAK_START, this.level().getGameTime());
            this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BREAK,
                    SoundSource.HOSTILE, 1.0F, 0.9F);
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.8D, this.getZ(),
                        20, 0.6D, 0.5D, 0.6D, 0.2D);
            }
        }
        this.finishState(ACTION_COOLDOWN);
    }

    private void clearDefenseResources() {
        if (this.isUsingItem()) {
            this.stopUsingItem();
        }
        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
    }

    @Override
    protected void blockUsingShield(LivingEntity attacker) {
        super.blockUsingShield(attacker);
        if (attacker instanceof Player player && this.validPlayer(player) && attacker.canDisableShield()) {
            this.endDefense(true);
        }
    }

    private void beginEating(LivingEntity target) {
        if (!this.isValidCombatTarget(target) || this.eatingActive) {
            return;
        }
        if (this.getState() != RETREAT && this.getState() != DEFENDING) {
            return;
        }
        ItemStack carrot = new ItemStack(Items.CARROT);
        this.setItemSlot(EquipmentSlot.MAINHAND, carrot);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.eatingActive = true;
        this.eatingRewardGranted = false;
        this.eatingTicks = 0;
        this.entityData.set(DATA_EATING, true);
    }

    private void tickEatingAction() {
        if (!this.eatingActive) {
            return;
        }
        if (!this.isAlive() || this.isRemoved() || !this.getMainHandItem().is(Items.CARROT)) {
            this.clearEatingResources();
            return;
        }
        byte state = this.getState();
        if (state != RETREAT && state != DEFENDING && state != IDLE) {
            this.interruptEating();
            return;
        }

        this.eatingTicks++;
        if (this.eatingTicks >= 7 && (this.eatingTicks - 7) % 4 == 0) {
            this.playEatingEffects();
        }
        if (this.eatingTicks >= EAT_DURATION) {
            this.finishEatingSuccessfully();
        }
    }

    private void playEatingEffects() {
        ItemStack carrot = this.getMainHandItem();
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE,
                0.5F + 0.5F * this.random.nextInt(2),
                (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, carrot),
                    this.getX(), this.getEyeY(), this.getZ(), 5, 0.12D, 0.12D, 0.12D, 0.02D);
        }
    }

    private void finishEatingSuccessfully() {
        if (!this.eatingActive || this.eatingRewardGranted
                || !this.isAlive() || this.isRemoved()) {
            return;
        }
        this.eatingRewardGranted = true;
        this.clearEatingResources();
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                EAT_REGENERATION_DURATION, 1, false, true, true));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                EAT_STRENGTH_DURATION, 2, false, true, true));
    }

    private void interruptEating() {
        if (!this.eatingActive) {
            return;
        }
        this.eatingRewardGranted = false;
        this.clearEatingResources();
    }

    private void clearEatingResources() {
        this.eatingActive = false;
        this.eatingTicks = 0;
        this.entityData.set(DATA_EATING, false);
        if (this.getMainHandItem().is(Items.CARROT)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }
    private void beginAscending() {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        this.phase3Pending = false;
        this.clearPhase3ComboRuntime();
        this.clearDefenseResources();
        this.interruptEating();
        this.interruptAirEating();
        this.releaseHoveringCarrots();
        this.setSharedFlag(7, false);
        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        this.ascendStartY = this.getY();
        this.enterState(ASCENDING);
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
    }

    private void tickAscending(LivingEntity target) {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        int ticks = (int) this.getStateTicks(0.0F);
        this.setYRot((this.getYRot() + ASCEND_SPIN_DEGREES) % 360.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();

        double step = ASCEND_HEIGHT / ASCEND_DURATION;
        this.move(MoverType.SELF, new Vec3(0.0D, step, 0.0D));
        boolean reachedHeight = this.getY() - this.ascendStartY >= ASCEND_HEIGHT - 0.05D;
        if (reachedHeight || ticks >= ASCEND_DURATION || this.verticalCollision) {
            this.beginGliding(target);
        }
    }

    private void equipElytra() {
        ItemStack chest = this.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.is(Items.ELYTRA)) {
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
            this.setItemSlot(EquipmentSlot.CHEST, elytra);
            this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        }
    }

    private void clearPhase3Equipment() {
        this.setSharedFlag(7, false);
        if (this.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            this.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        }
    }

    private void beginGliding(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        this.equipElytra();
        this.enterState(GLIDING);
        this.setNoGravity(false);
        this.setSharedFlag(7, true);
        this.getNavigation().stop();
        this.setDeltaMovement(this.getLookAngle().scale(0.70D));
    }
    private void tickGliding(LivingEntity target) {
        target = this.resolvePhase3AirTarget(target);
        this.getNavigation().stop();
        if (this.onGround()) {
            this.handlePhase3Landing(target);
            return;
        }
        if (this.isInWater() || this.isInLava()) {
            this.beginPhase3GroundWait(target);
            return;
        }
        this.tickPhase3AirActions(target);
        if (this.getState() != GLIDING) {
            return;
        }
        this.setSharedFlag(7, true);
        if (this.isValidCombatTarget(target)) {
            this.steerGlideToward(target);
        }
        Vec3 look = this.getLookAngle();
        double speed = this.getDeltaMovement().length();
        if (speed < GLIDE_MIN_SPEED) {
            this.setDeltaMovement(this.getDeltaMovement().add(look.scale(0.05D)));
        } else if (speed > GLIDE_MAX_SPEED) {
            this.setDeltaMovement(this.getDeltaMovement().scale(GLIDE_MAX_SPEED / speed));
        }
    }

    private void steerGlideToward(LivingEntity target) {
        Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition());
        if (this.getY() > target.getY() + GLIDE_HEIGHT_LIMIT) {
            toTarget = new Vec3(toTarget.x, -GLIDE_HEIGHT_LIMIT, toTarget.z);
        }
        double horizontal = toTarget.horizontalDistance();
        float desiredYaw = (float) (Mth.atan2(-toTarget.x, toTarget.z) * (180.0D / Math.PI));
        float desiredPitch = (float) (Mth.atan2(-toTarget.y, Math.max(0.01D, horizontal)) * (180.0D / Math.PI));
        desiredPitch = Mth.clamp(desiredPitch, -65.0F, 65.0F);
        float turnLerp = this.airEatingActive
                ? ModConfigs.QUNYOU_AIR_EATING_TURN_LERP.get().floatValue()
                : GLIDE_TURN_LERP;
        float yaw = Mth.rotLerp(turnLerp, this.getYRot(), desiredYaw);
        float pitch = Mth.lerp(turnLerp, this.getXRot(), desiredPitch);
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /** 第三阶段落地后的唯一分流入口，避免滑翔状态在地面上持续套用空中运动学。 */
    private void handlePhase3Landing(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        this.getNavigation().stop();
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        if (!this.isValidCombatTarget(target)) {
            this.beginPhase3GroundWait(null);
            return;
        }
        this.resumePhase3GroundDecision(target);
    }

    /**
     * 第三阶段地面动作的统一决策入口。三连冲撞、胡萝卜射击与起飞分别检查
     * 自己的攻击冷却；三种攻击都不可用时才使用无冷却的逃离作为安全兜底。
     */
    private void resumePhase3GroundDecision(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        if (!this.isValidCombatTarget(target)) {
            this.beginPhase3GroundWait(null);
            return;
        }
        if (!this.onGround()) {
            this.beginGliding(target);
            return;
        }
        if (this.pigEjectionCooldown == 0) {
            this.startPigEjectionChain(target);
            return;
        }
        if (this.phase3ComboCooldown == 0) {
            this.beginPhase3Combo(target);
            return;
        }
        if (this.specialCooldown == 0) {
            this.beginSummon(target);
            return;
        }
        if (this.takeoffCooldown == 0) {
            this.beginTakeoff(target);
            return;
        }
        this.beginEscape(target);
    }

    /** Boss 从逃离等第三阶段动作回到 IDLE 后，恢复空战循环而不是重新播放升天。 */
    private void resumePhase3AirCombat(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.setTarget(null);
            if (this.onGround()) {
                this.beginPhase3GroundWait(null);
            } else {
                this.beginGliding(null);
            }
            return;
        }
        if (!this.onGround() && !this.isInWater() && !this.isInLava()) {
            this.beginGliding(target);
            return;
        }
        this.resumePhase3GroundDecision(target);
    }

    private void beginPhase3Combo(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved() || !this.isValidCombatTarget(target)
                || this.phase3ComboCooldown > 0) {
            this.resumePhase3GroundDecision(target);
            return;
        }
        this.equipElytra();
        this.phase3ComboCooldown = PHASE3_COMBO_COOLDOWN;
        this.clearPhase3ComboRuntime();
        this.level().playSound(null, this.blockPosition(), SoundEvents.PIG_AMBIENT, SoundSource.HOSTILE,
                1.0F, 0.78F);
        this.beginPhase3ComboAim(target);
    }

    private void beginPhase3ComboAim(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved() || !this.isValidCombatTarget(target)) {
            this.abortPhase3Combo(target);
            return;
        }
        this.enterState(PHASE3_COMBO_AIM);
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.faceTarget(target);
    }

    private void tickPhase3ComboAim(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.abortPhase3Combo(target);
            return;
        }
        this.faceTarget(target);
        if (this.getStateTicks(0.0F) >= PHASE3_COMBO_AIM_DURATION) {
            this.beginPhase3ComboCharge(target);
        }
    }

    private void beginPhase3ComboCharge(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved() || !this.isValidCombatTarget(target)) {
            this.abortPhase3Combo(target);
            return;
        }
        this.chargeDirection = horizontalDirection(target);
        this.phase3ComboVictims.clear();
        this.phase3ComboChargeDistance = 0.0D;
        this.enterState(PHASE3_COMBO_CHARGE);
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.faceDirection(this.chargeDirection);
        this.spawnChargeBoostEffects(PHASE3_COMBO_PARTICLE_COUNT, true);
    }

    private void tickPhase3ComboCharge(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.abortPhase3Combo(target);
            return;
        }

        int ticks = (int) this.getStateTicks(0.0F);
        Vec3 desired = horizontalDirection(target);
        this.chargeDirection = this.chargeDirection.scale(1.0D - PHASE3_COMBO_TURN_LERP)
                .add(desired.scale(PHASE3_COMBO_TURN_LERP)).normalize();
        this.faceDirection(this.chargeDirection);

        double speed = ticks < PHASE3_COMBO_BOOST_TICKS
                ? PHASE3_COMBO_INITIAL_SPEED : PHASE3_COMBO_CRUISE_SPEED;
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = center.add(this.chargeDirection.scale(speed));
        BlockHitResult blockHit = this.level().clip(new ClipContext(center, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 moveEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        Vec3 step = moveEnd.subtract(center);

        AABB sweep = this.getBoundingBox().expandTowards(step).inflate(0.30D);
        if (target.getBoundingBox().inflate(0.35D).intersects(sweep)
                && target.getBoundingBox().inflate(0.35D).clip(center, moveEnd).isPresent()
                && this.phase3ComboVictims.add(target.getUUID())) {
            target.hurt(this.damageSources().mobAttack(this),
                    (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
            target.setDeltaMovement(target.getDeltaMovement().add(
                    this.chargeDirection.x * PHASE3_COMBO_KNOCKBACK,
                    PHASE3_COMBO_UPWARD,
                    this.chargeDirection.z * PHASE3_COMBO_KNOCKBACK));
            target.hurtMarked = true;
            this.completePhase3ComboCharge(target);
            return;
        }

        this.move(MoverType.SELF, step);
        this.phase3ComboChargeDistance += step.length();
        if (ticks % PHASE3_COMBO_TRAIL_INTERVAL == 0) {
            this.spawnChargeBoostEffects(4, false);
        }
        if (blockHit.getType() != HitResult.Type.MISS || this.horizontalCollision
                || ticks >= PHASE3_COMBO_CHARGE_MAX_DURATION
                || this.phase3ComboChargeDistance >= PHASE3_COMBO_MAX_DISTANCE) {
            this.completePhase3ComboCharge(target);
        }
    }

    private void completePhase3ComboCharge(LivingEntity target) {
        this.phase3ComboChargesCompleted++;
        this.entityData.set(DATA_PHASE3_COMBO_STRIKE, (byte) this.phase3ComboChargesCompleted);
        this.phase3ComboVictims.clear();
        this.setDeltaMovement(Vec3.ZERO);
        if (this.phase3ComboChargesCompleted >= PHASE3_COMBO_STRIKE_COUNT) {
            this.clearPhase3ComboRuntime();
            if (this.isValidCombatTarget(target)) {
                // 三连击结束后重新决策：起飞可用就起飞，否则才进入无冷却逃离。
                this.resumePhase3GroundDecision(target);
            } else {
                this.abortPhase3Combo(target);
            }
            return;
        }
        if (this.isValidCombatTarget(target)) {
            this.beginPhase3ComboAim(target);
        } else {
            this.abortPhase3Combo(target);
        }
    }

    private void abortPhase3Combo(LivingEntity target) {
        this.clearPhase3ComboRuntime();
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        if (this.onGround()) {
            this.beginPhase3GroundWait(target);
        } else {
            this.beginGliding(target);
        }
    }

    private void clearPhase3ComboRuntime() {
        this.phase3ComboChargesCompleted = 0;
        this.phase3ComboChargeDistance = 0.0D;
        this.phase3ComboVictims.clear();
        this.entityData.set(DATA_PHASE3_COMBO_STRIKE, (byte) 0);
    }

    private void startPigEjectionChain(LivingEntity target) {
        // 这里绝不能回调 resumePhase3GroundDecision：如果地面判定不成立，
        // 决策器会再次选择猪弹射并造成无限递归。
        if (!this.thirdPhase || !this.onGround() || this.pigEjectionCooldown > 0) {
            return;
        }
        if (!this.isValidCombatTarget(target)) {
            this.beginPhase3GroundWait(null);
            return;
        }
        this.pigEjectionChainHits = 0;
        this.pigEjectionChainTarget = null;
        this.beginPigEjectionCharge(target);
    }

    private void beginPigEjectionCharge(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.failPigEjection(target);
            return;
        }
        this.phase3AirTarget = target;
        this.pigEjectionCooldown = ModConfigs.QUNYOU_PIG_EJECTION_COOLDOWN.get();
        this.pigEjectionVictims.clear();
        this.pigEjectionBoostStage = 0;
        this.pigEjectionBoostTicker = 0;
        this.pigEjectionDistance = 0.0D;
        this.enterState(PHASE3_EJECTION_CHARGE);
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.faceTarget(target);
    }

    private void tickPigEjectionCharge(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (!this.isValidPigEjectionTarget(target)) {
            this.failPigEjection(target);
            return;
        }
        this.faceTarget(target);
        if (this.getStateTicks(0.0F) >= ModConfigs.QUNYOU_PIG_EJECTION_CHARGE_TICKS.get()) {
            this.beginPigEjectionRush(target);
        }
    }

    private void beginPigEjectionRush(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            this.failPigEjection(target);
            return;
        }
        this.phase3AirTarget = target;
        this.pigEjectionDirection = horizontalDirection(target);
        this.pigEjectionVictims.clear();
        this.pigEjectionBoostStage = 1;
        this.pigEjectionBoostTicker = 0;
        this.pigEjectionSpeed = ModConfigs.QUNYOU_PIG_EJECTION_INITIAL_SPEED.get();
        this.pigEjectionDistance = 0.0D;
        this.enterState(PHASE3_EJECTION_RUSH);
        this.setNoGravity(true);
        this.setSharedFlag(7, false);
        this.faceDirection(this.pigEjectionDirection);
        this.spawnPigEjectionBoost(this.pigEjectionBoostStage);
    }

    private void tickPigEjectionRush(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (!this.isValidPigEjectionTarget(target)) {
            this.failPigEjection(target);
            return;
        }
        Vec3 desired = horizontalDirection(target);
        double turn = ModConfigs.QUNYOU_PIG_EJECTION_TURN_LERP.get();
        this.pigEjectionDirection = this.pigEjectionDirection.scale(1.0D - turn)
                .add(desired.scale(turn)).normalize();
        this.faceDirection(this.pigEjectionDirection);

        this.pigEjectionBoostTicker++;
        int interval = ModConfigs.QUNYOU_PIG_EJECTION_BOOST_INTERVAL.get();
        int maxStages = ModConfigs.QUNYOU_PIG_EJECTION_BOOST_STAGES.get();
        if (this.pigEjectionBoostStage < maxStages && this.pigEjectionBoostTicker >= interval) {
            this.pigEjectionBoostStage++;
            this.pigEjectionBoostTicker = 0;
            this.pigEjectionSpeed = Math.min(ModConfigs.QUNYOU_PIG_EJECTION_MAX_SPEED.get(),
                    this.pigEjectionSpeed + ModConfigs.QUNYOU_PIG_EJECTION_ACCELERATION.get());
            this.spawnPigEjectionBoost(this.pigEjectionBoostStage);
        }

        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = center.add(this.pigEjectionDirection.scale(this.pigEjectionSpeed));
        BlockHitResult blockHit = this.level().clip(new ClipContext(center, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 moveEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        Vec3 step = moveEnd.subtract(center);
        AABB sweep = this.getBoundingBox().expandTowards(step).inflate(0.30D);
        LivingEntity primaryHit = null;
        Player hitPlayerVictim = null;
        for (LivingEntity victim : this.level().getEntitiesOfClass(
                LivingEntity.class, sweep,
                candidate -> this.isValidCombatTarget(candidate) && this.isAllowedAttackTarget(candidate))) {
            if (!victim.getBoundingBox().inflate(0.20D).intersects(sweep)
                    || !this.pigEjectionVictims.add(victim.getUUID())) {
                continue;
            }
            victim.hurt(this.damageSources().mobAttack(this),
                    ModConfigs.QUNYOU_PIG_EJECTION_DAMAGE.get().floatValue());
            victim.push(this.pigEjectionDirection.x * ModConfigs.QUNYOU_PIG_EJECTION_KNOCKBACK.get(),
                    ModConfigs.QUNYOU_PIG_EJECTION_UPWARD.get(),
                    this.pigEjectionDirection.z * ModConfigs.QUNYOU_PIG_EJECTION_KNOCKBACK.get());
            victim.hurtMarked = true;
            if (primaryHit == null) {
                primaryHit = victim;
            }
            if (victim instanceof Player player) {
                hitPlayerVictim = player;
            }
        }
        if (primaryHit != null) {
            this.completePigEjectionHit(primaryHit, hitPlayerVictim);
            return;
        }

        this.move(MoverType.SELF, step);
        this.pigEjectionDistance += step.length();
        if (this.getStateTicks(0.0F) % 2 == 0) {
            this.spawnPigEjectionTrail();
        }
        if (blockHit.getType() != HitResult.Type.MISS || this.horizontalCollision
                || this.pigEjectionDistance >= ModConfigs.QUNYOU_PIG_EJECTION_MAX_DISTANCE.get()
                || this.getStateTicks(0.0F) >= ModConfigs.QUNYOU_PIG_EJECTION_MAX_TICKS.get()) {
            this.beginPigEjectionAscend(target);
        }
    }

    private void completePigEjectionHit(LivingEntity target, Player hitPlayer) {
        if (hitPlayer != null && this.isValidCombatTarget(hitPlayer)) {
            this.pigEjectionChainHits++;
            int maxHits = ModConfigs.QUNYOU_PIG_EJECTION_MAX_CHASE_HITS.get();
            this.pigEjectionChainTarget = hitPlayer.getUUID();
            if (this.pigEjectionChainHits < maxHits) {
                this.pigEjectionCooldown = 0;
                this.phase3AirTarget = hitPlayer;
                this.enterState(PHASE3_EJECTION_RECOVER);
                this.setNoGravity(false);
                this.faceTarget(hitPlayer);
                return;
            }
        }
        this.pigEjectionChainHits = 0;
        this.pigEjectionChainTarget = null;
        this.pigEjectionCooldown = ModConfigs.QUNYOU_PIG_EJECTION_COOLDOWN.get();
        if (this.onGround()) {
            this.beginPhase3GroundWait(target);
        } else {
            this.beginGliding(target);
        }
    }

    private void tickPigEjectionRecover(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (!this.isValidPigEjectionTarget(target)) {
            this.failPigEjection(target);
            return;
        }
        if (this.pigEjectionChainTarget != null && !this.pigEjectionChainTarget.equals(target.getUUID())) {
            this.failPigEjection(target);
            return;
        }
        this.faceTarget(target);
        if (this.getStateTicks(0.0F)
                >= ModConfigs.QUNYOU_PIG_EJECTION_CHASE_RECOVERY_TICKS.get()) {
            this.beginPigEjectionCharge(target);
        }
    }

    private void beginPigEjectionAscend(LivingEntity target) {
        this.phase3AirTarget = this.isValidCombatTarget(target) ? target : null;
        this.pigEjectionAscendStartY = this.getY();
        Vec3 horizontal = target != null && this.isValidCombatTarget(target)
                ? horizontalDirection(target) : this.pigEjectionDirection.multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        }
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        double angle = Math.toRadians(ModConfigs.QUNYOU_PIG_EJECTION_ASCEND_ANGLE.get());
        this.pigEjectionDirection = new Vec3(
                horizontal.x * Math.cos(angle), Math.sin(angle), horizontal.z * Math.cos(angle)).normalize();
        this.enterState(PHASE3_EJECTION_ASCEND);
        this.equipElytra();
        this.setNoGravity(true);
        this.setSharedFlag(7, true);
        this.setDeltaMovement(this.pigEjectionDirection.scale(
                ModConfigs.QUNYOU_PIG_EJECTION_ASCEND_SPEED.get()));
        this.faceDirection3D(this.pigEjectionDirection);
    }

    private void tickPigEjectionAscend(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (this.isValidCombatTarget(target)) {
            Vec3 desired = horizontalDirection(target);
            double turn = ModConfigs.QUNYOU_PIG_EJECTION_TURN_LERP.get() * 0.5D;
            this.pigEjectionDirection = this.pigEjectionDirection.scale(1.0D - turn)
                    .add(desired.scale(turn)).normalize();
        }
        this.setDeltaMovement(this.pigEjectionDirection.scale(
                ModConfigs.QUNYOU_PIG_EJECTION_ASCEND_SPEED.get()));
        this.faceDirection3D(this.pigEjectionDirection);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.getY() - this.pigEjectionAscendStartY
                >= ModConfigs.QUNYOU_PIG_EJECTION_ASCEND_HEIGHT.get()
                || this.horizontalCollision || this.verticalCollision
                || this.getStateTicks(0.0F) >= 80) {
            this.pigEjectionChainHits = 0;
            this.pigEjectionChainTarget = null;
            this.beginGliding(target);
        }
    }

    private boolean isValidPigEjectionTarget(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            return false;
        }
        if (this.pigEjectionChainTarget != null && !this.pigEjectionChainTarget.equals(target.getUUID())) {
            return false;
        }
        double range = ModConfigs.QUNYOU_PIG_EJECTION_CHASE_RANGE.get();
        return this.distanceToSqr(target) <= range * range;
    }

    private void failPigEjection(LivingEntity target) {
        this.pigEjectionChainHits = 0;
        this.pigEjectionChainTarget = null;
        this.pigEjectionCooldown = ModConfigs.QUNYOU_PIG_EJECTION_COOLDOWN.get();
        if (this.onGround()) {
            this.beginPhase3GroundWait(target);
        } else {
            this.beginGliding(target);
        }
    }

    private void spawnPigEjectionBoost(int stage) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 back = this.pigEjectionDirection.scale(-0.45D);
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.getX() + back.x, this.getY() + 0.55D, this.getZ() + back.z,
                18 + stage * 8, 0.25D, 0.18D, 0.25D, 0.18D);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, 1.0F, 0.85F + stage * 0.06F);
    }

    private void spawnPigEjectionTrail() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 back = this.pigEjectionDirection.scale(-0.40D);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX() + back.x, this.getY() + 0.35D, this.getZ() + back.z,
                3, 0.10D, 0.08D, 0.10D, 0.02D);
    }

    /** 猪导弹的脱手召唤序列；不占用群友本体状态或滑翔运动。 */
    private void tickPigMissile(LivingEntity target) {
        if (this.pigMissileSequenceActive) {
            this.tickPigMissileSequence();
            return;
        }
        if (this.getState() != GLIDING || this.airEatingActive
                || this.pigMissileCooldown > 0 || !this.isValidCombatTarget(target)) {
            return;
        }
        // 冷却好且处于无冲突滑翔状态时立即释放，不再进行概率判定。
        this.beginPigMissileSequence(target);
    }

    private void beginPigMissileSequence(LivingEntity target) {
        if (!this.isValidCombatTarget(target) || this.pigMissileCooldown > 0) {
            return;
        }
        this.clearPigMissileSequence();
        this.pigMissileTarget = target.getUUID();
        this.pigMissileCount = ModConfigs.QUNYOU_PIG_MISSILE_COUNT.get();
        this.pigMissileLaunched = 0;
        this.pigMissileLaunchTicker = 0;
        this.pigMissileSequenceActive = true;
        this.pigMissileCooldown = ModConfigs.QUNYOU_PIG_MISSILE_COOLDOWN.get();
        for (int i = 0; i < this.pigMissileCount; i++) {
            QunyouGoldenCarrotProjectile projectile = new QunyouGoldenCarrotProjectile(
                    ModEntityTypes.QUNYOU_GOLDEN_CARROT.get(), this.level());
            projectile.setOwner(this);
            projectile.setHoverPosition(this.hoverPosition(i, this.pigMissileCount, 0));
            this.level().addFreshEntity(projectile);
            this.goldenCarrots.add(projectile);
        }
    }

    private void tickPigMissileSequence() {
        if (!this.pigMissileSequenceActive) {
            return;
        }
        LivingEntity target = this.resolvePigMissileTarget();
        if (!this.isValidCombatTarget(target)) {
            this.clearPigMissileSequence();
            return;
        }
        for (int i = 0; i < this.goldenCarrots.size(); i++) {
            QunyouGoldenCarrotProjectile projectile = this.goldenCarrots.get(i);
            if (!projectile.isLaunched() && !projectile.isRemoved()) {
                projectile.setHoverPosition(this.hoverPosition(i, this.pigMissileCount,
                        this.pigMissileLaunchTicker));
            }
        }
        this.pigMissileLaunchTicker++;
        if (this.pigMissileLaunchTicker >= ModConfigs.QUNYOU_PIG_MISSILE_LAUNCH_INTERVAL.get()
                && this.pigMissileLaunched < this.goldenCarrots.size()) {
            QunyouGoldenCarrotProjectile projectile = this.goldenCarrots.get(this.pigMissileLaunched++);
            if (!projectile.isRemoved()) {
                projectile.launch(target);
            }
            this.pigMissileLaunchTicker = 0;
        }
        if (this.pigMissileLaunched >= this.goldenCarrots.size()) {
            this.goldenCarrots.clear();
            this.pigMissileSequenceActive = false;
            this.pigMissileTarget = null;
        }
    }

    private LivingEntity resolvePigMissileTarget() {
        if (this.pigMissileTarget == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Entity entity = serverLevel.getEntity(this.pigMissileTarget);
        return entity instanceof LivingEntity living ? living : null;
    }

    private void clearPigMissileSequence() {
        for (QunyouGoldenCarrotProjectile projectile : this.goldenCarrots) {
            if (!projectile.isLaunched() && !projectile.isRemoved()) {
                projectile.discard();
            }
        }
        this.goldenCarrots.clear();
        this.pigMissileSequenceActive = false;
        this.pigMissileLaunchTicker = 0;
        this.pigMissileLaunched = 0;
        this.pigMissileCount = 0;
        this.pigMissileTarget = null;
    }

    /** 当原有地面决策选择猪逃离时，以配置概率改为猪撤退。 */
    private boolean tryBeginPigRetreat(LivingEntity target) {
        if (!this.thirdPhase || !this.onGround() || !this.isValidCombatTarget(target)
                || this.random.nextDouble() >= ModConfigs.QUNYOU_PIG_RETREAT_REPLACE_CHANCE.get()) {
            return false;
        }
        this.beginPigRetreat(target);
        return true;
    }

    private void beginPigRetreat(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            return;
        }
        this.phase3AirTarget = target;
        Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        this.pigRetreatDirection = away.lengthSqr() < 1.0E-6D
                ? this.getLookAngle().multiply(1.0D, 0.0D, 1.0D).scale(-1.0D) : away.normalize();
        this.pigRetreatSpeed = 0.0D;
        this.pigRetreatDistance = 0.0D;
        this.enterState(PHASE3_PIG_RETREAT_CHARGE);
        this.equipElytra();
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.faceDirection(this.pigRetreatDirection);
    }

    private void tickPigRetreatCharge(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (this.isValidCombatTarget(target)) {
            Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-6D) {
                this.pigRetreatDirection = this.pigRetreatDirection.scale(0.88D)
                        .add(away.normalize().scale(0.12D)).normalize();
                this.faceDirection(this.pigRetreatDirection);
            }
        }
        if (this.getStateTicks(0.0F) >= ModConfigs.QUNYOU_PIG_RETREAT_CHARGE_TICKS.get()) {
            this.beginPigRetreatHorizontal(target);
        }
    }

    private void beginPigRetreatHorizontal(LivingEntity target) {
        this.phase3AirTarget = this.isValidCombatTarget(target) ? target : null;
        this.enterState(PHASE3_PIG_RETREAT_HORIZONTAL);
        this.equipElytra();
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.pigRetreatSpeed = ModConfigs.QUNYOU_PIG_RETREAT_HORIZONTAL_SPEED.get();
        this.pigRetreatDistance = 0.0D;
        this.faceDirection(this.pigRetreatDirection);
        this.spawnPigRetreatBurst(true);
    }

    private void tickPigRetreatHorizontal(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (this.isValidCombatTarget(target)) {
            Vec3 away = this.position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() > 1.0E-6D) {
                this.pigRetreatDirection = this.pigRetreatDirection.scale(0.92D)
                        .add(away.normalize().scale(0.08D)).normalize();
            }
        }
        this.faceDirection(this.pigRetreatDirection);
        this.pigRetreatSpeed += ModConfigs.QUNYOU_PIG_RETREAT_ACCELERATION.get();
        Vec3 center = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = center.add(this.pigRetreatDirection.scale(this.pigRetreatSpeed));
        BlockHitResult blockHit = this.level().clip(new ClipContext(center, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 moveEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        Vec3 step = moveEnd.subtract(center);
        this.move(MoverType.SELF, step);
        this.pigRetreatDistance += step.length();
        if (this.getStateTicks(0.0F) % 2 == 0) {
            this.spawnPigRetreatTrail();
        }
        if (blockHit.getType() != HitResult.Type.MISS || this.horizontalCollision
                || this.pigRetreatDistance >= ModConfigs.QUNYOU_PIG_RETREAT_DISTANCE.get()
                || this.getStateTicks(0.0F) >= ModConfigs.QUNYOU_PIG_RETREAT_HORIZONTAL_TICKS.get()) {
            this.beginPigRetreatAscend(target);
        }
    }

    private void beginPigRetreatAscend(LivingEntity target) {
        this.phase3AirTarget = this.isValidCombatTarget(target) ? target : null;
        this.pigRetreatAscendStartY = this.getY();
        Vec3 horizontal = this.pigRetreatDirection.multiply(1.0D, 0.0D, 1.0D);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            horizontal = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontal = horizontal.normalize();
        double angle = Math.toRadians(ModConfigs.QUNYOU_PIG_RETREAT_ASCEND_ANGLE.get());
        this.pigRetreatDirection = new Vec3(
                horizontal.x * Math.cos(angle), Math.sin(angle), horizontal.z * Math.cos(angle)).normalize();
        this.enterState(PHASE3_PIG_RETREAT_ASCEND);
        this.equipElytra();
        this.setNoGravity(true);
        this.setSharedFlag(7, true);
        this.setDeltaMovement(this.pigRetreatDirection.scale(
                ModConfigs.QUNYOU_PIG_RETREAT_ASCEND_SPEED.get()));
        this.faceDirection3D(this.pigRetreatDirection);
        this.spawnPigRetreatBurst(false);
    }

    private void tickPigRetreatAscend(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        this.setDeltaMovement(this.pigRetreatDirection.scale(
                ModConfigs.QUNYOU_PIG_RETREAT_ASCEND_SPEED.get()));
        this.faceDirection3D(this.pigRetreatDirection);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.getY() - this.pigRetreatAscendStartY
                >= ModConfigs.QUNYOU_PIG_RETREAT_ASCEND_HEIGHT.get()
                || this.horizontalCollision || this.verticalCollision
                || this.getStateTicks(0.0F) >= 100) {
            this.beginGliding(target);
        }
    }

    private void spawnPigRetreatBurst(boolean horizontal) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 back = this.pigRetreatDirection.scale(-0.45D);
        int count = ModConfigs.QUNYOU_PIG_RETREAT_BURST_PARTICLES.get();
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.getX() + back.x, this.getY() + 0.5D, this.getZ() + back.z,
                horizontal ? count : count + 10, 0.25D, 0.18D, 0.25D, 0.18D);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, horizontal ? 0.9F : 1.1F, horizontal ? 0.9F : 1.2F);
    }

    private void spawnPigRetreatTrail() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 back = this.pigRetreatDirection.scale(-0.35D);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX() + back.x, this.getY() + 0.35D, this.getZ() + back.z,
                3, 0.10D, 0.08D, 0.10D, 0.018D);
    }

    /** 第三阶段滑翔中的动作选择器：空中进食、猪机动、猪导弹共用互斥判定。 */
    private void tickPhase3AirActions(LivingEntity target) {
        if (this.airEatingActive) {
            this.tickAirEating(target);
            return;
        }
        if (this.pigMissileSequenceActive) {
            this.tickPigMissile(target);
            return;
        }
        if (this.pigMissileCooldown == 0 && this.isValidCombatTarget(target)) {
            this.tickPigMissile(target);
            return;
        }
        if (this.tryStartAirEating(target)) {
            this.tickAirEating(target);
            return;
        }
        if (this.tryStartPigManeuver(target)) {
            return;
        }
    }

    private boolean tryStartPigManeuver(LivingEntity target) {
        if (this.getState() != GLIDING || !this.isValidCombatTarget(target)
                || this.airEatingActive || this.pigMissileSequenceActive
                || this.pigManeuverCooldown > 0) {
            this.pigManeuverCheckTicker = 0;
            return false;
        }
        this.pigManeuverCheckTicker++;
        if (this.pigManeuverCheckTicker < ModConfigs.QUNYOU_PIG_MANEUVER_CHECK_INTERVAL.get()) {
            return false;
        }
        this.pigManeuverCheckTicker = 0;
        if (this.random.nextDouble() >= ModConfigs.QUNYOU_PIG_MANEUVER_CHANCE.get()) {
            return false;
        }
        this.beginPigManeuver(target);
        return true;
    }

    private void beginPigManeuver(LivingEntity target) {
        if (!this.isValidCombatTarget(target)) {
            return;
        }
        this.phase3AirTarget = target;
        this.pigManeuverCooldown = ModConfigs.QUNYOU_PIG_MANEUVER_COOLDOWN.get();
        // 猪机动会立即刷新猪导弹冷却，但不跳过猪导弹自身的概率判定间隔。
        this.pigMissileCooldown = 0;
        this.pigManeuverStartY = this.getY();
        this.enterState(PHASE3_AIR_MANEUVER);
        this.equipElytra();
        this.setNoGravity(true);
        this.setSharedFlag(7, true);
        this.faceTarget(target);
    }

    private void tickPigManeuver(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        if (this.isValidCombatTarget(target)) {
            Vec3 toTarget = target.getEyePosition().subtract(this.getEyePosition());
            float desiredYaw = (float) (Mth.atan2(-toTarget.x, toTarget.z) * (180.0D / Math.PI));
            float yaw = Mth.rotLerp(0.10F, this.getYRot(), desiredYaw);
            this.setYRot(yaw);
            this.yBodyRot = yaw;
            this.yHeadRot = yaw;
        }
        int duration = Math.max(1, ModConfigs.QUNYOU_PIG_MANEUVER_DURATION.get());
        float progress = Mth.clamp(this.getStateTicks(0.0F) / duration, 0.0F, 1.0F);
        double climbHeight = ModConfigs.QUNYOU_PIG_MANEUVER_CLIMB_HEIGHT.get();
        double verticalSpeed = (climbHeight / duration)
                * (1.2D + 0.8D * Math.sin(progress * Math.PI))
                * ModConfigs.QUNYOU_PIG_MANEUVER_VERTICAL_SPEED.get();
        Vec3 horizontal = new Vec3(-Math.sin(this.getYRot() * Mth.DEG_TO_RAD), 0.0D,
                Math.cos(this.getYRot() * Mth.DEG_TO_RAD));
        double horizontalSpeed = ModConfigs.QUNYOU_PIG_MANEUVER_HORIZONTAL_SPEED.get();
        this.setDeltaMovement(horizontal.x * horizontalSpeed, verticalSpeed, horizontal.z * horizontalSpeed);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.getStateTicks(0.0F) >= duration
                || this.getY() - this.pigManeuverStartY >= climbHeight
                || this.horizontalCollision || this.verticalCollision) {
            this.beginGliding(target);
        }
    }

    private boolean tryStartAirEating(LivingEntity target) {
        if (this.getState() != GLIDING || !this.isValidCombatTarget(target)
                || this.airEatingActive || this.pigMissileSequenceActive
                || this.airEatingCooldown > 0) {
            this.airEatingCheckTicker = 0;
            return false;
        }
        this.airEatingCheckTicker++;
        if (this.airEatingCheckTicker < ModConfigs.QUNYOU_AIR_EATING_CHECK_INTERVAL.get()) {
            return false;
        }
        this.airEatingCheckTicker = 0;
        if (this.random.nextDouble() >= ModConfigs.QUNYOU_AIR_EATING_CHANCE.get()) {
            return false;
        }
        this.beginAirEating();
        return this.airEatingActive;
    }

    private void beginAirEating() {
        if (this.getState() != GLIDING || this.airEatingActive) {
            return;
        }
        ItemStack goldenCarrot = new ItemStack(Items.GOLDEN_CARROT);
        this.setItemSlot(EquipmentSlot.MAINHAND, goldenCarrot);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.airEatingActive = true;
        this.airEatingRewardGranted = false;
        this.airEatingTicks = 0;
        this.airEatingCooldown = ModConfigs.QUNYOU_AIR_EATING_COOLDOWN.get();
        this.entityData.set(DATA_AIR_EATING, true);
    }

    private void tickAirEating(LivingEntity target) {
        if (!this.airEatingActive) {
            return;
        }
        if (this.getState() != GLIDING || !this.isAlive() || this.isRemoved()
                || !this.isValidCombatTarget(target)
                || !this.getMainHandItem().is(Items.GOLDEN_CARROT)) {
            this.interruptAirEating();
            return;
        }
        this.airEatingTicks++;
        if (this.airEatingTicks >= 7 && (this.airEatingTicks - 7) % 4 == 0) {
            this.playAirEatingEffects();
        }
        if (this.airEatingTicks >= ModConfigs.QUNYOU_AIR_EATING_DURATION.get()) {
            this.finishAirEatingSuccessfully();
        }
    }

    private void playAirEatingEffects() {
        ItemStack item = this.getMainHandItem();
        this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.HOSTILE,
                0.6F + 0.3F * this.random.nextFloat(), 0.95F + 0.1F * this.random.nextFloat());
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item),
                    this.getX(), this.getEyeY(), this.getZ(), 4, 0.10D, 0.10D, 0.10D, 0.02D);
        }
    }

    private void finishAirEatingSuccessfully() {
        if (!this.airEatingActive || this.airEatingRewardGranted
                || !this.isAlive() || this.isRemoved() || this.getState() != GLIDING) {
            return;
        }
        this.airEatingRewardGranted = true;
        this.clearAirEatingResources();
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,
                ModConfigs.QUNYOU_AIR_EATING_STRENGTH_DURATION.get(), 4, false, true, true));
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                ModConfigs.QUNYOU_AIR_EATING_REGENERATION_DURATION.get(), 2, false, true, true));
    }

    private void interruptAirEating() {
        if (!this.airEatingActive) {
            return;
        }
        this.airEatingRewardGranted = false;
        this.clearAirEatingResources();
    }

    private void clearAirEatingResources() {
        this.airEatingActive = false;
        this.airEatingTicks = 0;
        this.entityData.set(DATA_AIR_EATING, false);
        if (this.getMainHandItem().is(Items.GOLDEN_CARROT)) {
            this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /**
     * 清理失效登记，并按距离保留最多四只分身，兼容旧存档中曾登记过多只的情况。
     */
    private List<QunyouClone> resolveActivePigClones(ServerLevel serverLevel) {
        List<QunyouClone> loadedClones = new ArrayList<>();
        for (UUID cloneId : new ArrayList<>(this.activePigCloneIds)) {
            Entity entity = serverLevel.getEntity(cloneId);
            if (entity instanceof QunyouClone clone && clone.isAlive() && !clone.isRemoved()) {
                loadedClones.add(clone);
            } else {
                this.activePigCloneIds.remove(cloneId);
            }
        }
        loadedClones.sort((first, second) ->
                Double.compare(this.distanceToSqr(first), this.distanceToSqr(second)));
        while (loadedClones.size() > PIG_CLONE_ACTIVE_LIMIT) {
            QunyouClone excessClone = loadedClones.remove(loadedClones.size() - 1);
            this.activePigCloneIds.remove(excessClone.getUUID());
            excessClone.discard();
        }
        return loadedClones;
    }

    /** 将已有分身放到本体附近的安全位置，并立即同步本体当前目标。 */
    private boolean teleportPigCloneNearOwner(ServerLevel serverLevel, QunyouClone clone) {
        Vec3 originalPosition = clone.position();
        float originalYaw = clone.getYRot();
        float originalPitch = clone.getXRot();
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = Math.toRadians(this.getYRot() - 70.0D + attempt * 23.0D);
            double radius = 2.0D + 0.25D * (attempt / 4);
            double x = this.getX() + Math.cos(angle) * radius;
            double y = this.getY();
            double z = this.getZ() + Math.sin(angle) * radius;
            clone.moveTo(x, y, z, this.getYRot() + 180.0F, 0.0F);
            if (!serverLevel.noCollision(clone)) {
                continue;
            }
            clone.teleportTo(x, y, z);
            clone.setYRot(this.getYRot() + 180.0F);
            clone.setXRot(0.0F);
            clone.setDeltaMovement(Vec3.ZERO);
            clone.getNavigation().stop();
            LivingEntity currentTarget = this.getTarget();
            if (clone.isValidCombatTarget(currentTarget)) {
                clone.setTarget(currentTarget);
            }
            return true;
        }
        clone.moveTo(originalPosition.x, originalPosition.y, originalPosition.z, originalYaw, originalPitch);
        return false;
    }

    private void notifyCloneOwnerRemoved() {
        if (!this.isClone() || this.cloneOwnerUuid == null
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity owner = serverLevel.getEntity(this.cloneOwnerUuid);
        if (owner instanceof Qunyou qunyou) {
            qunyou.activePigCloneIds.remove(this.getUUID());
        }
    }

    /** 主群友真正死亡时，击杀当前世界中仍加载的所有登记分身。 */
    private void killAllPigClones() {
        if (this.isClone() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        for (UUID cloneId : new ArrayList<>(this.activePigCloneIds)) {
            Entity entity = serverLevel.getEntity(cloneId);
            if (entity instanceof QunyouClone clone && clone.isAlive() && !clone.isRemoved()) {
                clone.hurt(this.damageSources().genericKill(), Float.MAX_VALUE);
                if (!clone.isRemoved()) {
                    clone.discard();
                }
            }
        }
        this.activePigCloneIds.clear();
    }

    /** 仅为真正的猪逃离状态掷一次分身概率；猪撤退分支不会调用此方法。 */
    private void trySummonPigClones() {
        if (this.isClone() || !ModConfigs.QUNYOU_PIG_CLONE_ENABLED.get()
                || this.random.nextDouble() >= ModConfigs.QUNYOU_PIG_CLONE_CHANCE.get()
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        List<QunyouClone> existingClones = this.resolveActivePigClones(serverLevel);
        if (existingClones.size() >= PIG_CLONE_ACTIVE_LIMIT) {
            if (!this.teleportPigCloneNearOwner(serverLevel, existingClones.get(0))) {
                LOGGER.warn("Failed to teleport existing Qunyou clone near its owner due to unsafe positions");
            }
            return;
        }
        // 单次召唤只新增一只；达到四只上限后才改为传送已有分身。
        int count = Math.min(1, ModConfigs.QUNYOU_PIG_CLONE_COUNT.get());
        int phase = this.getCombatPhase();
        int spawned = 0;
        List<Vec3> usedPositions = new ArrayList<>();
        for (int cloneIndex = 0; cloneIndex < count; cloneIndex++) {
            boolean spawnedOne = false;
            for (int attempt = 0; attempt < 16 && !spawnedOne; attempt++) {
                QunyouClone clone = ModEntityTypes.QUNYOU_CLONE.get().create(serverLevel);
                if (clone == null) {
                    break;
                }
                double side = cloneIndex == 0 ? -1.0D : 1.0D;
                double angle = Math.toRadians(this.getYRot() + side * 70.0D + attempt * 23.0D);
                double radius = 2.0D + 0.25D * (attempt / 4);
                Vec3 candidate = new Vec3(
                        this.getX() + Math.cos(angle) * radius,
                        this.getY(),
                        this.getZ() + Math.sin(angle) * radius);
                if (usedPositions.stream().anyMatch(position -> position.distanceToSqr(candidate) < 1.0D)) {
                    continue;
                }
                clone.moveTo(candidate.x, candidate.y, candidate.z, this.getYRot() + 180.0F, 0.0F);
                clone.configureClonePhase(phase);
                clone.configureCloneOwner(this.getUUID());
                var healthAttribute = clone.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttribute != null) {
                    healthAttribute.setBaseValue(ModConfigs.QUNYOU_PIG_CLONE_MAX_HEALTH.get());
                }
                clone.setHealth(clone.getMaxHealth());
                if (!serverLevel.noCollision(clone)) {
                    continue;
                }
                if (!serverLevel.addFreshEntity(clone)) {
                    continue;
                }
                LivingEntity currentTarget = this.getTarget();
                if (clone.isValidCombatTarget(currentTarget)) {
                    clone.setTarget(currentTarget);
                }
                usedPositions.add(candidate);
                this.activePigCloneIds.add(clone.getUUID());
                spawned++;
                spawnedOne = true;
            }
        }
        if (spawned < count) {
            LOGGER.warn("Qunyou spawned only {}/{} pig clones due to unsafe positions", spawned, count);
        }
    }

    private void beginTakeoff(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        if (!this.isValidCombatTarget(target) || this.takeoffCooldown > 0) {
            this.setDoomStrikeActive(false);
            this.phase3AirTarget = null;
            this.beginPhase3GroundWait(target);
            return;
        }
        this.setDoomStrikeActive(this.getHealth() < DOOM_STRIKE_HEALTH_THRESHOLD);
        this.phase3AirTarget = target;
        this.takeoffFireworkStage = 0;
        this.takeoffFireworkBoostTicks = 0;
        this.takeoffVerticalSpeed = 0.0D;
        this.takeoffBurstTriggered = false;
        this.entityData.set(DATA_TAKEOFF_PROGRESS, 0.0F);
        this.takeoffCooldown = TAKEOFF_COOLDOWN;
        this.entityData.set(DATA_TAKEOFF_READY, false);
        this.clearPhase3ComboRuntime();
        this.enterState(TAKEOFF);
        // 普通起飞立即离地；毁灭打击先保持重力与地面接触完成蓄力。
        this.setNoGravity(!this.doomStrikeActive);
        this.setSharedFlag(7, false);
        this.takeoffStartY = this.getY();
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.doomStrikeActive) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                    SoundSource.HOSTILE, 1.0F, 0.9F);
        }
    }

    private void beginPhase3GroundWait(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        this.enterState(PHASE3_GROUND_WAIT);
        this.setNoGravity(false);
        this.setSharedFlag(7, false);
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        if (this.isValidCombatTarget(target)) {
            this.faceTarget(target);
        }
    }

    private void tickPhase3GroundWait(LivingEntity target) {
        this.getNavigation().stop();
        this.setSharedFlag(7, false);
        this.setNoGravity(false);
        this.setDeltaMovement(0.0D, Math.min(0.0D, this.getDeltaMovement().y), 0.0D);
        if (this.isValidCombatTarget(target)) {
            this.faceTarget(target);
            this.resumePhase3GroundDecision(target);
        }
    }

    private void tickTakeoff(LivingEntity currentTarget) {
        LivingEntity target = this.resolvePhase3AirTarget(currentTarget);
        this.getNavigation().stop();
        int ticks = (int) this.getStateTicks(0.0F);
        double takeoffHeight = this.doomStrikeActive ? DOOM_STRIKE_TAKEOFF_HEIGHT : TAKEOFF_HEIGHT;

        if (this.doomStrikeActive && !this.takeoffBurstTriggered
                && ticks < DOOM_STRIKE_TAKEOFF_CHARGE_TICKS) {
            this.setDeltaMovement(Vec3.ZERO);
            if (!this.isValidCombatTarget(target)) {
                this.beginPhase3GroundWait(null);
                return;
            }
            this.spawnDoomStrikeChargeDust(ticks);
            return;
        }
        if (this.doomStrikeActive && !this.takeoffBurstTriggered) {
            if (!this.isValidCombatTarget(target)) {
                this.beginPhase3GroundWait(null);
                return;
            }
            this.takeoffBurstTriggered = true;
            this.takeoffVerticalSpeed = DOOM_STRIKE_TAKEOFF_INITIAL_SPEED;
            this.setNoGravity(true);
            this.spawnDoomStrikeLaunchBurst();
        }

        this.setDeltaMovement(Vec3.ZERO);
        double ascent = Math.max(0.0D, this.getY() - this.takeoffStartY);
        double progress = Mth.clamp(ascent / takeoffHeight, 0.0D, 1.0D);
        double takeoffStep;
        if (this.doomStrikeActive) {
            if (progress < DOOM_STRIKE_TAKEOFF_DECELERATION_PROGRESS) {
                this.takeoffVerticalSpeed = Math.min(DOOM_STRIKE_TAKEOFF_MAX_SPEED,
                        this.takeoffVerticalSpeed + DOOM_STRIKE_TAKEOFF_ACCELERATION);
            } else {
                this.takeoffVerticalSpeed = Math.max(TAKEOFF_STEP,
                        this.takeoffVerticalSpeed - DOOM_STRIKE_TAKEOFF_DECELERATION);
            }
            int targetStage = Math.min(DOOM_STRIKE_FIREWORK_STAGE_COUNT,
                    (int) (progress * (DOOM_STRIKE_FIREWORK_STAGE_COUNT + 1)));
            while (this.takeoffFireworkStage < targetStage) {
                this.takeoffFireworkStage++;
                this.takeoffFireworkBoostTicks = DOOM_STRIKE_FIREWORK_BOOST_TICKS;
                this.spawnTakeoffFireworkBurst(this.takeoffFireworkStage);
            }
            takeoffStep = this.takeoffVerticalSpeed * (this.takeoffFireworkBoostTicks > 0
                    ? DOOM_STRIKE_FIREWORK_BOOST_MULTIPLIER : 1.0D);
        } else {
            takeoffStep = TAKEOFF_STEP;
        }

        this.move(MoverType.SELF, new Vec3(0.0D, takeoffStep, 0.0D));
        if (this.takeoffFireworkBoostTicks > 0) {
            this.takeoffFireworkBoostTicks--;
        }
        ascent = Math.max(0.0D, this.getY() - this.takeoffStartY);
        progress = Mth.clamp(ascent / takeoffHeight, 0.0D, 1.0D);
        if (ticks % 4 == 0 || progress >= 1.0D) {
            this.entityData.set(DATA_TAKEOFF_PROGRESS, (float) progress);
        }
        if (ticks % 4 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.FIREWORK, this.getX(), this.getY() + 0.4D, this.getZ(),
                    8, 0.15D, 0.15D, 0.15D, 0.08D);
        }
        boolean reachedHeight = ascent >= takeoffHeight;
        int maxDuration = this.doomStrikeActive ? DOOM_STRIKE_MAX_DURATION : TAKEOFF_MAX_DURATION;
        if (reachedHeight || ticks >= maxDuration || this.verticalCollision) {
            this.entityData.set(DATA_TAKEOFF_PROGRESS, 1.0F);
            if (this.isValidCombatTarget(target)) {
                this.beginDescendingAttack(target);
            } else {
                this.beginGliding(target);
            }
        }
    }

    /** 蓄力阶段只产生脚边少量扬尘，不提前播放烟花爆发。 */
    private void spawnDoomStrikeChargeDust(int ticks) {
        if (ticks % 2 != 0 || !this.onGround() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.sendParticles(ParticleTypes.CLOUD,
                this.getX(), this.getY() + 0.05D, this.getZ(), 3,
                0.22D, 0.03D, 0.22D, 0.01D);
    }

    /** 毁灭打击离地瞬间的集中烟花助推、烟尘和音效。 */
    private void spawnDoomStrikeLaunchBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 look = this.getLookAngle();
        Vec3 backward = new Vec3(-look.x, 0.0D, -look.z);
        if (backward.lengthSqr() < 1.0E-6D) {
            backward = new Vec3(0.0D, 0.0D, -1.0D);
        }
        backward = backward.normalize();
        double burstX = this.getX() + backward.x * 0.45D;
        double burstY = this.getY() + 0.25D;
        double burstZ = this.getZ() + backward.z * 0.45D;
        serverLevel.sendParticles(ParticleTypes.FIREWORK, burstX, burstY, burstZ,
                DOOM_STRIKE_TAKEOFF_BURST_PARTICLES, 0.32D, 0.12D, 0.32D, 0.20D);
        serverLevel.sendParticles(ParticleTypes.FLAME, burstX, burstY - 0.10D, burstZ,
                16, 0.20D, 0.06D, 0.20D, 0.035D);
        serverLevel.sendParticles(ParticleTypes.SMOKE, burstX, burstY - 0.08D, burstZ,
                14, 0.28D, 0.05D, 0.28D, 0.025D);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.HOSTILE, 1.1F, 0.85F);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    private void spawnTakeoffFireworkBurst(int stage) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        int particleCount = DOOM_STRIKE_FIREWORK_PARTICLES_PER_STAGE + stage * 8;
        serverLevel.sendParticles(ParticleTypes.FIREWORK,
                this.getX(), this.getY() + 0.45D, this.getZ(), particleCount,
                0.30D, 0.20D, 0.30D, 0.16D);
        serverLevel.sendParticles(ParticleTypes.FLAME,
                this.getX(), this.getY() + 0.40D, this.getZ(), 6 + stage * 2,
                0.18D, 0.12D, 0.18D, 0.03D);
        this.level().playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.HOSTILE, 1.0F, 0.82F + stage * 0.08F);
    }

    private void beginDescendingAttack(LivingEntity target) {
        if (!this.isAlive() || this.isRemoved()) {
            return;
        }
        if (!this.isValidCombatTarget(target)) {
            this.beginGliding(target);
            return;
        }
        this.enterState(DESCENDING_ATTACK);
        this.setNoGravity(true);
        this.setSharedFlag(7, false);
        this.descentDistance = 0.0D;
        this.descentTicks = 0;
        this.descendingImpactResolved = false;
        Vec3 direction = target.getEyePosition().subtract(this.getEyePosition());
        this.descendDirection = direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, -1.0D, 0.0D)
                : direction.normalize();
        this.faceDirection3D(this.descendDirection);
    }

    private void tickDescendingAttack(LivingEntity target) {
        target = this.resolvePhase3AirTarget(target);
        this.getNavigation().stop();
        this.descentTicks++;
        float turnLerp = this.doomStrikeActive ? DOOM_STRIKE_DESCEND_TURN_LERP : DESCEND_TURN_LERP;
        if (this.isValidCombatTarget(target)) {
            Vec3 desired = target.getEyePosition().subtract(this.getEyePosition());
            if (desired.lengthSqr() > 1.0E-6D) {
                desired = desired.normalize();
                this.descendDirection = this.descendDirection.scale(1.0D - turnLerp)
                        .add(desired.scale(turnLerp)).normalize();
            }
        }
        this.faceDirection3D(this.descendDirection);

        double speed = this.doomStrikeActive
                ? Math.min(DOOM_STRIKE_DESCEND_MAX_SPEED,
                        DOOM_STRIKE_DESCEND_INITIAL_SPEED
                                + DOOM_STRIKE_DESCEND_ACCELERATION * this.descentTicks)
                : Math.min(DESCEND_MAX_SPEED,
                        DESCEND_INITIAL_SPEED + DESCEND_ACCELERATION * this.descentTicks);
        Vec3 start = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
        Vec3 end = start.add(this.descendDirection.scale(speed));
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 moveEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
        Vec3 step = moveEnd.subtract(start);

        if (this.isValidCombatTarget(target)
                && target.getBoundingBox().inflate(0.35D).clip(start, moveEnd).isPresent()) {
            this.resolveDescendingImpact(target, true);
            return;
        }

        this.move(MoverType.SELF, step);
        this.descentDistance += step.length();
        if (this.doomStrikeActive) {
            this.spawnDescendingFlameTrail();
        }
        if (blockHit.getType() != HitResult.Type.MISS || this.onGround()
                || this.horizontalCollision || this.verticalCollision
                || this.descentTicks >= DESCEND_MAX_DURATION) {
            this.resolveDescendingImpact(target, false);
        }
    }

    private void spawnDescendingFlameTrail() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double trailX = this.getX() - this.descendDirection.x * 0.45D;
        double trailY = this.getY() + this.getBbHeight() * 0.5D - this.descendDirection.y * 0.45D;
        double trailZ = this.getZ() - this.descendDirection.z * 0.45D;
        serverLevel.sendParticles(ParticleTypes.FLAME, trailX, trailY, trailZ,
                4, 0.12D, 0.12D, 0.12D, 0.015D);
    }

    private void resolveDescendingImpact(LivingEntity target, boolean directHit) {
        if (this.descendingImpactResolved) {
            return;
        }
        this.descendingImpactResolved = true;
        float damageBonus = Math.min(DESCEND_MAX_DAMAGE_BONUS,
                (float) this.descentDistance * DESCEND_DAMAGE_PER_BLOCK);
        float mainDamage = DESCEND_BASE_DAMAGE + damageBonus;
        boolean targetHit = this.isValidCombatTarget(target)
                && (directHit || this.distanceToSqr(target) <= DESCEND_AOE_RADIUS * DESCEND_AOE_RADIUS);
        if (targetHit) {
            target.hurt(this.damageSources().mobAttack(this), mainDamage);
            Vec3 push = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
            target.push(push.x * DESCEND_KNOCKBACK, 0.45D, push.z * DESCEND_KNOCKBACK);
        }

        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(DESCEND_AOE_RADIUS),
                candidate -> candidate != target && this.isValidCombatTarget(candidate)
                        && this.isAllowedAttackTarget(candidate))) {
            if (this.distanceToSqr(victim) <= DESCEND_AOE_RADIUS * DESCEND_AOE_RADIUS) {
                victim.hurt(this.damageSources().mobAttack(this), mainDamage * DESCEND_AOE_MULTIPLIER);
                Vec3 push = victim.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).normalize();
                victim.push(push.x * DESCEND_KNOCKBACK, 0.45D, push.z * DESCEND_KNOCKBACK);
            }
        }

        var impactSound = this.descentDistance > 5.0D
                ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
        this.level().playSound(null, this.blockPosition(), impactSound, SoundSource.HOSTILE, 1.2F, 0.9F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5D, this.getZ(),
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.5D, this.getZ(),
                    36, DESCEND_AOE_RADIUS * 0.35D, 0.25D, DESCEND_AOE_RADIUS * 0.35D, 0.2D);
            if (this.doomStrikeActive) {
                serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
                        this.getX(), this.getY() + 0.2D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_LARGE,
                        this.getX(), this.getY() + 0.2D, this.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                serverLevel.sendParticles(ParticleTypes.GUST,
                        this.getX(), this.getY() + 0.35D, this.getZ(), 18,
                        DESCEND_AOE_RADIUS * 0.25D, 0.15D, DESCEND_AOE_RADIUS * 0.25D, 0.08D);
            }
        }
        if (this.doomStrikeActive) {
            this.level().playSound(null, this.blockPosition(), SoundEvents.WIND_CHARGE_BURST.value(),
                    SoundSource.HOSTILE, 1.2F, 0.9F);
        }
        this.fallDistance = 0.0F;
        this.setDeltaMovement(Vec3.ZERO);
        if (this.onGround()) {
            this.handlePhase3Landing(target);
        } else {
            this.beginGliding(target);
        }
    }

    private void faceDirection3D(Vec3 direction) {
        double horizontal = direction.horizontalDistance();
        float yaw = (float) (Mth.atan2(-direction.x, direction.z) * (180.0D / Math.PI));
        float pitch = (float) (Mth.atan2(-direction.y, Math.max(0.01D, horizontal)) * (180.0D / Math.PI));
        this.setYRot(yaw);
        this.setXRot(Mth.clamp(pitch, -85.0F, 85.0F));
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }
    private void tickInverted() {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (this.getStateTicks(0.0F) >= INVERTED_DURATION) {
            if (this.phase3Pending) {
                this.finishState(ACTION_COOLDOWN);
            } else if (this.random.nextFloat() < this.getSleepChance()) {
                this.enterState(SLEEPING);
            } else {
                this.enterState(FLIP);
            }
        }
    }

    private float getSleepChance() {
        float lostHealthProgress = Mth.clamp(
                (float) ((MAX_HEALTH - this.getHealth())
                        / (MAX_HEALTH - SLEEP_CHANCE_THRESHOLD_HEALTH)),
                0.0F, 1.0F);
        return Mth.lerp(lostHealthProgress, SLEEP_CHANCE_AT_FULL_HEALTH,
                SLEEP_CHANCE_AT_THRESHOLD_HEALTH);
    }

    /** 受到攻击时打断胡萝卜射击，尚未发射的胡萝卜掉落为物品。 */
    public void interruptCarrotSummonFromAttack() {
        if (this.level().isClientSide) {
            return;
        }
        boolean normalSummon = this.getState() == SUMMON;
        boolean defenseSummon = this.defenseSummonActive;
        if (!normalSummon && !defenseSummon) {
            return;
        }
        this.dropHoveringCarrots();
        this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
        if (normalSummon) {
            this.finishState(ACTION_COOLDOWN);
        } else {
            this.defenseSummonActive = false;
            this.defenseSummonTicks = 0;
            this.defenseSummonLaunched = 0;
            this.defenseSummonCount = 0;
        }
        this.level().playSound(null, this.blockPosition(), SoundEvents.ITEM_BREAK,
                SoundSource.HOSTILE, 0.8F, 1.15F);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CARROT)),
                    this.getX(), this.getY() + 1.0D, this.getZ(), 12,
                    0.35D, 0.30D, 0.35D, 0.04D);
        }
    }

    /** 受到攻击时立即结束睡眠，且不会在同一击后立刻转入闪避。 */
    public void interruptSleepFromAttack() {
        if (this.level().isClientSide || this.getState() != SLEEPING) {
            return;
        }
        this.dodgeCooldown = DODGE_COOLDOWN;
        this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
        this.removeEffect(MobEffects.REGENERATION);
        this.finishState(ACTION_COOLDOWN);
    }

    private void tickSleeping() {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (this.getStateTicks(0.0F) >= SLEEP_DURATION) {
            this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
        }
    }

    private void tickFlip() {
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        if (this.getStateTicks(0.0F) >= FLIP_DURATION) {
            this.specialCooldown = SPECIAL_DECISION_COOLDOWN;
            this.finishState(ACTION_COOLDOWN);
        }
    }

    private void enterState(byte state) {
        if (state != TAKEOFF && state != GLIDING && state != DESCENDING_ATTACK
                && state != PHASE3_EJECTION_CHARGE && state != PHASE3_EJECTION_RUSH
                && state != PHASE3_EJECTION_RECOVER && state != PHASE3_EJECTION_ASCEND
                && state != PHASE3_PIG_RETREAT_CHARGE && state != PHASE3_PIG_RETREAT_HORIZONTAL
                && state != PHASE3_PIG_RETREAT_ASCEND && state != PHASE3_AIR_MANEUVER) {
            this.phase3AirTarget = null;
        }
        if (this.getState() == GLIDING && state != GLIDING) {
            this.clearPigMissileSequence();
            this.interruptAirEating();
        }
        if (state != TAKEOFF && state != DESCENDING_ATTACK) {
            this.setDoomStrikeActive(false);
        }
        if (this.getState() == SUMMON && state != SUMMON) {
            this.releaseHoveringCarrots();
        }
        if (this.getState() == DEFENDING && state != DEFENDING) {
            this.cancelDefenseSummon(true);
            this.clearDefenseResources();
        }
        if (this.eatingActive && state != RETREAT && state != DEFENDING) {
            this.interruptEating();
        }
        this.entityData.set(DATA_PREVIOUS_STATE, this.getState());
        this.entityData.set(DATA_STATE, state);
        this.entityData.set(DATA_STATE_START, this.level().getGameTime());
        this.getNavigation().stop();
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.entityData.set(DATA_CHARGE_BOOST, false);
        this.chargeBoostTicksRemaining = 0;
        this.setNoGravity(state == CHARGE || state == INVERTED || state == SLEEPING || state == FLIP
                || state == SUMMON || state == ASCENDING || state == TAKEOFF || state == DESCENDING_ATTACK);
        if (state != GLIDING) {
            this.setSharedFlag(7, false);
        }
        if (state == MELEE) {
            this.meleeHit = false;
        }
        if (state == SLEEPING) {
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, SLEEP_DURATION,
                    SLEEP_REGENERATION_AMPLIFIER, false, true, true));
        }
    }

    private void finishState(int cooldown) {
        if (this.getState() == DEFENDING) {
            this.cancelDefenseSummon(true);
            this.clearDefenseResources();
        }
        this.interruptAirEating();
        this.releaseHoveringCarrots();
        this.setSharedFlag(7, false);
        this.entityData.set(DATA_CHARGE_BOOST, false);
        this.chargeBoostTicksRemaining = 0;
        this.entityData.set(DATA_PREVIOUS_STATE, this.getState());
        this.entityData.set(DATA_STATE, IDLE);
        this.entityData.set(DATA_STATE_START, this.level().getGameTime());
        this.setNoGravity(false);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.stateCooldown = cooldown;
    }

    private Vec3 hoverPosition(int index, int count, int tick) {
        double angle = (Math.PI * 2.0D * index) / count;
        double bob = Math.sin((tick + index * 7) * 0.15D) * 0.12D;
        return this.position().add(Math.cos(angle) * 1.25D, 1.05D + bob, Math.sin(angle) * 1.25D);
    }

    private void releaseHoveringCarrots() {
        for (QunyouCarrotProjectile carrot : this.summonedCarrots) {
            if (!carrot.isLaunched() && !carrot.isRemoved()) {
                carrot.discard();
            }
        }
        this.summonedCarrots.clear();
    }

    /** 将尚未发射的悬浮胡萝卜实体转换成普通胡萝卜掉落物。 */
    private void dropHoveringCarrots() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.releaseHoveringCarrots();
            return;
        }
        for (QunyouCarrotProjectile carrot : this.summonedCarrots) {
            if (!carrot.isLaunched() && !carrot.isRemoved()) {
                ItemEntity drop = new ItemEntity(serverLevel,
                        carrot.getX(), carrot.getY(), carrot.getZ(), new ItemStack(Items.CARROT));
                drop.setDefaultPickUpDelay();
                drop.setDeltaMovement(
                        (this.random.nextDouble() - 0.5D) * 0.16D,
                        0.16D + this.random.nextDouble() * 0.08D,
                        (this.random.nextDouble() - 0.5D) * 0.16D);
                serverLevel.addFreshEntity(drop);
                carrot.discard();
            }
        }
        this.summonedCarrots.clear();
    }

    private void clearAllSummons() {
        for (QunyouCarrotProjectile carrot : this.summonedCarrots) {
            if (!carrot.isRemoved()) {
                carrot.discard();
            }
        }
        this.summonedCarrots.clear();
        this.defenseSummonActive = false;
        this.defenseSummonTicks = 0;
        this.defenseSummonLaunched = 0;
        this.defenseSummonCount = 0;
    }

    /**
     * 第三阶段空中状态持续恢复锁定目标。目标选择 Goal 会按 32 格跟随范围清除目标，
     * 但起飞到 100 格、滑翔和降临期间仍应保持原战斗目标。
     */
    private LivingEntity resolvePhase3AirTarget(LivingEntity currentTarget) {
        LivingEntity target = this.isValidCombatTarget(this.phase3AirTarget) ? this.phase3AirTarget : currentTarget;
        if (this.isValidCombatTarget(target)) {
            this.phase3AirTarget = target;
            if (this.getTarget() != target) {
                this.setTarget(target);
            }
            return target;
        }
        this.phase3AirTarget = null;
        return null;
    }

    /**
     * 群友统一战斗目标过滤。优先使用原版 canAttack/canAttackType/队伍关系，
     * 再排除同类、特殊 NPC、已驯服或已被玩家拥有的生物与非战斗实体。
     */
    public boolean isValidCombatTarget(LivingEntity target) {
        if (target == null || target == this || target instanceof Qunyou
                || target.level() != this.level()
                || !target.isAlive() || target.isRemoved()
                || !target.canBeSeenAsEnemy()) {
            return false;
        }
        if (target instanceof Player player) {
            if (player.isSpectator() || player.getAbilities().instabuild) {
                return false;
            }
        } else {
            boolean naturallyHostile = target instanceof Enemy || target instanceof NeutralMob
                    || target instanceof Animal || target instanceof WaterAnimal
                    || target instanceof FlyingAnimal || target instanceof AmbientCreature;
            if (!naturallyHostile) {
                return false;
            }
        }
        if (target instanceof ArmorStand || target instanceof AbstractVillager || target instanceof IronGolem) {
            return false;
        }
        if (target instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        if (target instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) {
            return false;
        }
        if (target instanceof Mob mob && mob.isNoAi()) {
            return false;
        }
        return !this.isAlliedTo(target)
                && this.canAttack(target)
                && this.canAttackType(target.getType());
    }

    /**
     * 分身只能攻击本体当前同步目标；本体不受此限制。
     * 所有近战、范围伤害与弹射物命中判断都必须同时使用此规则。
     */
    public boolean isAllowedAttackTarget(LivingEntity target) {
        if (target == null) {
            return false;
        }
        if (!this.isClone()) {
            return true;
        }
        LivingEntity syncedTarget = this.getTarget();
        return syncedTarget != null && syncedTarget.getUUID().equals(target.getUUID());
    }

    private boolean validPlayer(LivingEntity target) {
        return target instanceof Player player
                && player.isAlive()
                && !player.isRemoved()
                && player.level() == this.level()
                && !player.isSpectator()
                && !player.getAbilities().instabuild;
    }

    /**
     * 闪避只允许在完全空闲时启动。状态恢复计时、进食覆盖动作和防御资源
     * 都算作忙碌，避免攻击被错误地当成闪避而取消。
     */
    public boolean canDodgeAttack(LivingEntity attacker) {
        return !this.level().isClientSide
                && !this.isNoAi()
                && this.isAlive()
                && !this.isDeadOrDying()
                && this.isValidCombatTarget(attacker)
                && this.isAllowedAttackTarget(attacker)
                && !this.thirdPhase
                && !this.phase3Pending
                && this.getState() == IDLE
                && this.stateCooldown == 0
                && !this.eatingActive
                && !this.isDefending()
                && this.dodgeCooldown == 0;
    }

    public boolean tryDodgeAttack(LivingEntity attacker) {
        if (!this.canDodgeAttack(attacker)) {
            return false;
        }
        this.nextDodgeRight = !this.nextDodgeRight;
        this.entityData.set(DATA_DODGE_RIGHT, this.nextDodgeRight);
        this.dodgeDirection = horizontalDirection(attacker);
        Vec3 right = new Vec3(-this.dodgeDirection.z, 0.0D, this.dodgeDirection.x);
        this.dodgeDirection = this.nextDodgeRight ? right : right.scale(-1.0D);
        this.dodgeCooldown = DODGE_COOLDOWN;
        this.enterState(DODGE);
        return this.getState() == DODGE;
    }

    private boolean tryDodgeAimedRangedWeapon() {
        if (this.thirdPhase || this.phase3Pending || this.getState() != IDLE) {
            return false;
        }
        for (Player player : this.level().getEntitiesOfClass(Player.class,
                this.getBoundingBox().inflate(32.0D),
                candidate -> this.validPlayer(candidate) && this.isAllowedAttackTarget(candidate))) {
            if (!this.isAimedRangedWeapon(player)) {
                continue;
            }
            // 远程瞄准会立即刷新闪避冷却；是否真正闪避仍由 canDodgeAttack
            // 检查当前状态，忙碌中的群友不会因此获得伤害免疫。
            this.dodgeCooldown = 0;
            if (this.tryDodgeAttack(player)) {
                return true;
            }
        }
        return false;
    }

    private boolean isAimedRangedWeapon(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack useItem = player.getUseItem();
        boolean rangedWeapon = mainHand.getItem() instanceof ProjectileWeaponItem
                || mainHand.getItem() instanceof TridentItem
                || useItem.getItem() instanceof ProjectileWeaponItem
                || useItem.getItem() instanceof TridentItem;
        if (!rangedWeapon || player.distanceToSqr(this) > RANGED_DODGE_RANGE_SQR) {
            return false;
        }
        Vec3 toTarget = this.getEyePosition().subtract(player.getEyePosition());
        if (toTarget.lengthSqr() < 1.0E-6D) {
            return true;
        }
        return player.getViewVector(1.0F).dot(toTarget.normalize()) >= RANGED_AIM_DOT_THRESHOLD
                && player.hasLineOfSight(this);
    }

    private Vec3 horizontalDirection(LivingEntity target) {
        Vec3 delta = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
        return delta.lengthSqr() < 1.0E-6D ? this.getLookAngle().multiply(1.0D, 0.0D, 1.0D) : delta.normalize();
    }

    private void faceTarget(LivingEntity target) {
        if (target == null) {
            return;
        }
        Vec3 delta = target.position().subtract(this.position());
        float yaw = (float) (Mth.atan2(-delta.x, delta.z) * (180.0D / Math.PI));
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        this.getLookControl().setLookAt(target, 45.0F, 45.0F);
    }

    private void faceDirection(Vec3 direction) {
        if (direction.lengthSqr() < 1.0E-6D) {
            return;
        }
        float yaw = (float) (Mth.atan2(-direction.x, direction.z) * (180.0D / Math.PI));
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void updateBossBar() {
        if (!this.isClone()) {
            this.bossEvent.setProgress(Mth.clamp(this.getHealth() / this.getMaxHealth(), 0.0F, 1.0F));
        }
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {

        float healthBefore = this.getHealth();
        float absorptionBefore = this.getAbsorptionAmount();
        boolean damaged = super.hurt(source, amount);
        boolean actualDamage = this.getHealth() < healthBefore
                || this.getAbsorptionAmount() < absorptionBefore;
        if (damaged && actualDamage && this.isAlive() && !this.isRemoved()) {
            if (this.eatingActive) {
                this.interruptEating();
            }
            if (this.airEatingActive) {
                this.interruptAirEating();
            }
            if (this.onGround() && this.isAlive() && !this.isRemoved()
                    && this.isValidCombatTarget(this.getTarget())
                    && this.random.nextDouble() < ModConfigs.QUNYOU_ESCAPE_ON_HURT_CHANCE.get()) {
                this.beginEscape(this.getTarget());
            }
        }
        return damaged;
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (!this.isClone() && this.isAlive() && !this.isRemoved()) {
            this.bossEvent.setName(this.getDisplayName());
            this.updateBossBar();
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (!this.isClone()) {
            this.bossEvent.removePlayer(player);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && !this.isClone() && !this.undyingTotemUsed
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.undyingTotemUsed = true;
            this.setHealth(1.0F);
            this.removeEffectsCuredBy(EffectCures.PROTECTED_BY_TOTEM);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                    TOTEM_REGENERATION_DURATION, 1));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    TOTEM_ABSORPTION_DURATION, 1));
            this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,
                    TOTEM_FIRE_RESISTANCE_DURATION, 0));
            this.updateBossBar();
            this.level().broadcastEntityEvent(this, (byte) 35);
            return;
        }
        this.notifyCloneOwnerRemoved();
        if (!this.isClone()) {
            this.killAllPigClones();
        }
        this.phase3AirTarget = null;
        this.setDoomStrikeActive(false);
        this.pigEjectionChainHits = 0;
        this.pigEjectionChainTarget = null;
        this.clearPigMissileSequence();
        this.clearPhase3ComboRuntime();
        this.clearPhase3Equipment();
        this.clearDefenseResources();
        this.clearEatingResources();
        this.clearAirEatingResources();
        this.clearAllSummons();
        if (!this.isClone()) {
            this.bossEvent.setProgress(0.0F);
            this.bossEvent.removeAllPlayers();
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.notifyCloneOwnerRemoved();
        this.phase3AirTarget = null;
        this.setDoomStrikeActive(false);
        this.pigEjectionChainHits = 0;
        this.pigEjectionChainTarget = null;
        this.clearPigMissileSequence();
        this.clearPhase3ComboRuntime();
        this.clearPhase3Equipment();
        this.clearDefenseResources();
        this.clearEatingResources();
        this.clearAirEatingResources();
        this.clearAllSummons();
        if (!this.isClone()) {
            this.bossEvent.removeAllPlayers();
        }
        super.remove(reason);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("SecondPhase", this.secondPhase);
        tag.putBoolean("ThirdPhase", this.thirdPhase);
        tag.putBoolean("Phase3Pending", this.phase3Pending);
        tag.putBoolean("UndyingTotemUsed", this.undyingTotemUsed);
        tag.putInt("TakeoffCooldown", this.takeoffCooldown);
        tag.putInt("Phase3ComboCooldown", this.phase3ComboCooldown);
        tag.putInt("PigEjectionCooldown", this.pigEjectionCooldown);
        tag.putInt("PigMissileCooldown", this.pigMissileCooldown);
        tag.putInt("PigManeuverCooldown", this.pigManeuverCooldown);
        tag.putInt("AirEatingCooldown", this.airEatingCooldown);
        if (this.cloneOwnerUuid != null) {
            tag.putString("CloneOwner", this.cloneOwnerUuid.toString());
        }
        ListTag cloneIds = new ListTag();
        for (UUID cloneId : this.activePigCloneIds) {
            cloneIds.add(StringTag.valueOf(cloneId.toString()));
        }
        tag.put("PigCloneIds", cloneIds);
        // In-progress special states and summoned projectiles are intentionally not persisted.
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.clearPhase3Equipment();
        this.clearDefenseResources();
        this.clearEatingResources();
        this.clearAirEatingResources();
        this.eatingRewardGranted = false;
        this.airEatingRewardGranted = false;
        this.clearAllSummons();
        this.secondPhase = tag.getBoolean("SecondPhase");
        this.thirdPhase = tag.getBoolean("ThirdPhase");
        this.phase3Pending = this.thirdPhase || tag.getBoolean("Phase3Pending");
        this.undyingTotemUsed = tag.getBoolean("UndyingTotemUsed");
        this.phase3AirTarget = null;
        this.setDoomStrikeActive(false);
        this.pigEjectionChainHits = 0;
        this.pigEjectionChainTarget = null;
        this.clearPigMissileSequence();
        this.clearPhase3ComboRuntime();
        this.takeoffCooldown = Math.max(0, tag.getInt("TakeoffCooldown"));
        this.phase3ComboCooldown = Math.max(0, tag.getInt("Phase3ComboCooldown"));
        this.pigEjectionCooldown = Math.max(0, tag.getInt("PigEjectionCooldown"));
        this.pigMissileCooldown = Math.max(0, tag.getInt("PigMissileCooldown"));
        this.pigManeuverCooldown = Math.max(0, tag.getInt("PigManeuverCooldown"));
        this.airEatingCooldown = Math.max(0, tag.getInt("AirEatingCooldown"));
        this.cloneOwnerUuid = null;
        if (tag.contains("CloneOwner")) {
            try {
                this.cloneOwnerUuid = UUID.fromString(tag.getString("CloneOwner"));
            } catch (IllegalArgumentException ignored) {
                this.cloneOwnerUuid = null;
            }
        }
        this.activePigCloneIds.clear();
        ListTag cloneIds = tag.getList("PigCloneIds", Tag.TAG_STRING);
        for (int i = 0; i < cloneIds.size(); i++) {
            try {
                this.activePigCloneIds.add(UUID.fromString(cloneIds.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // Ignore malformed UUID entries from external NBT edits.
            }
        }
        this.pigManeuverCheckTicker = 0;
        this.airEatingCheckTicker = 0;
        this.entityData.set(DATA_TAKEOFF_READY, this.takeoffCooldown == 0);
        this.entityData.set(DATA_SECOND_PHASE, this.secondPhase);
        this.entityData.set(DATA_THIRD_PHASE, this.thirdPhase);
        this.entityData.set(DATA_PHASE_START, 0L);
        this.entityData.set(DATA_CHARGE_BOOST, false);
        this.entityData.set(DATA_PHASE3_COMBO_STRIKE, (byte) 0);
        this.entityData.set(DATA_SHIELD_BREAK_START, 0L);
        this.entityData.set(DATA_PREVIOUS_STATE, IDLE);
        this.entityData.set(DATA_STATE, IDLE);
        this.entityData.set(DATA_STATE_START, this.level().getGameTime());
        this.setNoGravity(false);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.setTarget(null);
    }
}
