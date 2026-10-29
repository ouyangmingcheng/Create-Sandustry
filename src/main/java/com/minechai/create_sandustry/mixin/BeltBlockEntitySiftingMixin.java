package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.belt.SiftingBeltProcessor;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 为每个传送带段提供筛振处理入口。
 *
 * <p>BeltBlockEntity#tick 有多处提前返回，因此使用 RETURN 而不是 TAIL。</p>
 */
@Mixin(BeltBlockEntity.class)
public abstract class BeltBlockEntitySiftingMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void createsandustry$tickSifting(CallbackInfo ci) {
        SiftingBeltProcessor.tickSegment((BeltBlockEntity) (Object) this);
    }
}
