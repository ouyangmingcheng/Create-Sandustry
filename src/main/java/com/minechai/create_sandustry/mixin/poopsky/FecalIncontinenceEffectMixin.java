package com.minechai.create_sandustry.mixin.poopsky;

import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModMobEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional PoopSky integration: when a player with Golden Selection defecates
 * through {@code fecal_incontinence}, each poop item has a configurable chance
 * to be replaced by a vanilla gold nugget.
 *
 * <p>The mixin only targets PoopSky's own {@code FecalIncontinenceEffect}, so
 * manually used poop items and other poop sources are never affected.</p>
 */
@Pseudo
@Mixin(targets = "com.altnoir.poopsky.content.effect.FecalIncontinenceEffect", remap = false)
public abstract class FecalIncontinenceEffectMixin {
    @Unique
    private static final ThreadLocal<LivingEntity> createsandustry$CURRENT_ENTITY = new ThreadLocal<>();

    @Inject(method = "fecalIncontinence", at = @At("HEAD"))
    private void createsandustry$captureEntity(LivingEntity entity, int amplifier, CallbackInfo ci) {
        createsandustry$CURRENT_ENTITY.set(entity);
    }

    @Redirect(
            method = "fecalIncontinence",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean createsandustry$replacePoopWithGoldNugget(ServerLevel level, Entity entity) {
        if (ModConfigs.POOPSKY_ENABLED.get() && ModConfigs.POOPSKY_FECAL_INCONTINENCE_ENABLED.get()) {
            LivingEntity source = createsandustry$CURRENT_ENTITY.get();
            if (source instanceof ServerPlayer player && player.hasEffect(ModMobEffects.GOLDEN_SELECTION)) {
                if (player.getRandom().nextFloat() < ModConfigs.POOPSKY_FECAL_GOLD_CHANCE.get()) {
                    if (entity instanceof ItemEntity itemEntity) {
                        itemEntity.setItem(new ItemStack(Items.GOLD_NUGGET));
                    }
                }
            }
        }
        return level.addFreshEntity(entity);
    }

    @Inject(method = "fecalIncontinence", at = @At("RETURN"))
    private void createsandustry$releaseEntity(LivingEntity entity, int amplifier, CallbackInfo ci) {
        createsandustry$CURRENT_ENTITY.remove();
    }
}

