package com.minechai.create_sandustry.content.sandmastersword;

import com.minechai.create_sandustry.registry.ModEntityTypes;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordSandification;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.common.Tags;
import org.joml.Vector3f;

/**
 * 沙噬剑剑气：服务端权威的直线弹射实体。
 *
 * <p><b>移动与碰撞</b>：每 tick 沿发射时的方向前进 {@value #SPEED} 格，并对“本 tick 的整段位移”做一次方块射线
 * 与一次实体射线（{@link ProjectileUtil#getEntityHitResult}）。也就是说位移虽然是按 tick 推进的，但碰撞检测覆盖
 * 了整个扫过的线段，不存在“传送跳过目标”的问题。命中方块/实体后立即 {@link #discard()}，不会穿墙、不会贯穿。</p>
 *
 * <p><b>射程</b>：{@value #MAX_DISTANCE} 格，按每 tick 的实际位移累加（{@link #travelled}），而不是出生点到当前点
 * 的直线距离；最后一段位移会被截断到剩余射程内。另有一层 {@value #MAX_LIFETIME_TICKS} tick 的兜底寿命，
 * 配合实体类型上的 {@code noSave()}，不会在区块卸载/重载后残留。</p>
 *
 * <p><b>伤害与沙化</b>：命中有效 {@link LivingEntity} 固定造成 {@value #DAMAGE} 点伤害，伤害来源为专用伤害类型
 * {@code createsandustry:sand_master_sword_beam}（伤害归属发射玩家，且该类型在
 * {@code #minecraft:no_knockback} 标签里 ⇒ 不产生击退）。随后调用与近战共用的
 * {@link SandMasterSwordSandification}：根据发射玩家完整沙心数施加对应等级沙化，并在目标剩余
 * 沙化时长上追加 60 tick；普通目标与 boss 使用同一规则。</p>
 *
 * <p><b>附魔联动</b>：发射时读取主手沙噬剑的附魔等级并同步到实体上——</p>
 * <ul>
 *     <li><b>横扫之刃</b>：每级让剑气视觉直径与群伤扫掠半径增大 {@value #SWEEPING_SIZE_PER_LEVEL}（20%）；</li>
 *     <li>有横扫之刃时剑气变为<b>群伤</b>：沿途每个实体各结算一次（同一实体只结算一次），剑气继续飞行；</li>
 *     <li><b>火焰附加</b>：拖尾附带原版火焰粒子，命中时按原版规则点燃目标（每级 4 秒）；</li>
 *     <li><b>击退</b>：每级按原版近战规则把目标沿剑气飞行方向击退（无附魔时仍然完全无击退）。</li>
 *     <li><b>锋利</b>：命中敌对生物或撞到障碍物时生成两道半月形剑气；两道剑气独立搜索目标并追踪，
 *         没有目标时不生成。横扫之刃穿透时在首次命中敌对生物处立即生成一对，后续命中不会重复生成。</li>
 * </ul>
 *
 * <p><b>客户端</b>：客户端只负责渲染（见 {@code SandMasterSwordBeamRenderer}），旋转与尺寸由渲染器根据同步来的
 * 速度、附魔等级与 tick 计算，服务器不参与渲染。</p>
 */
public class SandMasterSwordBeam extends Projectile {
    /** 专用伤害类型（伤害归属发射玩家、无击退）。 */
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand_master_sword_beam"));
    /** 飞行速度：2 格/tick。 */
    public static final double SPEED = 2.0D;
    /** 最大累计飞行距离：100 格。 */
    public static final double MAX_DISTANCE = 100.0D;
    /** 命中伤害：2 点（1 颗心）。 */
    public static final float DAMAGE = 2.0F;
    /** 兜底寿命，避免任何情况下残留实体。 */
    private static final int MAX_LIFETIME_TICKS = 200;
    /** 沙色拖尾粒子颜色（0~1）。 */
    private static final Vector3f SAND_COLOR = new Vector3f(0.85F, 0.74F, 0.50F);
    /** 横扫之刃每级让剑气变大 20%（同时扩大群伤扫掠半径）。 */
    public static final double SWEEPING_SIZE_PER_LEVEL = 0.2D;
    /** 火焰附加每级点燃 4 秒（与原版一致）。 */
    public static final float FIRE_ASPECT_SECONDS_PER_LEVEL = 4.0F;
    /** 击退附魔每级提供的击退强度（原版近战为 f4 * 0.5，f4 每级 +1 ⇒ 每级 0.5）。 */
    public static final double KNOCKBACK_STRENGTH_PER_LEVEL = 0.5D;
    /** 横扫之刃等级（0 = 无）：决定剑气大小与是否群伤。 */
    private static final EntityDataAccessor<Integer> DATA_SWEEPING_LEVEL =
            SynchedEntityData.defineId(SandMasterSwordBeam.class, EntityDataSerializers.INT);
    /** 火焰附加等级（0 = 无）：决定火焰粒子与命中点燃。 */
    private static final EntityDataAccessor<Integer> DATA_FIRE_ASPECT_LEVEL =
            SynchedEntityData.defineId(SandMasterSwordBeam.class, EntityDataSerializers.INT);

    /** 已累计飞行距离。 */
    private double travelled;
    /** 发射时确定的飞行方向（单位向量）；每 tick 重新写入速度，保证不会被爆炸等外力吹偏。 */
    private Vec3 flightDirection = Vec3.ZERO;
    /** 群伤模式下已经结算过的实体 id，保证同一个实体在一次飞行中只吃一次伤害。 */
    private final Set<Integer> damagedEntityIds = new HashSet<>();
    /** 击退附魔等级（0 = 无）：只在服务端命中结算时使用，客户端无需同步。 */
    private int knockbackLevel;
    /** 锋利等级（0 = 无）：只在服务端决定是否生成两道半月形剑气。 */
    private int sharpnessLevel;
    /** 最近一次命中的生物脚底位置；无横扫之刃时用于在敌对生物命中点生成半月剑气。 */
    private Vec3 lastHitEntityPos;
    /** 最近一次命中的敌怪脚底位置；横扫模式首次命中敌怪时立即用于生成半月剑气。 */
    private Vec3 lastHitEnemyPos;
    /** 最近一次命中的生物是否为敌对目标；普通生物不会触发生成半月剑气。 */
    private boolean lastHitWasHostile;
    /** 本次剑气是否已经生成过半月形剑气，避免重复触发。 */
    private boolean crescentsSpawned;

    public SandMasterSwordBeam(EntityType<? extends SandMasterSwordBeam> entityType, Level level) {
        super(entityType, level);
        // 自行处理碰撞（方块用射线、实体用扫掠 AABB），实体本身不参与物理推挤。
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    /**
     * 由发射者在服务端创建：从<b>主手持剑手附近</b>出射，沿<b>瞄准方向</b>直线飞行。
     *
     * <p>出生点 = 身体中心 + 水平前方 0.6 格 + 主手一侧 0.35 格 + 约胸口高度 1.0 格，再沿瞄准方向前移 0.5 格。
     * 这样第一/第三人称下都像是从剑上飞出，同时出生点已位于玩家碰撞箱之外（左右偏移超过半宽 0.3），
     * 不会生成在身体内部；配合实体自身的“发射者/重叠实体”过滤，不会立即命中自己。</p>
     *
     * <p>方向严格取服务端视角（{@link ServerPlayer#getLookAngle()}，由客户端同步、服务端可用），
     * 客户端不会上传任何坐标或方向，因此无法伪造出生点。</p>
     */
    public static SandMasterSwordBeam fire(ServerPlayer shooter, int sweepingLevel, int fireAspectLevel,
            int knockbackLevel, int sharpnessLevel) {
        SandMasterSwordBeam beam = new SandMasterSwordBeam(ModEntityTypes.SAND_MASTER_SWORD_BEAM.get(),
                shooter.serverLevel());

        Vec3 aim = shooter.getLookAngle();
        float yawRad = shooter.getYRot() * Mth.DEG_TO_RAD;
        // 水平前方与水平右方（yaw 0 面向 +Z，右侧为 -X）。
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0D, Mth.cos(yawRad));
        Vec3 right = new Vec3(-Mth.cos(yawRad), 0.0D, -Mth.sin(yawRad));
        // 主手侧：默认右手；玩家在选项里改为左手时跟随。
        boolean leftHanded = shooter.clientInformation().mainHand() == HumanoidArm.LEFT;
        double sideOffset = leftHanded ? -0.35D : 0.35D;

        Vec3 handPosition = shooter.position()
                .add(forward.scale(0.6D))
                .add(right.scale(sideOffset))
                .add(0.0D, 1.0D, 0.0D);
        Vec3 start = handPosition.add(aim.scale(0.5D));

        beam.setOwner(shooter);
        beam.setPos(start.x, start.y, start.z);
        beam.setDeltaMovement(aim.scale(SPEED));
        // 附魔等级随实体一起同步给客户端（渲染器按横扫之刃等级放大）。
        beam.entityData.set(DATA_SWEEPING_LEVEL, Math.max(0, sweepingLevel));
        beam.entityData.set(DATA_FIRE_ASPECT_LEVEL, Math.max(0, fireAspectLevel));
        beam.knockbackLevel = Math.max(0, knockbackLevel);
        beam.sharpnessLevel = Math.max(0, sharpnessLevel);
        return beam;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SWEEPING_LEVEL, 0);
        builder.define(DATA_FIRE_ASPECT_LEVEL, 0);
    }

    /** 横扫之刃等级（0 = 无）。 */
    public int getSweepingLevel() {
        return this.entityData.get(DATA_SWEEPING_LEVEL);
    }

    /** 火焰附加等级（0 = 无）。 */
    public int getFireAspectLevel() {
        return this.entityData.get(DATA_FIRE_ASPECT_LEVEL);
    }

    /** 剑气大小倍率：无附魔为 1.0，横扫之刃每级 +20%。 */
    public double getSizeScale() {
        return 1.0D + SWEEPING_SIZE_PER_LEVEL * this.getSweepingLevel();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.tickCount > MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }
        this.serverTick();
    }

    private void serverTick() {
        if (this.flightDirection == Vec3.ZERO) {
            Vec3 initial = this.getDeltaMovement();
            if (initial.lengthSqr() < 1.0E-8D) {
                this.discard();
                return;
            }
            this.flightDirection = initial.normalize();
        }
        // 锁定发射时的方向与速度：末影水晶爆炸会把范围内所有实体的速度直接改写，这里每 tick 复位一次。
        this.setDeltaMovement(this.flightDirection.scale(SPEED));

        Vec3 start = this.position();
        Vec3 motion = this.getDeltaMovement();
        double remaining = MAX_DISTANCE - this.travelled;
        if (remaining <= 0.0D) {
            this.discard();
            return;
        }

        Vec3 target = start.add(motion);
        if (start.distanceTo(target) > remaining) {
            target = start.add(motion.normalize().scale(remaining));
        }

        // 1) 方块碰撞：整个 tick 的位移做一次射线（不会穿墙）。
        BlockHitResult blockHit = this.level().clip(new ClipContext(start, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        boolean hitBlock = blockHit.getType() != HitResult.Type.MISS;
        Vec3 end = hitBlock ? blockHit.getLocation() : target;

        // 2) 剑气扫过的低矮植被与火焰：破坏植物（正常掉落）/熄灭火焰后继续飞行。
        //    注意：只处理“没有碰撞体积”的方块，因此石头、泥土、树叶、杜鹃等依旧会由下面的固体射线拦下。
        this.cutPassableTargets(start, end);

        // 3) 末影水晶：走原版 EndCrystal#hurt（保留其爆炸机制），剑气继续飞行。
        this.destroyCrystals(start, end);

        // 4) 实体命中：没有横扫之刃时保留原来的“最近目标命中后消失”；
        //    有横扫之刃时改为群伤：整段扫掠体积内的实体各结算一次，剑气继续飞行。
        EntityHitResult entityHit = null;
        if (this.getSweepingLevel() > 0) {
            this.damageSweptEntities(start, end);
        } else {
            entityHit = ProjectileUtil.getEntityHitResult(this.level(), this, start, end,
                    this.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D), this::canHitEntity);
            if (entityHit != null) {
                end = entityHit.getLocation();
            }
        }

        this.travelled += start.distanceTo(end);
        this.setPos(end.x, end.y, end.z);

        if (entityHit != null) {
            this.hitEntity(entityHit.getEntity());
            if (this.lastHitWasHostile) {
                this.trySpawnCrescents(end, true);
            }
            this.discard();
            return;
        }
        if (hitBlock) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.SAND_BREAK,
                    SoundSource.PLAYERS, 0.5F, 1.4F);
            this.spawnTrail(4);
            this.trySpawnCrescents(end, true);
            this.discard();
            return;
        }

        this.spawnTrail(2);
        if (this.travelled >= MAX_DISTANCE) {
            this.discard();
        }
    }

    /**
     * 命中一个实体：固定 2 点伤害 + 与近战共用的沙化规则 + 火焰附加，然后由调用方决定是否终止剑气。
     */
    private void hitEntity(Entity hit) {
        this.lastHitWasHostile = false;
        Entity target = hit;
        if (target instanceof PartEntity<?> part) {
            target = part.getParent();
        }
        if (!(target instanceof LivingEntity living)) {
            return;
        }

        this.lastHitEntityPos = target.position();
        this.lastHitWasHostile = living instanceof Enemy || living.getType().is(Tags.EntityTypes.BOSSES);
        if (this.lastHitWasHostile) {
            this.lastHitEnemyPos = target.position();
        }

        Entity owner = this.getOwner();
        // 注意 DamageSource 构造顺序是 (直接来源, 归属来源)：剑气是直接来源，玩家是归属来源，
        // 这样死亡播报/击杀统计才会算在发射者头上。
        DamageSource source = this.damageSources().source(DAMAGE_TYPE, this, owner);
        living.hurt(source, DAMAGE);
        // 与近战沙噬剑共用同一套沙化规则；owner 是服务端记录的实际发射玩家。
        SandMasterSwordSandification.apply(living, owner);
        if (this.getFireAspectLevel() > 0) {
            // 与原版火焰附加一致：每级 4 秒。
            living.igniteForSeconds(FIRE_ASPECT_SECONDS_PER_LEVEL * this.getFireAspectLevel());
        }
        if (this.knockbackLevel > 0) {
            // 与原版近战击退附魔一致：每级 +1 强度，再像近战那样乘 0.5 交给 knockback()。
            // 传入“指向发射者”的水平方向，knockback 内部会减去它，因此目标被沿剑气飞行方向推走。
            Vec3 direction = this.getDeltaMovement();
            double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            if (horizontal > 1.0E-4D) {
                living.knockback(KNOCKBACK_STRENGTH_PER_LEVEL * this.knockbackLevel,
                        -direction.x / horizontal, -direction.z / horizontal);
            }
        }

        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 0.8F, 1.5F);
        this.spawnTrail(6);
    }

    /**
     * 锋利原始剑气命中敌对生物或障碍物时生成两道半月形剑气。
     *
     * <p>没有横扫之刃时，敌对实体命中点或方块碰撞点可以作为生成位置；普通生物不会触发生成。有横扫之刃穿透时，
     * 第一次命中敌对生物就在 {@link #lastHitEnemyPos} 处立即生成一对，随后 {@link #crescentsSpawned} 会阻止后续命中重复生成；
     * 若全程没有命中敌怪，则仅允许在撞到障碍物时使用碰撞点生成。</p>
     */
    private void trySpawnCrescents(Vec3 fallbackPosition, boolean allowFallbackPosition) {
        if (this.sharpnessLevel <= 0 || this.crescentsSpawned) {
            return;
        }
        Vec3 position = this.getSweepingLevel() > 0 ? this.lastHitEnemyPos : this.lastHitEntityPos;
        if (position == null) {
            if (!allowFallbackPosition) {
                return;
            }
            position = fallbackPosition;
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            // 先置位再生成：即使当前调用链出现异常分支，也绝不允许同一次原始剑气生成第二对。
            this.crescentsSpawned = true;
            SandBladeCrescent.spawnPair(serverLevel, position, this.getOwner(), this.getDeltaMovement());
        }
    }

    /**
     * 横扫之刃：剑气变成群伤——对整段扫掠体积内的所有实体各结算一次（同一实体在一次飞行中只结算一次），
     * 剑气本身继续飞行（仍会被方块与 100 格射程拦下）。扫掠半径随剑气大小（横扫之刃等级）增大。
     */
    private void damageSweptEntities(Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double radius = 0.45D * this.getSizeScale();
        AABB sweep = this.getBoundingBox().expandTowards(end.subtract(start)).inflate(radius);
        for (Entity entity : serverLevel.getEntities(this, sweep, this::canHitEntity)) {
            if (this.damagedEntityIds.add(entity.getId())) {
                this.hitEntity(entity);
                if (this.lastHitWasHostile) {
                    // 横扫穿透继续飞行，但首次命中敌怪时立即生成；crescentsSpawned 防止后续敌怪重复生成。
                    this.trySpawnCrescents(this.lastHitEnemyPos, false);
                }
            }
        }
    }

    /**
     * 削断剑气路径上的低矮植被、熄灭火焰，然后让剑气继续飞行。
     *
     * <p>用剑气自身 0.9 格的碰撞箱沿路径每 0.25 格做一次扫掠采样，因此不会漏掉高速移动路径上的方块；
     * 只处理<b>碰撞体积为空</b>的目标，所以石头、泥土、建筑方块（以及树叶、杜鹃等有碰撞的植物）仍然由
     * {@code level.clip} 的固体射线拦下，剑气不会因为它们可被破坏就无条件穿透。</p>
     *
     * <p>植物走原版 {@link net.minecraft.world.level.Level#destroyBlock(BlockPos, boolean, Entity, int)}
     * （掉落与归属交给原版，工具为空，因此不会带时运），作物按自身方块状态与战利品表正常结算；
     * 普通火与灵魂火直接移除，不产生掉落。</p>
     */
    private void cutPassableTargets(Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 offset = end.subtract(start);
        double length = offset.length();
        if (length < 1.0E-4D) {
            return;
        }
        Entity owner = this.getOwner();
        int steps = Math.max(1, Mth.ceil(length / 0.25D));
        Set<BlockPos> visited = new HashSet<>();
        // 注意：实体的 AABB 以脚底为下界（y..y+height），并不以中心对称；
        // 这里用一个以剑气中心为原点的 0.9³ 立方体做扫掠截面，才能同时覆盖上下相邻的草/花/作物与火焰。
        double half = 0.45D * this.getSizeScale();
        AABB beamBox = new AABB(-half, -half, -half, half, half, half);

        for (int i = 0; i <= steps; i++) {
            Vec3 point = start.add(offset.scale((double) i / (double) steps));
            AABB swept = beamBox.move(point);
            BlockPos min = BlockPos.containing(swept.minX, swept.minY, swept.minZ);
            BlockPos max = BlockPos.containing(swept.maxX, swept.maxY, swept.maxZ);
            for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
                BlockPos immutable = pos.immutable();
                if (!visited.add(immutable)) {
                    continue;
                }
                BlockState state = serverLevel.getBlockState(immutable);
                if (state.isAir() || !state.getCollisionShape(serverLevel, immutable).isEmpty()) {
                    continue;   // 有碰撞的方块一律不在此处理，交给固体射线
                }
                if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                    serverLevel.removeBlock(immutable, false);   // 熄灭，无掉落
                    continue;
                }
                if (isCuttablePlant(state)) {
                    serverLevel.destroyBlock(immutable, true, owner, 512);   // 正常破坏 + 掉落归属
                }
            }
        }
    }

    /** 剑气可破坏的植物：原版作物/花/下界疣，以及“树木可替换”的低矮草/蕨类（兼容把草类塞进该标签的模组）。 */
    private static boolean isCuttablePlant(BlockState state) {
        if (state.is(BlockTags.CROPS) || state.is(BlockTags.FLOWERS) || state.is(Blocks.NETHER_WART)) {
            return true;
        }
        return state.is(BlockTags.REPLACEABLE_BY_TREES) && state.getBlock() instanceof BushBlock;
    }

    /**
     * 摧毁路径上的末影水晶：调用水晶自己的 {@code hurt}（与玩家/箭矢一致：移除并触发原版爆炸），
     * 之后剑气继续飞行，不会被水晶终止。
     */
    private void destroyCrystals(Vec3 start, Vec3 end) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        AABB search = this.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        Entity owner = this.getOwner();
        for (EndCrystal crystal : serverLevel.getEntitiesOfClass(EndCrystal.class, search)) {
            if (crystal.isRemoved() || crystal.getBoundingBox().inflate(0.3D).clip(start, end).isEmpty()) {
                continue;
            }
            crystal.hurt(this.damageSources().source(DAMAGE_TYPE, this, owner), 1.0F);
        }
    }
    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = this.getOwner();
        if (target == owner || target == this) {
            return false;
        }
        // 末影水晶由 destroyCrystals 单独处理（摧毁后剑气继续飞行），不参与普通实体命中。
        if (target instanceof EndCrystal) {
            return false;
        }
        // 刚发射的两 tick 内忽略与发射者重叠的实体，避免贴脸瞬间命中。
        if (this.tickCount <= 2 && owner != null
                && target.getBoundingBox().intersects(owner.getBoundingBox().inflate(0.5D))) {
            return false;
        }
        return super.canHitEntity(target);
    }

    /** 沙色拖尾粒子（原版 dust 粒子，无自定义粒子类型）。 */
    private void spawnTrail(int count) {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new DustParticleOptions(SAND_COLOR, 1.0F), this.getX(), this.getY(), this.getZ(),
                    count, 0.08D, 0.08D, 0.08D, 0.01D);
            if (this.getFireAspectLevel() > 0) {
                // 火焰附加：拖尾附带原版小火焰粒子（服务端统一发送，客户端不会重复生成）。
                serverLevel.sendParticles(ParticleTypes.SMALL_FLAME, this.getX(), this.getY(), this.getZ(),
                        Math.max(1, count / 2), 0.1D, 0.1D, 0.1D, 0.01D);
            }
        }
    }
}
