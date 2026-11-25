package com.minechai.create_sandustry.content.livingsand;

import com.minechai.create_sandustry.content.sand.SpecialSandBlock;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModEntityTypes;
import com.minechai.create_sandustry.registry.ModBlocks;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class LivingSandEvents {
    private static final int PARASITE_AIR_CONSUMPTION = 20;
    private static final int BLINDNESS_DURATION = 20;
    private static final ResourceKey<DamageType> LIVING_SAND_SUFFOCATION = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "living_sand_suffocation"));
    private static final List<PendingSpawn> PENDING_SPAWNS = new ArrayList<>();

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        float spawnChance;
        if (event.getState().is(Blocks.SUSPICIOUS_SAND)) {
            // 精准采集挖掘可疑沙子时，不触发活沙生成。
            if (SpecialSandBlock.hasSilkTouch(serverLevel, event.getPlayer().getMainHandItem())) {
                return;
            }
            spawnChance = ModConfigs.LIVING_SAND_SPAWN_FROM_SUSPICIOUS_SAND.get().floatValue();
        } else if (event.getState().is(Blocks.SAND)) {
            spawnChance = ModConfigs.LIVING_SAND_SPAWN_FROM_SAND.get().floatValue();
        } else if (event.getState().is(ModBlocks.HARDENED_SAND.get())) {
            spawnChance = ModConfigs.LIVING_SAND_SPAWN_FROM_HARDENED_SAND.get().floatValue();
        } else {
            return;
        }

        PENDING_SPAWNS.add(new PendingSpawn(
                serverLevel,
                event.getPos().immutable(),
                serverLevel.getServer().getTickCount() + 1,
                spawnChance));
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos placedPos = event.getPos();
        if (event.getPlacedBlock().is(ModBlocks.LIVING_SAND.get())) {
            trySpawnBabyVillager(serverLevel, placedPos.below(), placedPos);
        } else if (event.getPlacedBlock().is(Blocks.SOUL_SAND)) {
            trySpawnBabyVillager(serverLevel, placedPos, placedPos.above());
        }
    }

    private static void trySpawnBabyVillager(ServerLevel level, BlockPos soulSandPos, BlockPos livingSandPos) {
        if (!level.getBlockState(soulSandPos).is(Blocks.SOUL_SAND)
                || !level.getBlockState(livingSandPos).is(ModBlocks.LIVING_SAND.get())) {
            return;
        }

        level.removeBlock(livingSandPos, false);
        level.removeBlock(soulSandPos, false);

        level.levelEvent(
                LevelEvent.PARTICLES_DESTROY_BLOCK,
                livingSandPos,
                Block.getId(ModBlocks.LIVING_SAND.get().defaultBlockState()));
        level.levelEvent(
                LevelEvent.PARTICLES_DESTROY_BLOCK,
                soulSandPos,
                Block.getId(Blocks.SOUL_SAND.defaultBlockState()));

        Villager villager = EntityType.VILLAGER.create(level);
        if (villager != null) {
            villager.setBaby(true);
            villager.moveTo(
                    soulSandPos.getX() + 0.5D,
                    soulSandPos.getY() + 0.05D,
                    soulSandPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F);
            level.addFreshEntityWithPassengers(villager);
        }
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        int now = event.getServer().getTickCount();
        Iterator<PendingSpawn> iterator = PENDING_SPAWNS.iterator();

        while (iterator.hasNext()) {
            PendingSpawn pending = iterator.next();
            if (pending.level().getServer() != event.getServer()) {
                iterator.remove();
                continue;
            }

            if (pending.tick() <= now) {
                iterator.remove();
                if (pending.level().getBlockState(pending.pos()).isAir()
                        && pending.level().random.nextFloat() < pending.spawnChance()) {
                    ModEntityTypes.LIVING_SAND.get().spawn(
                            pending.level(),
                            pending.pos(),
                            MobSpawnType.TRIGGERED);
                }
            }
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !isParasitized(player)) {
            return;
        }

        MobEffectInstance blindness = player.getEffect(MobEffects.BLINDNESS);
        if (blindness == null || blindness.getDuration() <= 10) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    BLINDNESS_DURATION,
                    0,
                    false,
                    false,
                    false));
        }
    }

    public static void onLivingBreathe(LivingBreatheEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isParasitized(player)) {
            return;
        }

        event.setCanBreathe(false);
        event.setConsumeAirAmount(PARASITE_AIR_CONSUMPTION);
    }

    public static void onLivingDrown(LivingDrownEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isParasitized(player) || !event.isDrowning()) {
            return;
        }

        event.setCanceled(true);
        player.setAirSupply(0);

        int bubbleCount = event.getBubbleCount();
        if (bubbleCount > 0) {
            Vec3 velocity = player.getDeltaMovement();
            for (int i = 0; i < bubbleCount; i++) {
                player.level().addParticle(
                        ParticleTypes.BUBBLE,
                        player.getX() + (player.getRandom().nextDouble() - 0.5D),
                        player.getY() + player.getRandom().nextDouble(),
                        player.getZ() + (player.getRandom().nextDouble() - 0.5D),
                        velocity.x,
                        velocity.y,
                        velocity.z);
            }
        }

        float damage = event.getDamageAmount();
        if (damage > 0.0F) {
            player.hurt(
                    player.level().damageSources().source(LIVING_SAND_SUFFOCATION),
                    damage);
        }
    }

    private static boolean isParasitized(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.LIVING_SAND.get());
    }

    private record PendingSpawn(ServerLevel level, BlockPos pos, int tick, float spawnChance) {
    }

    private LivingSandEvents() {
    }
}

