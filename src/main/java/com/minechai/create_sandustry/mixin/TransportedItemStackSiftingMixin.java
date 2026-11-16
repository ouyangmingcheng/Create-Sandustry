package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.sifting.SiftingProgressAccess;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给 Create 的传送带物品增加独立的筛振进度字段。
 *
 * <p>不复用鼓风机的 {@code processedBy/processingTime}，避免两种处理逻辑互相覆盖。</p>
 */
@Mixin(TransportedItemStack.class)
public abstract class TransportedItemStackSiftingMixin implements SiftingProgressAccess {
    @Unique
    private ResourceLocation createsandustry$siftingRecipeId;

    @Unique
    private int createsandustry$siftingProgress;

    @Unique
    private int createsandustry$siftingRequiredProgress;

    @Override
    public ResourceLocation getSiftingRecipeId() {
        return createsandustry$siftingRecipeId;
    }

    @Override
    public int getSiftingProgress() {
        return createsandustry$siftingProgress;
    }

    @Override
    public int getSiftingRequiredProgress() {
        return createsandustry$siftingRequiredProgress;
    }

    @Override
    public void setSiftingProgress(ResourceLocation recipeId, int progress, int requiredProgress) {
        createsandustry$siftingRecipeId = recipeId;
        createsandustry$siftingProgress = Math.max(0, progress);
        createsandustry$siftingRequiredProgress = Math.max(0, requiredProgress);
    }

    @Override
    public void clearSiftingProgress() {
        createsandustry$siftingRecipeId = null;
        createsandustry$siftingProgress = 0;
        createsandustry$siftingRequiredProgress = 0;
    }

    @Inject(method = "getSimilar", at = @At("RETURN"))
    private void createsandustry$copySiftingProgress(CallbackInfoReturnable<TransportedItemStack> cir) {
        SiftingProgressAccess copy = (SiftingProgressAccess) (Object) cir.getReturnValue();
        copy.setSiftingProgress(
                createsandustry$siftingRecipeId,
                createsandustry$siftingProgress,
                createsandustry$siftingRequiredProgress);
    }

    @Inject(method = "serializeNBT", at = @At("RETURN"))
    private void createsandustry$writeSiftingProgress(
            HolderLookup.Provider registries,
            CallbackInfoReturnable<CompoundTag> cir) {
        if (createsandustry$siftingRecipeId == null || createsandustry$siftingRequiredProgress <= 0) {
            return;
        }

        CompoundTag nbt = cir.getReturnValue();
        nbt.putString("SandustrySiftingRecipe", createsandustry$siftingRecipeId.toString());
        nbt.putInt("SandustrySiftingProgress", createsandustry$siftingProgress);
        nbt.putInt("SandustrySiftingRequiredProgress", createsandustry$siftingRequiredProgress);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void createsandustry$readSiftingProgress(
            CompoundTag nbt,
            HolderLookup.Provider registries,
            CallbackInfoReturnable<TransportedItemStack> cir) {
        if (!nbt.contains("SandustrySiftingRecipe")) {
            return;
        }

        ResourceLocation recipeId = ResourceLocation.tryParse(nbt.getString("SandustrySiftingRecipe"));
        int progress = nbt.getInt("SandustrySiftingProgress");
        int requiredProgress = nbt.getInt("SandustrySiftingRequiredProgress");
        if (recipeId == null || requiredProgress <= 0) {
            return;
        }

        ((SiftingProgressAccess) (Object) cir.getReturnValue())
                .setSiftingProgress(recipeId, progress, requiredProgress);
    }
}
