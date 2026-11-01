package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.melody.MelodyPuzzleManager;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 将原版唱片机的插片/清空与区块重载生命周期桥接到谜题管理器。 */
@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityPuzzleMixin {
    @Inject(method = "setTheItem", at = @At("RETURN"))
    private void createsandustry$afterSetItem(ItemStack stack, CallbackInfo ci) {
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity) (Object) this;
        if (jukebox.getLevel() instanceof ServerLevel serverLevel) {
            MelodyPuzzleManager.onJukeboxItemChanged(serverLevel, jukebox, stack);
        }
    }

    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void createsandustry$afterLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        JukeboxBlockEntity jukebox = (JukeboxBlockEntity) (Object) this;
        if (jukebox.getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().execute(() -> {
                if (!jukebox.isRemoved() && jukebox.getLevel() == serverLevel) {
                    MelodyPuzzleManager.recoverJukebox(serverLevel, jukebox);
                }
            });
        }
    }
}
