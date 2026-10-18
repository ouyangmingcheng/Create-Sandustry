package com.minechai.create_candustry.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minechai.create_candustry.effect.ModMobEffects;
import com.minechai.create_candustry.item.ModItems;
import com.minechai.create_candustry.network.FireSandBeamPayload;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Q 键拦截（客户端）：原版所有“手持物品按 Q / Ctrl+Q”的丢弃都会走到
 * {@link LocalPlayer#drop(boolean)}，这里在它之前截断。
 *
 * <ul>
 *     <li>主手是<b>完整沙噬剑</b>：阻止丢弃；处于“沙化之兆”且攻击冷却满足完整攻击时发送一次发射请求，并让本地攻击冷却
 *         指示器同步进入冷却（真正的判定与扣耐久在服务端）；</li>
 *     <li>主手是<b>破损沙噬剑</b>：既不丢弃也不发射；</li>
 *     <li>其它物品：完全不干预，原版 Q 行为不变。</li>
 * </ul>
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerDropMixin {
    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void createsandustry$fireBeamInsteadOfDrop(boolean fullStack, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer self = (LocalPlayer) (Object) this;
        ItemStack stack = self.getMainHandItem();
        if (stack.getItem() == ModItems.SAND_MASTER_SWORD.get()) {
            // 只有处于“沙化之兆”期间才发送发射请求；不满足时什么都不做（仍然阻止丢弃）。
            if (self.hasEffect(ModMobEffects.SAND_HARDCORE) && self.getAttackStrengthScale(0.5F) >= 1.0F) {
                PacketDistributor.sendToServer(new FireSandBeamPayload());
                self.resetAttackStrengthTicker();
            }
            cir.setReturnValue(false);
        } else if (stack.getItem() == ModItems.SAND_MASTER_SWORD_BROKEN.get()) {
            cir.setReturnValue(false);
        }
    }
}