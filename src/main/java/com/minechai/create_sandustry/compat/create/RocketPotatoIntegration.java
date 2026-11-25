package com.minechai.create_sandustry.compat.create;

import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileBlockHitAction;
import com.simibubi.create.api.equipment.potatoCannon.PotatoProjectileEntityHitAction;
import com.simibubi.create.api.registry.CreateRegistries;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 火箭土豆与 Create 土豆加农炮的兼容逻辑。
 *
 * <p>弹药类型仍由数据包注册；这里只补充 Create 当前未提供的两项能力：
 * 火箭火焰尾迹，以及“低硬度方块全掉落”的范围爆炸命中效果。</p>
 */
public final class RocketPotatoIntegration {
    private static final ResourceLocation EXPLOSION_ACTION_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "rocket_potato_explosion");

    /** 原版爆炸只负责粒子、音效和击退，实体伤害改由 detonate 按固定上限计算。 */
    private static final ExplosionDamageCalculator VISUAL_ONLY_EXPLOSION = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }
    };

    private RocketPotatoIntegration() {
    }

    /** 在 Create 的内置命中 Action 注册表中登记数据包可引用的 codec。 */
    public static void registerActions(RegisterEvent event) {
        event.register(CreateRegistries.POTATO_PROJECTILE_BLOCK_HIT_ACTION, EXPLOSION_ACTION_ID,
                () -> ExplosiveBlockAction.CODEC);
        event.register(CreateRegistries.POTATO_PROJECTILE_ENTITY_HIT_ACTION, EXPLOSION_ACTION_ID,
                () -> ExplosiveEntityAction.CODEC);
    }

    /** 服务端生成粒子，客户端由原版粒子同步系统显示，避免双方各生成一份。 */
    public static void onProjectileTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof PotatoProjectileEntity projectile)
                || !(projectile.level() instanceof ServerLevel level)
                || projectile.isRemoved()
                || projectile.getStuckEntity() != null
                || !projectile.getItem().is(ModItems.POTATO_ROCKET.get())) {
            return;
        }

        Vec3 motion = projectile.getDeltaMovement();
        Vec3 tail = projectile.position();
        if (motion.lengthSqr() > 1.0E-6D) {
            tail = tail.subtract(motion.normalize().scale(0.28D));
        }

        level.sendParticles(ParticleTypes.FLAME, tail.x, tail.y, tail.z,
                2, 0.035D, 0.035D, 0.035D, 0.012D);
        level.sendParticles(ParticleTypes.SMALL_FLAME, tail.x, tail.y, tail.z,
                1, 0.045D, 0.045D, 0.045D, 0.006D);
        if (projectile.tickCount % 2 == 0) {
            level.sendParticles(ParticleTypes.SMOKE, tail.x, tail.y, tail.z,
                    1, 0.055D, 0.055D, 0.055D, 0.01D);
        }
    }

    public static void detonateConsumed(ServerLevel level, Vec3 center) {
        detonate(level, center, 3.0F, 5.0F, 0.6F);
    }

    private static void detonate(ServerLevel level, Vec3 center, float radius, float maxDamage,
                                 float maxHardness) {
        DamageSource damageSource = level.damageSources().explosion(null, null);

        // NONE 不破坏方块；自定义计算器关闭原版不定值伤害，只保留爆炸粒子、音效和击退。
        level.explode(null, damageSource, VISUAL_ONLY_EXPLOSION, center, radius, false,
                Level.ExplosionInteraction.NONE);

        // 爆心最多 maxDamage 点，并按距离线性衰减到爆炸边缘的 0 点。
        AABB damageArea = new AABB(center, center).inflate(radius);
        for (Entity entity : level.getEntities((Entity) null, damageArea)) {
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                continue;
            }

            double distance = Math.sqrt(living.distanceToSqr(center));
            if (distance >= radius) {
                continue;
            }

            float falloff = (float) (1.0D - distance / radius);
            float damage = maxDamage * falloff;
            if (damage > 0.0F) {
                living.hurt(damageSource, damage);
            }
        }

        BlockPos origin = BlockPos.containing(center);
        int blockRadius = Mth.ceil(radius);
        double radiusSqr = radius * radius;
        for (BlockPos pos : BlockPos.betweenClosed(
                origin.offset(-blockRadius, -blockRadius, -blockRadius),
                origin.offset(blockRadius, blockRadius, blockRadius))) {
            if (center.distanceToSqr(Vec3.atCenterOf(pos)) > radiusSqr) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0.0F || hardness > maxHardness) {
                continue;
            }

            // 显示与正常破坏一致的方块碎屑，再通过 destroyBlock(dropBlock=true)
            // 走方块战利品表，保证每个被摧毁方块都执行一次完整掉落。
            level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));
            level.destroyBlock(pos, true, null, 512);
        }
    }

    public record ExplosiveBlockAction(float radius, float damage, float maxHardness)
            implements PotatoProjectileBlockHitAction {
        public static final MapCodec<ExplosiveBlockAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.FLOAT.optionalFieldOf("radius", 3.0F).forGetter(ExplosiveBlockAction::radius),
                        Codec.FLOAT.optionalFieldOf("damage", 5.0F).forGetter(ExplosiveBlockAction::damage),
                        Codec.FLOAT.optionalFieldOf("max_hardness", 0.6F)
                                .forGetter(ExplosiveBlockAction::maxHardness)
                ).apply(instance, ExplosiveBlockAction::new));

        @Override
        public boolean execute(LevelAccessor level, ItemStack projectile, BlockHitResult ray) {
            if (level instanceof ServerLevel serverLevel) {
                detonate(serverLevel, ray.getLocation(), radius, damage, maxHardness);
            }
            return true;
        }

        @Override
        public MapCodec<? extends PotatoProjectileBlockHitAction> codec() {
            return CODEC;
        }
    }

    public record ExplosiveEntityAction(float radius, float damage, float maxHardness)
            implements PotatoProjectileEntityHitAction {
        public static final MapCodec<ExplosiveEntityAction> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.FLOAT.optionalFieldOf("radius", 3.0F).forGetter(ExplosiveEntityAction::radius),
                        Codec.FLOAT.optionalFieldOf("damage", 5.0F).forGetter(ExplosiveEntityAction::damage),
                        Codec.FLOAT.optionalFieldOf("max_hardness", 0.6F)
                                .forGetter(ExplosiveEntityAction::maxHardness)
                ).apply(instance, ExplosiveEntityAction::new));

        @Override
        public boolean execute(ItemStack projectile, EntityHitResult ray, Type type) {
            if (ray.getEntity().level() instanceof ServerLevel serverLevel) {
                detonate(serverLevel, ray.getLocation(), radius, damage, maxHardness);
            }
            return true;
        }

        @Override
        public MapCodec<? extends PotatoProjectileEntityHitAction> codec() {
            return CODEC;
        }
    }
}
