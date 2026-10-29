package com.minechai.create_sandustry.content.client.hud;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 沙心 HUD：把沙心从生命条<b>右端向左</b>画在生命条上，覆盖被侵蚀的生命格。
 *
 * <p>布局完全照抄原版生命条（{@code Gui#renderHealthLevel} / {@code Gui#renderHearts}）：
 * 基准点 {@code (guiWidth/2 - 91, guiHeight - 39)}，每行 10 颗、水平间距 8px，行间距
 * {@code max(10 - (行数 - 2), 3)}（行数用原版同一个 {@code l1} 公式）；心位总数由<b>未受沙心影响的外部最大生命值</b>
 * 推得，因此其它模组改变最大生命值后仍能对齐。</p>
 *
 * <p>与原版像素级对齐的三个关键点：</p>
 * <ol>
 *     <li><b>完整复刻原版的血量显示状态机</b>：原版并不是直接用当前血量算心数，而是用
 *         {@code displayHealth}（受击后最多滞后 1 秒才更新、且仅在 {@code j - lastHealthTime > 1000ms} 时刷新），
 *         再取 {@code f = max(最大生命值, max(displayHealth, 当前血量))}。受击瞬间 {@code displayHealth}
 *         仍偏大 → 原版会多画几颗心 → 抖动序列整体错位，这正是“受伤后最后半颗红心对不齐”的原因。
 *         这里把 {@code lastHealth / displayHealth / lastHealthTime / healthBlinkTime} 按原版同一套逻辑维护，
 *         使心数与抖动序列完全一致。</li>
 *     <li><b>复刻低血颤抖</b>：{@code ceil(血量) + ceil(吸收) ≤ 4} 时，原版按 {@code getGuiTicks() * 312871}
 *         为种子、从最右一颗往左逐颗抽取 0~1px 垂直抖动；这里用同一序列，原版覆盖不到的额外沙心格
 *         按同序列继续取值。</li>
 *     <li><b>半颗沙心直接绘制、必要时改为整颗</b>：{@code sand_half} 贴图本身把沙心画在<b>右半边</b>，
 *         与“沙心从右往左填充、不满的那颗在最左端”一致，所以原样绘制即可，同格里原版画在左半边的
 *         红心半心仍然可见；而当沙心<b>已经完全占据生命条</b>（{@code isFilled}）时，全部画整颗，
 *         顺带盖住原版残留在最左侧的那半颗红心，实现“完全占据时不显示红心”。</li>
 * </ol>
 *
 * <p>玩家拥有沙心时，原版伤害吸收心由 {@code GuiHeartMixin} 从原位置隐藏，再由本层接在沙心
 * 序列末尾绘制；红心、沙心和吸收心在同一列上共用抖动值，避免低血量时上下错位。</p>
 *
 * <p>另外：在原版没有画过容器的格子补画容器描边（含极限难度与受伤闪烁变体）；绘制前后把 GUI 颜色
 * 重置为不透明白色，避免继承其它 HUD 图层的残留着色。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SandHeartHudRenderer {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand_hearts");

    private static final ResourceLocation FULL = sprite("sand_full");
    private static final ResourceLocation FULL_BLINKING = sprite("sand_full_blinking");
    private static final ResourceLocation HALF = sprite("sand_half");
    private static final ResourceLocation HALF_BLINKING = sprite("sand_half_blinking");
    private static final ResourceLocation HARDCORE_FULL = sprite("sand_hardcore_full");
    private static final ResourceLocation HARDCORE_FULL_BLINKING = sprite("sand_hardcore_full_blinking");
    private static final ResourceLocation HARDCORE_HALF = sprite("sand_hardcore_half");
    private static final ResourceLocation HARDCORE_HALF_BLINKING = sprite("sand_hardcore_half_blinking");

    private static final ResourceLocation CONTAINER = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation CONTAINER_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/container_blinking");
    private static final ResourceLocation CONTAINER_HARDCORE =
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore");
    private static final ResourceLocation CONTAINER_HARDCORE_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/container_hardcore_blinking");

    private static final ResourceLocation ABSORBING_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full");
    private static final ResourceLocation ABSORBING_FULL_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full_blinking");
    private static final ResourceLocation ABSORBING_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half");
    private static final ResourceLocation ABSORBING_HALF_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half_blinking");
    private static final ResourceLocation ABSORBING_HARDCORE_FULL =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_full");
    private static final ResourceLocation ABSORBING_HARDCORE_FULL_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_full_blinking");
    private static final ResourceLocation ABSORBING_HARDCORE_HALF =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_half");
    private static final ResourceLocation ABSORBING_HARDCORE_HALF_BLINKING =
            ResourceLocation.withDefaultNamespace("hud/heart/absorbing_hardcore_half_blinking");

    private static final int HEARTS_PER_ROW = 10;
    private static final int HEART_SIZE = 9;
    private static final int HEART_SEPARATION = 8;
    private static final int BASE_Y_OFFSET = 39;

    // 与原版 Gui 相同的血量显示状态（默认值同原版字段初始值）。
    private static int lastHealth;
    private static int displayHealth;
    private static long lastHealthTime;
    private static long healthBlinkTime;

    private SandHeartHudRenderer() {
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "hud/heart/" + name);
    }

    /** 在原版生命条之后注册沙心覆盖层。 */
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, LAYER_ID, SandHeartHudRenderer::render);
    }

    private static void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.gameMode == null
                || !minecraft.gameMode.canHurtPlayer()) {
            lastHealth = 0;
            displayHealth = 0;
            lastHealthTime = 0L;
            healthBlinkTime = 0L;
            return;
        }

        int guiTicks = minecraft.gui.getGuiTicks();
        // ▼ 与原版 renderHealthLevel 完全相同的血量显示状态机
        int health = Mth.ceil(player.getHealth());
        boolean damageBlink = healthBlinkTime > (long) guiTicks
                && (healthBlinkTime - (long) guiTicks) / 3L % 2L == 1L;
        long now = Util.getMillis();
        if (health < lastHealth && player.invulnerableTime > 0) {
            lastHealthTime = now;
            healthBlinkTime = (long) (guiTicks + 20);
        } else if (health > lastHealth && player.invulnerableTime > 0) {
            lastHealthTime = now;
            healthBlinkTime = (long) (guiTicks + 10);
        }
        if (now - lastHealthTime > 1000L) {
            lastHealth = health;
            displayHealth = health;
            lastHealthTime = now;
        }
        lastHealth = health;

        int units = SandHeartManager.getUnits(player);
        if (units <= 0) {
            return;
        }
        double externalMax = SandHeartManager.getExternalMaxHealth(player);
        int totalHearts = Mth.ceil(externalMax / 2.0D);
        if (totalHearts <= 0) {
            return;
        }

        // 原版的心数基准 f 与行数，务必与 renderHealthLevel 一致，否则抖动序列与行距都会错位。
        float f = Math.max((float) player.getAttributeValue(Attributes.MAX_HEALTH),
                (float) Math.max(displayHealth, health));
        int absorptionAmount = Mth.ceil(player.getAbsorptionAmount());
        int vanillaRows = Mth.ceil((f + (float) absorptionAmount) / 2.0F / 10.0F);
        int rowHeight = Math.max(HEARTS_PER_ROW - (vanillaRows - 2), 3);
        int absorptionHearts = Mth.ceil(absorptionAmount / 2.0D);
        // 原版伤害吸收心由 GuiHeartMixin 在沙心存在时暂时隐藏，因此红心/沙心的抖动序列
        // 必须按“没有吸收心”的原版循环生成本。之后吸收心再从沙心序列末尾继续排布。
        int healthTop = Mth.ceil(f / 2.0F) - 1;
        int healthHearts = healthTop + 1;
        int regenIndex = -1;
        if (player.hasEffect(MobEffects.REGENERATION)) {
            // 与原版 renderHealthLevel 相同：再生效果会让一个红心按 tickCount % ceil(f + 5) 上浮。
            regenIndex = guiTicks % Mth.ceil(f + 5.0F);
        }

        int sandHearts = Mth.ceil(units / (double) SandHeartManager.UNITS_PER_HEART);
        int pullProgress = Math.max(0, player.getData(ModAttachments.SWORD_PULL_PROGRESS));
        int visibleSandHearts = Math.max(0, sandHearts - Math.min(pullProgress, sandHearts));
        boolean fullyOccupied = SandHeartManager.isFilled(player);
        int absorptionMaxIndex = -1;
        for (int a = 0; a < absorptionHearts; a++) {
            absorptionMaxIndex = Math.max(absorptionMaxIndex,
                    absorptionHeartIndex(totalHearts, visibleSandHearts, absorptionHearts, a));
        }
        int baseX = guiGraphics.guiWidth() / 2 - 91;
        int baseY = guiGraphics.guiHeight() - BASE_Y_OFFSET;

        boolean hardcore = player.level().getLevelData().isHardcore();
        ResourceLocation fullSprite = hardcore ? HARDCORE_FULL : FULL;
        ResourceLocation fullBlinkingSprite = hardcore ? HARDCORE_FULL_BLINKING : FULL_BLINKING;
        ResourceLocation halfSprite = hardcore ? HARDCORE_HALF : HALF;
        ResourceLocation halfBlinkingSprite = hardcore ? HARDCORE_HALF_BLINKING : HALF_BLINKING;
        ResourceLocation containerSprite = hardcore ? CONTAINER_HARDCORE : CONTAINER;
        ResourceLocation containerBlinkingSprite = hardcore ? CONTAINER_HARDCORE_BLINKING : CONTAINER_BLINKING;
        ResourceLocation absorbingFullSprite = hardcore ? ABSORBING_HARDCORE_FULL : ABSORBING_FULL;
        ResourceLocation absorbingFullBlinkingSprite =
                hardcore ? ABSORBING_HARDCORE_FULL_BLINKING : ABSORBING_FULL_BLINKING;
        ResourceLocation absorbingHalfSprite = hardcore ? ABSORBING_HARDCORE_HALF : ABSORBING_HALF;
        ResourceLocation absorbingHalfBlinkingSprite =
                hardcore ? ABSORBING_HARDCORE_HALF_BLINKING : ABSORBING_HALF_BLINKING;

        boolean lowHealth = health <= 4;
        int maxIndex = Math.max(Math.max(healthTop, totalHearts - 1),
                Math.max(absorptionMaxIndex, HEARTS_PER_ROW - 1));
        int[] jitter = new int[Math.max(maxIndex + 1, 1)];
        if (lowHealth) {
            RandomSource random = RandomSource.create(guiTicks * 312871L);
            for (int l = healthTop; l >= 0; l--) {
                jitter[l] = random.nextInt(2);
            }
            for (int l = healthTop + 1; l <= maxIndex; l++) {
                jitter[l] = random.nextInt(2);
            }
        }


        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // 先按正常右对齐位置画完整容器；拔剑预演时隐藏的右端沙心仍保留空容器。
        for (int n = 0; n < sandHearts && n < totalHearts; n++) {
            int index = totalHearts - 1 - n;
            int column = index % HEARTS_PER_ROW;
            int x = baseX + column * HEART_SEPARATION;
            int y = baseY - (index / HEARTS_PER_ROW) * rowHeight + jitter[column];
            if (regenIndex >= 0 && regenIndex < healthHearts
                    && column == regenIndex % HEARTS_PER_ROW) {
                y -= 2;
            }
            if (index > healthTop) {
                guiGraphics.blitSprite(damageBlink ? containerBlinkingSprite : containerSprite,
                        x, y, HEART_SIZE, HEART_SIZE);
            }
        }

        // 正常状态从右向左铺满；拔剑预演时剩余沙心移动到最左侧，因此最右侧会先消失。
        for (int n = 0; n < visibleSandHearts && n < totalHearts; n++) {
            int index = pullProgress > 0 ? n : totalHearts - 1 - n;
            int column = index % HEARTS_PER_ROW;
            int x = baseX + column * HEART_SEPARATION;
            int y = baseY - (index / HEARTS_PER_ROW) * rowHeight + jitter[column];
            if (regenIndex >= 0 && regenIndex < healthHearts
                    && column == regenIndex % HEARTS_PER_ROW) {
                y -= 2;
            }
            boolean half = !fullyOccupied
                    && n == sandHearts - 1
                    && units % SandHeartManager.UNITS_PER_HEART != 0;
            ResourceLocation heart = half ? halfSprite : fullSprite;
            if (damageBlink) {
                heart = half ? halfBlinkingSprite : fullBlinkingSprite;
            }
            guiGraphics.blitSprite(heart, x, y, HEART_SIZE, HEART_SIZE);
        }

        // 伤害吸收心接在沙心序列之后；组内从左到右排列，半颗心位于最右端。
        for (int a = 0; a < absorptionHearts; a++) {
            int index = absorptionHeartIndex(totalHearts, visibleSandHearts, absorptionHearts, a);
            int column = index % HEARTS_PER_ROW;
            int x = baseX + column * HEART_SEPARATION;
            int y = baseY - (index / HEARTS_PER_ROW) * rowHeight + jitter[column];
            guiGraphics.blitSprite(damageBlink ? containerBlinkingSprite : containerSprite,
                    x, y, HEART_SIZE, HEART_SIZE);
            boolean half = absorptionAmount % 2 != 0 && a == absorptionHearts - 1;
            ResourceLocation heart = half ? absorbingHalfSprite : absorbingFullSprite;
            if (damageBlink) {
                heart = half ? absorbingHalfBlinkingSprite : absorbingFullBlinkingSprite;
            }
            guiGraphics.blitSprite(heart, x, y, HEART_SIZE, HEART_SIZE);
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * 计算第 {@code offset} 颗伤害吸收心在沙心之后应占据的槽位。
     *
     * <p>如果当前排剩余空间足够，整组吸收心紧贴在沙心左侧，并按从左到右的顺序排列，
     * 半颗心位于整组的最右端。如果当前排放不下，整组移到上方新的一排，从最左侧开始
     * 向右排布，避免吸收心把顺序反转成沙心式的从右到左。</p>
     */
    private static int absorptionHeartIndex(int totalHearts, int sandHearts, int absorptionHearts, int offset) {
        int rowsUsed = (totalHearts + HEARTS_PER_ROW - 1) / HEARTS_PER_ROW;
        int lastRowStart = (rowsUsed - 1) * HEARTS_PER_ROW;
        // 沙心占满生命条右端；这里只统计最后一排中沙心左侧真正可用的槽位。
        int availableInLastRow = Math.max(0, totalHearts - sandHearts - lastRowStart);
        if (absorptionHearts <= availableInLastRow) {
            return availableInLastRow - absorptionHearts + offset;
        }

        return rowsUsed * HEARTS_PER_ROW + offset;
    }
}
