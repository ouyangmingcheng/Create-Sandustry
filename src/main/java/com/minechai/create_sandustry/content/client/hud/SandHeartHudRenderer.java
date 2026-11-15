package com.minechai.create_sandustry.content.client.hud;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.event.EventHooks;

/**
 * 有沙心时在原版 renderHearts 调用处统一绘制生命条。
 * 红心、沙心和吸收心共用原版的闪烁状态、随机源以及每个槽位的坐标，避免叠加层错帧。
 */
@OnlyIn(Dist.CLIENT)
public final class SandHeartHudRenderer {
    private static final ResourceLocation FULL = sprite("sand_full");
    private static final ResourceLocation FULL_BLINKING = sprite("sand_full_blinking");
    private static final ResourceLocation HALF = sprite("sand_half");
    private static final ResourceLocation HALF_BLINKING = sprite("sand_half_blinking");
    private static final ResourceLocation HARDCORE_FULL = sprite("sand_hardcore_full");
    private static final ResourceLocation HARDCORE_FULL_BLINKING = sprite("sand_hardcore_full_blinking");
    private static final ResourceLocation HARDCORE_HALF = sprite("sand_hardcore_half");
    private static final ResourceLocation HARDCORE_HALF_BLINKING = sprite("sand_hardcore_half_blinking");

    private static final int HEARTS_PER_ROW = 10;
    private static final int HEART_SIZE = 9;
    private static final int HEART_SEPARATION = 8;

    private SandHeartHudRenderer() {
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "hud/heart/" + name);
    }

    /**
     * 使用原版刚计算的血量、闪烁和再生状态绘制，返回需要补给 leftHeight 的高度差。
     * 吸收心始终追加在外部最大生命值的全部槽位之后，不占用红心或沙心的槽位。
     */
    public static int render(GuiGraphics graphics, Player player, int x, int y, int vanillaRowHeight,
            int regenIndex, float vanillaMaxHealth, int currentHealth, int displayHealth,
            int absorptionAmount, boolean blink, int units, RandomSource random) {
        double externalMax = SandHeartManager.getExternalMaxHealth(player);
        int healthHearts = Mth.ceil(externalMax / 2.0D);
        int absorptionHearts = Mth.ceil(absorptionAmount / 2.0D);
        int totalHearts = healthHearts + absorptionHearts;
        int rows = Mth.ceil(totalHearts / (double) HEARTS_PER_ROW);
        int rowHeight = Math.max(HEARTS_PER_ROW - (rows - 2), 3);
        int vanillaRows = Mth.ceil((vanillaMaxHealth + absorptionAmount) / 2.0F / HEARTS_PER_ROW);

        int sandHearts = Math.min(healthHearts, Mth.ceil(units / (double) SandHeartManager.UNITS_PER_HEART));
        int pullProgress = Math.max(0, player.getData(ModAttachments.SWORD_PULL_PROGRESS));
        int visibleSandHearts = Math.max(0, sandHearts - pullProgress);
        boolean fullyOccupied = units >= Math.max(0, (int) Math.floor(externalMax));
        // 属性下限会保留 1 点实际生命；HUD 中这点生命仍必须服从沙心占位，受伤残影也一样。
        int healthLimit = fullyOccupied ? 0 : Mth.ceil(Math.max(0.0D, externalMax - units));
        int visibleHealth = Math.min(currentHealth, healthLimit);
        int visibleDisplayHealth = Math.min(displayHealth, healthLimit);
        boolean lowHealth = currentHealth + absorptionAmount <= 4;
        boolean hardcore = player.level().getLevelData().isHardcore();
        Gui.HeartType healthType = heartType(player);
        Gui.HeartType absorptionType = healthType == Gui.HeartType.WITHERED
                ? healthType : Gui.HeartType.ABSORBING;

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        for (int index = totalHearts - 1; index >= 0; index--) {
            int heartX = x + index % HEARTS_PER_ROW * HEART_SEPARATION;
            int heartY = y - index / HEARTS_PER_ROW * rowHeight;
            if (lowHealth) {
                heartY += random.nextInt(2);
            }
            if (index < healthHearts && index == regenIndex) {
                heartY -= 2;
            }

            drawHeart(graphics, Gui.HeartType.CONTAINER, heartX, heartY, hardcore, false, blink);
            if (index >= healthHearts) {
                boolean half = (index - healthHearts) * 2 + 1 == absorptionAmount;
                drawHeart(graphics, absorptionType, heartX, heartY, hardcore, half, false);
                continue;
            }

            // 正常从右往左填沙心；拔剑预览仍把剩余沙心靠左排列，并保留原有空容器。
            int sandOffset = pullProgress > 0 ? index : healthHearts - 1 - index;
            boolean sand = sandOffset < visibleSandHearts;
            boolean halfSand = !fullyOccupied && sandOffset == sandHearts - 1
                    && units % SandHeartManager.UNITS_PER_HEART != 0;
            // 完整沙心的槽位直接省略红心，透明边缘和上下跳动时也不会露出底下的红心。
            if (!sand || halfSand) {
                int healthOffset = index * 2;
                if (blink && healthOffset < visibleDisplayHealth) {
                    drawHeart(graphics, healthType, heartX, heartY, hardcore,
                            healthOffset + 1 == visibleDisplayHealth, true);
                }
                if (healthOffset < visibleHealth) {
                    drawHeart(graphics, healthType, heartX, heartY, hardcore,
                            healthOffset + 1 == visibleHealth, false);
                }
            }
            if (sand) {
                graphics.blitSprite(sandSprite(hardcore, halfSand, blink), heartX, heartY, HEART_SIZE, HEART_SIZE);
            }
        }
        RenderSystem.disableBlend();
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 行距也随总行数改变，必须比较完整高度，不能只补“多出来的行数”。
        return (rows - 1) * rowHeight - (vanillaRows - 1) * vanillaRowHeight;
    }

    private static Gui.HeartType heartType(Player player) {
        Gui.HeartType type;
        if (player.hasEffect(MobEffects.POISON)) {
            type = Gui.HeartType.POISIONED;
        } else if (player.hasEffect(MobEffects.WITHER)) {
            type = Gui.HeartType.WITHERED;
        } else if (player.isFullyFrozen()) {
            type = Gui.HeartType.FROZEN;
        } else {
            type = Gui.HeartType.NORMAL;
        }
        return EventHooks.firePlayerHeartTypeEvent(player, type);
    }

    private static void drawHeart(GuiGraphics graphics, Gui.HeartType type, int x, int y,
            boolean hardcore, boolean half, boolean blink) {
        graphics.blitSprite(type.getSprite(hardcore, half, blink), x, y, HEART_SIZE, HEART_SIZE);
    }

    private static ResourceLocation sandSprite(boolean hardcore, boolean half, boolean blink) {
        if (hardcore) {
            return half ? (blink ? HARDCORE_HALF_BLINKING : HARDCORE_HALF)
                    : (blink ? HARDCORE_FULL_BLINKING : HARDCORE_FULL);
        }
        return half ? (blink ? HALF_BLINKING : HALF) : (blink ? FULL_BLINKING : FULL);
    }
}
