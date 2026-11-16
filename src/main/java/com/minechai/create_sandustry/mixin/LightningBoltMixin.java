package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.sandmastersword.SandMasterSwordPedestalEvents;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 在原版避雷针雷击判定阶段把命中的沙金剑座交给本模组处理。 */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {
    @Inject(method = "powerLightningRod", at = @At("HEAD"))
    private void createsandustry$handlePedestalLightningStrike(CallbackInfo ci) {
        LightningBolt lightning = (LightningBolt) (Object) this;
        Vec3 position = lightning.position();
        // 与 LightningBolt#getStrikePosition 完全一致：避雷针目标实体位于针上方一格。
        BlockPos strikePos = BlockPos.containing(position.x, position.y - 1.0E-6D, position.z);
        SandMasterSwordPedestalEvents.handleLightningStrike(lightning.level(), strikePos);
    }
}
