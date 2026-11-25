package com.minechai.create_sandustry.mixin.client;

import com.minechai.create_sandustry.content.sandheart.client.SandHeartHudRenderer;
import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 沙心与原版生命条共用一次绘制，保留原版血量显示状态和 HUD 基准坐标。 */
@Mixin(Gui.class)
public abstract class GuiHeartMixin {
    @Shadow
    @Final
    private RandomSource random;

    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void createsandustry$renderSandHearts(GuiGraphics graphics, Player player, int x, int y,
            int rowHeight, int regenIndex, float maxHealth, int currentHealth, int displayHealth,
            int absorptionAmount, boolean blink, CallbackInfo ci) {
        int units = SandHeartManager.getUnits(player);
        if (units <= 0) {
            return;
        }

        int heightDelta = SandHeartHudRenderer.render(graphics, player, x, y, rowHeight, regenIndex,
                maxHealth, currentHealth, displayHealth, absorptionAmount, blink, units, this.random);
        ((Gui) (Object) this).leftHeight += heightDelta;
        ci.cancel();
    }
}
