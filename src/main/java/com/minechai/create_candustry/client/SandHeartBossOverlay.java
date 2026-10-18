package com.minechai.create_candustry.client;

import com.minechai.create_candustry.sandheart.SandHeartManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.common.Tags;

/**
 * boss 血条上的“沙心挤占”叠加：在<b>原版血条之上</b>，从<b>右端往左</b>盖上一段原版<b>黄色</b> boss 条。
 *
 * <p>为什么不在服务端直接换颜色：原版一条 boss 条只能有一种颜色，而且进度只能从左往右填充。
 * 直接把整条换成黄色，就会变成“原本的血条消失 + 黄色从左往右长”。所以这里保留原版血条
 * （背景 + 按血量填充的原色进度 + 名称），再在其上用黄色进度贴图的<b>右半段</b>右对齐绘制一段，
 * 于是黄色随沙心增加从右往左覆盖，与玩家 HUD 上沙心吃掉生命条的方向一致。</p>
 *
 * <p>接入点是 NeoForge 的 {@link CustomizeGuiOverlayEvent.BossEventProgress}（客户端、可取消、
 * 在绘制某条 boss 条之前触发）：只在“这条 boss 条属于一只带沙心的 boss”时才接管它的绘制
 * （排版与原版 {@code BossHealthOverlay} 完全一致），其它 boss 条一律不干预。</p>
 *
 * <p>boss 本体与血条的对应关系取自原版固定的血条特征：末影龙的血条会“制造世界迷雾”，
 * 凋零的血条会“压暗天空”且为紫色（两者都是普通进度样式）。模组自定义 boss 的沙化机制照常生效，
 * 但它们的血条特征未知，因此不做叠加绘制。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SandHeartBossOverlay {
    /** 与原版 {@code BossHealthOverlay} 相同的尺寸。 */
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    /** 原版黄色进度贴图：取其右半段即可得到“从右往左”的填充效果。 */
    private static final ResourceLocation YELLOW_PROGRESS =
            ResourceLocation.withDefaultNamespace("boss_bar/yellow_progress");

    private SandHeartBossOverlay() {
    }

    /** 绘制某条 boss 条之前触发：属于带沙心的 boss 时接管这条 boss 条的绘制。 */
    public static void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bar = event.getBossEvent();
        if (bar.getOverlay() != BossEvent.BossBarOverlay.PROGRESS) {
            return;
        }
        LivingEntity boss = findSandifiedBoss(bar);
        if (boss == null) {
            return;
        }
        int units = SandHeartManager.getUnits(boss);
        int capacity = SandHeartManager.capacityUnits(boss);
        if (units <= 0 || capacity <= 0) {
            return;
        }
        int seizedWidth = Mth.lerpDiscrete(Mth.clamp((float) units / (float) capacity, 0.0F, 1.0F), 0, BAR_WIDTH);
        if (seizedWidth <= 0) {
            return;
        }

        // 接管这条 boss 条：自己画“原版血条 + 右端黄色挤占段 + 名称”。
        event.setCanceled(true);
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Minecraft minecraft = Minecraft.getInstance();
        int x = event.getX();
        int y = event.getY();

        RenderSystem.enableBlend();
        // 1) 原版血条：背景 + 按原版规则计算的当前血量进度（与 BossHealthOverlay#drawBar 一致）。
        guiGraphics.blitSprite(bossBarSprite(bar.getColor(), "_background"), x, y, BAR_WIDTH, BAR_HEIGHT);
        int healthWidth = Mth.lerpDiscrete(bar.getProgress(), 0, BAR_WIDTH);
        if (healthWidth > 0) {
            guiGraphics.blitSprite(bossBarSprite(bar.getColor(), "_progress"), BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y,
                    healthWidth, BAR_HEIGHT);
        }
        // 2) 黄色“沙心挤占”段：取黄色进度贴图的右半段、右对齐绘制 ⇒ 从右往左覆盖原版血条。
        guiGraphics.blitSprite(YELLOW_PROGRESS, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH - seizedWidth, 0,
                x + BAR_WIDTH - seizedWidth, y, seizedWidth, BAR_HEIGHT);
        RenderSystem.disableBlend();

        // 3) 名称：排版与原版 BossHealthOverlay#render 相同。
        Component name = bar.getName();
        guiGraphics.drawString(minecraft.font, name, x + BAR_WIDTH / 2 - minecraft.font.width(name) / 2, y - 9,
                0xFFFFFF);
    }

    /**
     * 判断当前这条 boss 条是否属于某只带沙心的 boss（沙心数量由服务端同步过来）。
     *
     * @return 对应的 boss 生物；无法确定归属时返回 {@code null}
     */
    private static LivingEntity findSandifiedBoss(BossEvent bar) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (!living.getType().is(Tags.EntityTypes.BOSSES) || SandHeartManager.getUnits(living) <= 0) {
                continue;
            }
            if (matchesBarType(living, bar)) {
                return living;
            }
        }
        return null;
    }

    /** boss 血条与本体的对应关系：用原版两种 boss 固定的血条特征来区分。 */
    private static boolean matchesBarType(LivingEntity boss, BossEvent bar) {
        if (boss instanceof EnderDragon) {
            return bar.shouldCreateWorldFog();
        }
        if (boss instanceof WitherBoss) {
            return bar.shouldDarkenScreen() && bar.getColor() == BossEvent.BossBarColor.PURPLE;
        }
        // 模组自定义 boss：血条特征未知，只保证沙化机制生效，不做叠加绘制。
        return false;
    }

    /** 按原版命名规则取 boss 条贴图：{@code minecraft:boss_bar/<颜色><后缀>}。 */
    private static ResourceLocation bossBarSprite(BossEvent.BossBarColor color, String suffix) {
        return ResourceLocation.withDefaultNamespace("boss_bar/" + color.getName() + suffix);
    }
}