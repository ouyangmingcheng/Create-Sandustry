package com.minechai.create_candustry.mixin;

import com.minechai.create_candustry.integration.DeployerBrushCompat;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让 Create 机械手用原版刷子一次接触完成一次考古。
 *
 * <p>机械手调用 {@code Item#useOn} 时，原版 {@code BrushItem#useOn} 只会
 * {@code startUsingItem} 然后返回，机械手不会持续 tick，因此刷掘永远不会推进。这里在
 * {@code useOn} 的 HEAD 拦截：仅当发起者是 Create 机械手（假玩家）且目标方块是可刷方块时，
 * 由 {@link DeployerBrushCompat} 立即完成一次等价刷掘，并让 {@code useOn} 返回
 * {@link InteractionResult#CONSUME}（与原版返回值一致），从而跳过 startUsingItem。</p>
 *
 * <p>普通玩家不是假玩家，因此原版多阶段刷掘行为完全不受影响；未安装 Create 时判定恒为 false，
 * 行为与未安装本兼容一致。</p>
 */
@Mixin(BrushItem.class)
public abstract class BrushItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void createsandustry$deployerAutoBrush(UseOnContext context,
                                                   CallbackInfoReturnable<InteractionResult> cir) {
        if (DeployerBrushCompat.tryCompleteArchaeology(context)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
