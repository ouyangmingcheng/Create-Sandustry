package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.gravity.GravityBounceTracker;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 沙金工艺的数据附件（NeoForge {@link AttachmentType}）。
 *
 * <p>附件可作用于实体与 BlockEntity。沙心数量保存在实体附件 {@link #SAND_HEARTS} 中（玩家与生物通用）：</p>
 * <ul>
 *     <li>{@code serialize(Codec.INT)}：写入玩家 NBT（存档 / 重进 / 维度切换后保留）；</li>
 *     <li>{@code sync(...)}：服务端权威值会同步到客户端（玩家自己也会收到，见
 *         {@code AttachmentSync#syncEntityUpdate}），供 HUD 及时显示；</li>
 *     <li>不设置 {@code copyOnDeath()}：死亡重生后沙心会被清空（见 {@code SandHeartEvents}）。</li>
 * </ul>
 * 数值单位是“半颗沙心”（见 {@code SandHeartManager}），不是普通生命值。
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, CreateSandustry.MODID);

    /** A saved ItemEntity flight resumes; newly dropped stacks start with a fresh attachment. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<GravityBounceTracker>> GRAVITY_BOUNCE =
            ATTACHMENT_TYPES.register("gravity_bounce", () -> AttachmentType.builder(GravityBounceTracker::new)
                    .serialize(GravityBounceTracker.CODEC)
                    .build());

    /** 实体当前拥有的沙心数量，单位：半颗沙心（玩家与生物通用；生物身上是“虚拟沙心”）。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SAND_HEARTS =
            ATTACHMENT_TYPES.register("sand_hearts", () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * 破损沙噬剑的自动修复计时，单位：服务端 tick（每 200 tick 尝试修复一次）。
     *
     * <p>只序列化、不参与客户端同步：纯服务端节流用，客户端不需要知道；随玩家存档持久化，
     * 因此退出重进不会立刻“免费修复”，也不会把计时彻底清零。</p>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SAND_MASTER_SWORD_REPAIR_TICKS =
            ATTACHMENT_TYPES.register("sand_master_sword_repair_ticks", () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    /** Tooltip-only snapshot of the repair cycle, synchronized once per second. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SAND_MASTER_SWORD_REPAIR_DISPLAY_TICKS =
            ATTACHMENT_TYPES.register("sand_master_sword_repair_display_ticks", () -> AttachmentType.<Integer>builder(() -> 0)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /** 月亮崇拜期间完成的有效跳跃次数。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> MOON_WORSHIP_JUMPS =
            ATTACHMENT_TYPES.register("moon_worship_jumps", () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT)
                    .build());

    /**
     * 传送带段是否安装了筛振筛网。
     *
     * <p>该附件作用于 Create 的 {@code BeltBlockEntity}：只序列化布尔值，
     * 并同步到客户端供后续筛网模型/动画使用。</p>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> SIFTING_BELT =
            ATTACHMENT_TYPES.register("sifting_belt", () -> AttachmentType.<Boolean>builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    /**
     * 该筛振传送带段是否使用黄铜溜槽材质。
     *
     * <p>Create 6.0.10 中的黄铜溜槽对应 {@code smart_chute}。该附件只影响
     * 批量处理能力和嘴部模型，不改变筛振本身的安装状态。</p>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> SIFTING_BELT_BRASS =
            ATTACHMENT_TYPES.register("sifting_belt_brass", () -> AttachmentType.<Boolean>builder(() -> false)
                    .serialize(Codec.BOOL)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    /**
     * 该筛振传送带段是否使用 Create: Connected 的黄铜溜槽材质。
     *
     * <p>对应方块 {@code create_connected:brass_chute}。该模组为可选依赖，
     * 因此安装、拆除和渲染都通过注册名与附件判断，避免缺失模组时加载失败。</p>
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> SIFTING_BELT_CONNECTED_BRASS =
            ATTACHMENT_TYPES.register("sifting_belt_connected_brass",
                    () -> AttachmentType.<Boolean>builder(() -> false)
                            .serialize(Codec.BOOL)
                            .sync(ByteBufCodecs.BOOL)
                            .build());

    /** 拔剑会话是否处于激活状态；用于客户端持手动画与取消右键挥臂。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Boolean>> SWORD_PULL_ACTIVE =
            ATTACHMENT_TYPES.register("sword_pull_active", () -> AttachmentType.<Boolean>builder(() -> false)
                    .sync(ByteBufCodecs.BOOL)
                    .build());
    /**
     * 当前拔剑进度的纯客户端展示值，0 表示未拔剑，1~6 表示已经从右向左隐藏的沙心数量。
     * 不序列化：拔剑会话本身是临时的，失败或退出后不应留下进度。
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SWORD_PULL_PROGRESS =
            ATTACHMENT_TYPES.register("sword_pull_progress", () -> AttachmentType.<Integer>builder(() -> 0)
                    .sync(ByteBufCodecs.VAR_INT)
                    .build());

    /**
     * 沙噬剑试炼已经支付过的永久生命上限代价，单位：生命值点数。
     * 跨死亡保留，并通过独立的 MAX_HEALTH 修饰符生效。
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Integer>> SAND_SWORD_HEALTH_PENALTY =
            ATTACHMENT_TYPES.register("sand_sword_health_penalty", () -> AttachmentType.<Integer>builder(() -> 0)
                    .serialize(Codec.INT)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    private ModAttachments() {
    }
}
