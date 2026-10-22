package com.minechai.create_candustry.mixin;

import com.minechai.create_candustry.sandheart.SandHeartManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 调整原版生命条中与沙心冲突的绘制。
 *
 * <p>沙心存在时，伤害吸收心交给 {@code SandHeartHudRenderer} 按“沙心之后”的序列绘制；
 * 沙心填满时暂时隐藏原版残留的最后半颗红心。两种情况都只影响 HUD 参数，不修改玩家生命值或沙心数据。</p>
 */
@Mixin(Gui.class)
public abstract class GuiHeartMixin {
    @ModifyVariable(method = "renderHearts", at = @At("HEAD"), argsOnly = true, index = 10)
    private int createsandustry$hideVanillaAbsorption(int absorptionAmount) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && SandHeartManager.getUnits(player) > 0) {
            return 0;
        }
        return absorptionAmount;
    }

    /**
     * 沙心填满时，原版仍可能保留最左侧的半颗红心（例如外部最大生命值为奇数或带小数）。
     * 暂时把红心显示值清零，只影响 HUD 绘制；沙心层本身仍按原数量绘制。
     */
    @ModifyVariable(method = "renderHearts", at = @At("HEAD"), argsOnly = true, index = 8)
    private int createsandustry$hideLastHalfRedCurrent(int currentHealth) {
        return createsandustry$hasFilledSandHearts() ? 0 : currentHealth;
    }

    @ModifyVariable(method = "renderHearts", at = @At("HEAD"), argsOnly = true, index = 9)
    private int createsandustry$hideLastHalfRedDisplay(int displayHealth) {
        return createsandustry$hasFilledSandHearts() ? 0 : displayHealth;
    }

    private static boolean createsandustry$hasFilledSandHearts() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null
                && minecraft.gameMode != null
                && minecraft.gameMode.canHurtPlayer()
                && SandHeartManager.getUnits(player) > 0
                && SandHeartManager.isFilled(player);
    }
}
