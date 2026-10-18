package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.sandheart.SandHeartManager;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.ModList;
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
 *         按同序列继续取值。安装了“更美观的血条 / Colorful Hearts”时改用<b>它的</b>抖动序列（见下）。</li>
 *     <li><b>半颗沙心直接绘制、必要时改为整颗</b>：{@code sand_half} 贴图本身把沙心画在<b>右半边</b>，
 *         与“沙心从右往左填充、不满的那颗在最左端”一致，所以原样绘制即可，同格里原版画在左半边的
 *         红心半心仍然可见；而当沙心<b>已经完全占据生命条</b>（{@code isFilled}）时，全部画整颗，
 *         顺带盖住原版残留在最左侧的那半颗红心，实现“完全占据时不显示红心”。</li>
 * </ol>
 *
 * <p><b>与“更美观的血条 / Colorful Hearts”（modId {@code colorfulhearts}）的兼容</b>：该模组在
 * {@code RenderGuiLayerEvent.Pre} 中把 {@code VanillaGuiLayers.PLAYER_HEALTH} 整个取消并自行绘制血条，
 * 但它的栅格与原版一致（{@code width/2 - 91} / {@code height - gui.leftHeight}，
 * 每行 10 格、间距 8px），所以默认情况（外部最大生命值 ≤ 20、即单行 10 颗）沙心仍然逐像素对齐。
 * 两处需要跟随它的地方已经在下面对齐：</p>
 * <ul>
 *     <li><b>低血抖动序列</b>：它从 index 0 开始<b>升序</b>为 0~19 号槽位各抽一次随机数（空槽也抽），
 *         与原版“从最右一颗往左”的降序恰好相反；</li>
 *     <li><b>行偏移</b>：它固定单行；而本模组超出一排的沙心改用 <b>3px 密集堆叠</b>，每排只露出 3px 并被前排遮挡（见下）。</li>
 * </ul>
 * <p><b>超出一排的密集化</b>（仅在安装 Colorful Hearts 时生效）：Colorful Hearts 本身只用一行 10 格表示血量，
 * 本模组把超出第一排的沙心逐排向上堆叠、每排仅保留 {@value #COLORFUL_HEARTS_DENSE_ROW_STEP}px，
 * 后排被前排遮挡，从而把“超出 20 点生命上限的沙心”压缩在血条上方一小条范围内，而不是散落到多行。</p>
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

    private static final int HEARTS_PER_ROW = 10;
    private static final int HEART_SIZE = 9;
    private static final int HEART_SEPARATION = 8;
    private static final int BASE_Y_OFFSET = 39;
    /** Colorful Hearts 维护的槽位总数：固定 20 格，且对空槽也会抽取抖动。 */
    private static final int COLORFUL_HEARTS_CELLS = 20;
    /**
     * Colorful Hearts 下，超出一排后的沙心密集堆叠步长：每排只向上移 3px。
     *
     * <p>沙心贴图是 9×9，相邻两排只错开 3px，因此后排（更靠上的一排）只露出顶部 3px，
     * 其余部分被前面（更靠下）的那一排盖住——相当于把超出 20 点生命上限的部分“压扁”堆在血条上方。
     * 绘制顺序是 index 从高到低（先画高排、最后画第一排），天然形成前排遮挡后排。</p>
     */
    private static final int COLORFUL_HEARTS_DENSE_ROW_STEP = 3;
    /** 兼容目标模组的 modId。 */
    private static final String COLORFUL_HEARTS_MOD_ID = "colorfulhearts";

    // 与原版 Gui 相同的血量显示状态（默认值同原版字段初始值）。
    private static int lastHealth;
    private static int displayHealth;
    private static long lastHealthTime;
    private static long healthBlinkTime;
    /** 缓存“是否安装了 Colorful Hearts”，避免每帧查询模组列表。 */
    private static Boolean colorfulHeartsPresent;

    private SandHeartHudRenderer() {
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "hud/heart/" + name);
    }

    /**
     * 是否安装了“更美观的血条 / Colorful Hearts”。
     *
     * <p>客户端运行时才查询（本类只会在客户端加载），结果缓存一次；未安装时沙心完全走原版栅格逻辑。</p>
     */
    private static boolean isColorfulHeartsPresent() {
        Boolean cached = colorfulHeartsPresent;
        if (cached == null) {
            cached = ModList.get().isLoaded(COLORFUL_HEARTS_MOD_ID);
            colorfulHeartsPresent = cached;
        }
        return cached;
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
        int vanillaTop = Mth.ceil(f / 2.0F) + absorptionHearts - 1;

        int sandHearts = Mth.ceil(units / (double) SandHeartManager.UNITS_PER_HEART);
        boolean fullyOccupied = SandHeartManager.isFilled(player);
        boolean colorfulHearts = isColorfulHeartsPresent();
        int baseX = guiGraphics.guiWidth() / 2 - 91;
        int baseY = guiGraphics.guiHeight() - BASE_Y_OFFSET;

        boolean hardcore = player.level().getLevelData().isHardcore();
        ResourceLocation fullSprite = hardcore ? HARDCORE_FULL : FULL;
        ResourceLocation fullBlinkingSprite = hardcore ? HARDCORE_FULL_BLINKING : FULL_BLINKING;
        ResourceLocation halfSprite = hardcore ? HARDCORE_HALF : HALF;
        ResourceLocation halfBlinkingSprite = hardcore ? HARDCORE_HALF_BLINKING : HALF_BLINKING;
        ResourceLocation containerSprite = hardcore ? CONTAINER_HARDCORE : CONTAINER;
        ResourceLocation containerBlinkingSprite = hardcore ? CONTAINER_HARDCORE_BLINKING : CONTAINER_BLINKING;

        boolean lowHealth = health + absorptionAmount <= 4;
        int maxIndex = Math.max(vanillaTop, totalHearts - 1);
        int[] jitter = new int[Math.max(maxIndex + 1, 1)];
        if (lowHealth) {
            RandomSource random = RandomSource.create(guiTicks * 312871L);
            if (colorfulHearts) {
                // Colorful Hearts 的抖动：从 index 0 开始升序逐格抽取（含空槽，共 20 格），
                // 与原版从最右一颗往左的降序恰好相反；这里复刻它的顺序，沙心才会与它画的心同相位。
                for (int l = 0; l <= Math.max(COLORFUL_HEARTS_CELLS - 1, maxIndex); l++) {
                    int roll = random.nextInt(2);
                    if (l <= maxIndex) {
                        jitter[l] = roll;
                    }
                }
            } else {
                for (int l = vanillaTop; l >= 0; l--) {
                    jitter[l] = random.nextInt(2);
                }
                for (int l = vanillaTop + 1; l <= maxIndex; l++) {
                    jitter[l] = random.nextInt(2);
                }
            }
        }

        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int n = 0; n < sandHearts && n < totalHearts; n++) {
            int index = totalHearts - 1 - n;
            int x = baseX + (index % HEARTS_PER_ROW) * HEART_SEPARATION;
            // 行偏移：原版随行数压缩行距；Colorful Hearts 下超出一排的沙心按 3px 密集堆叠，
            // 后排被前排遮挡（循环按 index 从高到低绘制，第一排最后画）。
            int y = colorfulHearts
                    ? baseY - (index / HEARTS_PER_ROW) * COLORFUL_HEARTS_DENSE_ROW_STEP + jitter[index]
                    : baseY - (index / HEARTS_PER_ROW) * rowHeight + jitter[index];
            // 沙心已完全占据生命条时一律画整颗：既保持整条都是沙心，也盖掉原版残留在最左侧的半颗红心。
            boolean half = !fullyOccupied
                    && n == sandHearts - 1
                    && units % SandHeartManager.UNITS_PER_HEART != 0;
            if (index > vanillaTop) {
                guiGraphics.blitSprite(damageBlink ? containerBlinkingSprite : containerSprite,
                        x, y, HEART_SIZE, HEART_SIZE);
            }
            ResourceLocation heart = half ? halfSprite : fullSprite;
            if (damageBlink) {
                heart = half ? halfBlinkingSprite : fullBlinkingSprite;
            }
            guiGraphics.blitSprite(heart, x, y, HEART_SIZE, HEART_SIZE);
        }
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}