package com.minechai.create_sandustry.content.client.hud;

import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
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
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

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
 * <p>boss 本体与血条的对应关系优先使用血条 UUID 与实体 UUID 的直接匹配；原版末影龙/凋零再使用
 * 固定的迷雾/压暗天空特征。其它模组 boss 通常使用实体显示名称作为血条名称，因此最后按名称匹配，
 * 多个同名 boss 时选择离当前玩家最近的一个。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SandHeartBossOverlay {
    /** 与原版 {@code BossHealthOverlay} 相同的尺寸。 */
    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    /** 原版黄色进度贴图：取其右半段即可得到“从右往左”的填充效果。 */
    private static final ResourceLocation YELLOW_PROGRESS =
            ResourceLocation.withDefaultNamespace("boss_bar/yellow_progress");
    private static final ResourceLocation WHITE_PROGRESS =
            ResourceLocation.withDefaultNamespace("boss_bar/white_progress");
    private static final ResourceLocation[] OVERLAY_BACKGROUND_SPRITES = {
            ResourceLocation.withDefaultNamespace("boss_bar/notched_6_background"),
            ResourceLocation.withDefaultNamespace("boss_bar/notched_10_background"),
            ResourceLocation.withDefaultNamespace("boss_bar/notched_12_background"),
            ResourceLocation.withDefaultNamespace("boss_bar/notched_20_background")
    };
    private static final ResourceLocation[] OVERLAY_PROGRESS_SPRITES = {
            ResourceLocation.withDefaultNamespace("boss_bar/notched_6_progress"),
            ResourceLocation.withDefaultNamespace("boss_bar/notched_10_progress"),
            ResourceLocation.withDefaultNamespace("boss_bar/notched_12_progress"),
            ResourceLocation.withDefaultNamespace("boss_bar/notched_20_progress")
    };

    private SandHeartBossOverlay() {
    }

    /** 绘制某条 boss 条之前触发：属于带沙心的 boss 时接管这条 boss 条的绘制。 */
    public static void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        BossEvent bar = event.getBossEvent();
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
        drawOverlayBackground(guiGraphics, bar, x, y);
        int healthWidth = Mth.lerpDiscrete(bar.getProgress(), 0, BAR_WIDTH);
        if (healthWidth > 0) {
            guiGraphics.blitSprite(bossBarSprite(bar.getColor(), "_progress"), BAR_WIDTH, BAR_HEIGHT, 0, 0, x, y,
                    healthWidth, BAR_HEIGHT);
            drawOverlayProgress(guiGraphics, bar, x, y, healthWidth, 0);
        }
        // 2) 黄色“沙心挤占”段：取黄色进度贴图的右半段、右对齐绘制 ⇒ 从右往左覆盖原版血条。
        // 黄色 Boss 条使用白色进度贴图，否则黄色侵占段会和原血条完全同色而不可见。
        ResourceLocation seizedProgress = bar.getColor() == BossEvent.BossBarColor.YELLOW
                ? WHITE_PROGRESS : YELLOW_PROGRESS;
        guiGraphics.blitSprite(seizedProgress, BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH - seizedWidth, 0,
                x + BAR_WIDTH - seizedWidth, y, seizedWidth, BAR_HEIGHT);
        drawOverlayProgress(guiGraphics, bar, x + BAR_WIDTH - seizedWidth, y, seizedWidth, BAR_WIDTH - seizedWidth);
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
        Player player = Minecraft.getInstance().player;
        LivingEntity vanillaMatch = null;
        LivingEntity nameMatch = null;
        double bestNameDistance = Double.MAX_VALUE;
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living instanceof Player || !living.isAlive() || living.isRemoved()
                    || SandHeartManager.getUnits(living) <= 0) {
                continue;
            }
            if (bar.getId().equals(living.getUUID())) {
                return living;
            }
            if (matchesVanillaBarType(living, bar)) {
                if (vanillaMatch == null) {
                    vanillaMatch = living;
                }
            } else if (matchesBarName(living, bar)) {
                double distance = player == null ? 0.0D : player.distanceToSqr(living);
                if (distance < bestNameDistance) {
                    bestNameDistance = distance;
                    nameMatch = living;
                }
            }
        }
        return vanillaMatch != null ? vanillaMatch : nameMatch;
    }

    /** 原版两种 boss 的固定血条特征。 */
    private static boolean matchesVanillaBarType(LivingEntity boss, BossEvent bar) {
        if (boss instanceof EnderDragon) {
            return bar.shouldCreateWorldFog();
        }
        if (boss instanceof WitherBoss) {
            return bar.shouldDarkenScreen() && bar.getColor() == BossEvent.BossBarColor.PURPLE;
        }
        return false;
    }

    /** 模组 boss 通常把实体显示名称用作 boss 条名称。 */
    private static boolean matchesBarName(LivingEntity boss, BossEvent bar) {
        String barName = bar.getName().getString();
        if (barName.isEmpty()) {
            return false;
        }
        return barName.equals(boss.getDisplayName().getString())
                || barName.equals(boss.getName().getString());
    }

    /** 按原版命名规则取 boss 条贴图：{@code minecraft:boss_bar/<颜色><后缀>}。 */
    private static ResourceLocation bossBarSprite(BossEvent.BossBarColor color, String suffix) {
        return ResourceLocation.withDefaultNamespace("boss_bar/" + color.getName() + suffix);
    }

    private static void drawOverlayBackground(GuiGraphics guiGraphics, BossEvent bar, int x, int y) {
        int index = bar.getOverlay().ordinal() - 1;
        if (index >= 0 && index < OVERLAY_BACKGROUND_SPRITES.length) {
            guiGraphics.blitSprite(OVERLAY_BACKGROUND_SPRITES[index], x, y, BAR_WIDTH, BAR_HEIGHT);
        }
    }

    private static void drawOverlayProgress(GuiGraphics guiGraphics, BossEvent bar, int x, int y, int width,
            int sourceX) {
        int index = bar.getOverlay().ordinal() - 1;
        if (index >= 0 && index < OVERLAY_PROGRESS_SPRITES.length) {
            guiGraphics.blitSprite(OVERLAY_PROGRESS_SPRITES[index], BAR_WIDTH, BAR_HEIGHT, sourceX, 0, x, y,
                    width, BAR_HEIGHT);
        }
    }
}
