package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.melody.MelodyPuzzleManager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 阻止特殊谜题唱片触发原版 64 格唱片机声音广播。 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelJukeboxAudioMixin {
    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void createsandustry$suppressPuzzleJukeboxSound(
            @Nullable Player player, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type != 1010 && type != 1011) {
            return;
        }
        ServerLevel level = (ServerLevel) (Object) this;
        if (level.getBlockEntity(pos) instanceof JukeboxBlockEntity jukebox
                && (MelodyPuzzleManager.hasSession(level, pos)
                || MelodyPuzzleManager.isPuzzleDisc(jukebox.getTheItem()))) {
            ci.cancel();
        }
    }
}
