package com.minechai.create_sandustry.mixin.compat.poopsky;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModMobEffects;
import com.minechai.create_sandustry.compat.poopsky.PoopskyCompat;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional PoopSky integration: when the {@code on_the_verge} effect triggers
 * its explosion (the only caller of the three-argument
 * {@code triggerExplosion}), the poop blocks created by the explosion are
 * replaced with vanilla gold blocks while the player has Golden Selection.
 *
 * <p>Poop TNT entities and minecarts call the two-argument overload, so their
 * poop blocks are unaffected.</p>
 */
@Pseudo
@Mixin(targets = "com.altnoir.poopsky.impl.util.PoopTntUtil", remap = false)
public abstract class PoopTntUtilMixin {
    @Inject(method = "triggerExplosion(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private static void createsandustry$beginReplacement(Entity entity, int power, ServerPlayer player, CallbackInfo ci) {
        if (ModConfigs.POOPSKY_ENABLED.get()
                && ModConfigs.POOPSKY_ON_THE_VERGE_ENABLED.get()
                && player != null
                && player.hasEffect(ModMobEffects.GOLDEN_SELECTION)) {
            PoopskyCompat.beginPoopBlockReplacement();
        }
    }

    @Inject(method = "triggerExplosion(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/server/level/ServerPlayer;)V", at = @At("RETURN"))
    private static void createsandustry$endReplacement(Entity entity, int power, ServerPlayer player, CallbackInfo ci) {
        PoopskyCompat.endPoopBlockReplacement();
    }

    @ModifyArg(
            method = "handleInnerExplosionBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            index = 1)
    private static BlockState createsandustry$replaceInner(BlockState state) {
        return PoopskyCompat.shouldReplacePoopBlock() ? Blocks.GOLD_BLOCK.defaultBlockState() : state;
    }

    @ModifyArg(
            method = "handleOuterExplosionBlock",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"),
            index = 1)
    private static BlockState createsandustry$replaceOuter(BlockState state) {
        return PoopskyCompat.shouldReplacePoopBlock() ? Blocks.GOLD_BLOCK.defaultBlockState() : state;
    }
}
