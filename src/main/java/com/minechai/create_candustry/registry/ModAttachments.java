package com.minechai.create_candustry.registry;

import com.minechai.create_candustry.CreateSandustry;
import com.mojang.serialization.Codec;

import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 沙金工艺的实体数据附件（NeoForge {@link AttachmentType}）。
 *
 * <p>沙心数量保存在实体附件 {@link #SAND_HEARTS} 中（玩家与生物通用）：</p>
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
    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }

    private ModAttachments() {
    }
}
