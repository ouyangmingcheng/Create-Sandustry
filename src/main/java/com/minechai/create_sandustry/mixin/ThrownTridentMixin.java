package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordPedestalBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 引雷三叉戟命中沙金剑座时，固定把闪电引到剑座本体。 */
@Mixin(ThrownTrident.class)
public abstract class ThrownTridentMixin {
    @Inject(method = "hitBlockEnchantmentEffects", at = @At("HEAD"), cancellable = true)
    private void createsandustry$channelLightningIntoPedestal(ServerLevel level, BlockHitResult hitResult,
            ItemStack stack, CallbackInfo ci) {
        if (!level.isThundering()
                || !(level.getBlockEntity(hitResult.getBlockPos()) instanceof SandMasterSwordPedestalBlockEntity pedestal)
                || !pedestal.isHoldingSandSword()) {
            return;
        }
        int channeling = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.CHANNELING)
                .map(stack::getEnchantmentLevel)
                .orElse(0);
        if (channeling <= 0) {
            return;
        }

        ThrownTrident trident = (ThrownTrident) (Object) this;
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
        if (lightning == null) {
            return;
        }
        lightning.moveTo(Vec3.atBottomCenterOf(hitResult.getBlockPos().above()));
        Entity owner = trident.getOwner();
        if (owner instanceof ServerPlayer player) {
            lightning.setCause(player);
        }
        level.addFreshEntity(lightning);
        level.playSound(null, hitResult.getBlockPos(), SoundEvents.TRIDENT_THUNDER.value(),
                SoundSource.PLAYERS, 5.0F, 1.0F);
        ci.cancel();
    }
}
