package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.compat.minecraft.CatLitterConversion;
import com.minechai.create_sandustry.compat.create.CatSeatHealing;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 猫砂转换与机械手坐垫喂食治疗的 Mixin。
 *
 * <p>1. {@code mobInteract} HEAD 拦截：当 Create 机械手对“坐在坐垫上且受伤”的猫执行交互时，
 * 在普通交互副作用（蹲起切换 / 驯服 / 繁殖）发生之前直接完成一次治疗并终止本次调用。</p>
 *
 * <p>2. {@code usePlayerItem} HEAD 注入：普通玩家喂食（治疗 / 驯服 / 求偶 / 催熟）以及机械手
 * 非坐垫场景下的成功喂食，都会经过 {@code usePlayerItem}，在此触发猫砂转换。专用治疗路径已提前
 * 终止 {@code mobInteract}，不会进入 {@code usePlayerItem}，因此同一次操作不会重复转换。</p>
 */
@Mixin(Cat.class)
public abstract class CatMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void createsandustry$deployerSeatHeal(Player player, InteractionHand hand,
                                                  CallbackInfoReturnable<InteractionResult> cir) {
        if (CatSeatHealing.tryHeal((Cat) (Object) this, player, hand)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "usePlayerItem", at = @At("HEAD"))
    private void createsandustry$onCatFed(Player player, InteractionHand hand, ItemStack stack, CallbackInfo ci) {
        CatLitterConversion.tryConvertSandToCatLitter((Cat) (Object) this);
    }
}
