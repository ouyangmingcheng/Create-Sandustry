package com.minechai.create_sandustry.content.effect;

import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModAttachments;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class MoonWorshipEvents {
    private static final int TICKS_PER_SECOND = 20;
    private static final ResourceKey<DamageType> MOON_WORSHIP_DAMAGE = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "moon_worship"));

    public static void start(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            return;
        }

        boolean alreadyActive = entity.hasEffect(ModMobEffects.MOON_WORSHIP);
        if (!alreadyActive) {
            entity.setData(ModAttachments.MOON_WORSHIP_JUMPS, 0);
        }

        int duration = Math.max(1, ModConfigs.MOON_WORSHIP_DURATION_SECONDS.get()) * TICKS_PER_SECOND;
        MobEffectInstance instance = new MobEffectInstance(
                ModMobEffects.MOON_WORSHIP,
                duration,
                0,
                false,
                true,
                true);
        if (alreadyActive) {
            // Refresh the full duration without replacing the effect and losing progress.
            entity.forceAddEffect(instance, entity);
        } else {
            entity.addEffect(instance);
        }
    }

    /** Called after a player finishes eating a Moon Crystal. */
    public static void onPlayerEat(ServerPlayer player) {
        if (!player.hasEffect(ModMobEffects.MOON_WORSHIP)) {
            start(player);
            return;
        }

        MobEffectInstance current = player.getEffect(ModMobEffects.MOON_WORSHIP);
        if (current == null) {
            start(player);
            return;
        }

        int reducedDuration = Math.max(1, current.getDuration() - 30 * TICKS_PER_SECOND);
        player.forceAddEffect(new MobEffectInstance(
                ModMobEffects.MOON_WORSHIP,
                reducedDuration,
                current.getAmplifier(),
                current.isAmbient(),
                current.isVisible(),
                current.showIcon()), player);
    }

    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()
                || !entity.isAlive()
                || !entity.hasEffect(ModMobEffects.MOON_WORSHIP)) {
            return;
        }

        int required = Math.max(1, ModConfigs.MOON_WORSHIP_JUMPS_REQUIRED.get());
        int jumps = entity.getData(ModAttachments.MOON_WORSHIP_JUMPS) + 1;
        entity.setData(ModAttachments.MOON_WORSHIP_JUMPS, jumps);

        if (jumps >= required) {
            complete(entity);
        }
    }

    public static void onEffectExpired(MobEffectEvent.Expired event) {
        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null || instance.getEffect().value() != ModMobEffects.MOON_WORSHIP.get()) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) {
            return;
        }

        entity.setData(ModAttachments.MOON_WORSHIP_JUMPS, 0);
        if (entity instanceof ServerPlayer player) {
            player.invulnerableTime = 0;
            player.hurt(player.level().damageSources().source(MOON_WORSHIP_DAMAGE), Float.MAX_VALUE);
        }
    }

    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.isCanceled() || event.getEffect().value() != ModMobEffects.MOON_WORSHIP.get()) {
            return;
        }
        if (!event.getEntity().level().isClientSide()) {
            event.getEntity().setData(ModAttachments.MOON_WORSHIP_JUMPS, 0);
        }
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getTarget() instanceof Rabbit rabbit)) {
            return;
        }

        Player player = event.getEntity();
        if (player.isSpectator() || player instanceof FakePlayer || !rabbit.isAlive()) {
            return;
        }

        ItemStack stack = player.getItemInHand(event.getHand());
        if (!stack.is(ModItems.MOONCRYSTAL.get())) {
            return;
        }

        if (rabbit.hasEffect(ModMobEffects.MOON_WORSHIP)) {
            if (rabbit.getVariant() == Rabbit.Variant.EVIL) {
                return;
            }
            rabbit.setVariant(Rabbit.Variant.EVIL);
            level.playSound(null, rabbit.blockPosition(), SoundEvents.RABBIT_ATTACK,
                    SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    rabbit.getX(), rabbit.getY() + 0.6D, rabbit.getZ(),
                    6, 0.25D, 0.25D, 0.25D, 0.0D);
        } else {
            start(rabbit);
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void complete(LivingEntity entity) {
        if (!entity.hasEffect(ModMobEffects.MOON_WORSHIP)) {
            return;
        }

        if (entity instanceof ServerPlayer player) {
            double chance = ModConfigs.MOON_WORSHIP_PLAYER_REWARD_CHANCE.get();
            if (player.getRandom().nextDouble() < chance) {
                player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.MOONHOP.get()));
            }
            player.displayClientMessage(Component.translatable(
                    "message.createsandustry.moon_worship.completed"), true);
        } else if (entity instanceof Rabbit rabbit) {
            double chance = ModConfigs.MOON_WORSHIP_RABBIT_REWARD_CHANCE.get();
            if (rabbit.getRandom().nextDouble() < chance) {
                rabbit.spawnAtLocation(new ItemStack(ModItems.MOONHOP.get()));
            }
        }

        if (entity.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD,
                    entity.getX(), entity.getY() + 0.7D, entity.getZ(),
                    12, 0.35D, 0.45D, 0.35D, 0.02D);
            level.playSound(null, entity.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.7F, 1.25F);
        }

        entity.setData(ModAttachments.MOON_WORSHIP_JUMPS, 0);
        entity.removeEffect(ModMobEffects.MOON_WORSHIP);
    }

    private MoonWorshipEvents() {
    }
}
