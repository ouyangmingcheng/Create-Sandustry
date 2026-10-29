package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.sandheart.SandHeartManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 调整原版生命/护甲布局中与沙心 HUD 冲突的部分。 */
@Mixin(Gui.class)
public abstract class GuiHeartMixin {
    @Shadow
    private int displayHealth;

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

    /**
     * 原版按有效最大生命值预留生命排行数；沙心 HUD 按外部最大生命值绘制，可能多出一排。
     * 在生命层结束后补足额外行高，后续护甲层就会整体上移，不再覆盖第二排沙心。
     */
    @Inject(method = "renderHealthLevel", at = @At("TAIL"))
    private void createsandustry$reserveSandHeartRows(GuiGraphics guiGraphics, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer()
                || SandHeartManager.getUnits(player) <= 0) {
            return;
        }

        int currentHealth = Mth.ceil(player.getHealth());
        float effectiveMax = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH),
                (float) Math.max(this.displayHealth, currentHealth));
        int absorption = Mth.ceil(player.getAbsorptionAmount());
        int vanillaRows = Mth.ceil((effectiveMax + absorption) / 2.0F / 10.0F);
        int externalHearts = Mth.ceil(SandHeartManager.getExternalMaxHealth(player) / 2.0D);
        int sandRows = (externalHearts + 9) / 10;
        if (sandRows <= vanillaRows) {
            return;
        }

        int rowHeight = Math.max(10 - (vanillaRows - 2), 3);
        Gui self = (Gui) (Object) this;
        self.leftHeight += (sandRows - vanillaRows) * rowHeight;
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
