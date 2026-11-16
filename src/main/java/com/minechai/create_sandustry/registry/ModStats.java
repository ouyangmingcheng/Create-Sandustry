package com.minechai.create_sandustry.registry;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.foundation.event.ArchaeologyCompletedEvent;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * 沙金工艺的自定义统计项。
 *
 * <p>这里只使用原版自带的统计框架（{@code BuiltInRegistries.CUSTOM_STAT} + {@code Stats.CUSTOM}），
 * 不新增任何独立的统计系统或任务系统：注册完成后，这些统计项会和原版统计一样出现在任何遍历
 * 自定义统计列表的界面里，例如 FTB Quests 内置「统计」任务（{@code ftbquests:stat}）的类型下拉框，
 * 任务作者可直接选择它并设置目标数量（目标数量、进度显示、持久化、团队/完成任务规则全部沿用
 * 该任务本身）。</p>
 *
 * <p>注册方式与原版 {@code Stats#makeCustomStat} 一致：先写入自定义统计注册表，再调用
 * {@code Stats.CUSTOM.get(id)} 实例化对应的 {@code Stat}，使其进入 {@code StatType} 的内部列表
 * （FTB 的类型下拉框正是遍历这个列表）。</p>
 */
public final class ModStats {
    /** 玩家完成的考古次数：{@code createsandustry:archaeology_completed}。 */
    public static final ResourceLocation ARCHAEOLOGY_COMPLETED =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "archaeology_completed");

    /** 玩家挖掘的沙子类方块数量：{@code createsandustry:sand_mined}。 */
    public static final ResourceLocation SAND_MINED =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "sand_mined");

    /** Successful Prismarine Shard to Moon Crystal conversions. */
    public static final ResourceLocation MOONCRYSTAL_CONVERTED =
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "mooncrystal_converted");

    private ModStats() {
    }

    /**
     * 在 mod 事件总线上注册自定义统计项。
     *
     * <p>{@code RegisterEvent} 触发期间 NeoForge 会临时解冻内置注册表，因此可以安全地向
     * {@code minecraft:custom_stat} 注册。</p>
     */
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.CUSTOM_STAT)) {
            return;
        }
        register(event, ARCHAEOLOGY_COMPLETED);
        register(event, SAND_MINED);
        register(event, MOONCRYSTAL_CONVERTED);
    }

    /** 与原版 {@code Stats#makeCustomStat} 相同：写入注册表并立刻实例化 Stat（否则不会出现在候选列表中）。 */
    private static void register(RegisterEvent event, ResourceLocation id) {
        event.register(Registries.CUSTOM_STAT, id, () -> id);
        Stats.CUSTOM.get(id);
    }

    /**
     * 一次考古在服务端真正完成时，为对应玩家累加 1 次考古统计。
     * 事件本身保证了同一次考古只会触发一次（详见 {@link ArchaeologyCompletedEvent}）。
     */
    public static void onArchaeologyCompleted(ArchaeologyCompletedEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player != null) {
            player.awardStat(ARCHAEOLOGY_COMPLETED, 1);
        }
    }

    /**
     * 玩家真正挖掘并成功破坏沙类方块时累加 1 次“挖掘沙子数”。
     *
     * <p>只使用 {@link BlockEvent.BreakEvent}（玩家破坏方块的事件，参数就是 {@code ServerPlayer}），
     * 因此爆炸、活塞、自然消失等非玩家行为不会计入；机械手等假玩家在这里被显式排除；
     * 事件被取消（未真正破坏）时也不计入。一次破坏只触发一次该事件，不会重复累计。</p>
     */
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel)) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        if (!isTrackedSand(event.getState())) {
            return;
        }
        player.awardStat(SAND_MINED, 1);
    }

    /** Awards a successful automated conversion to the nearest real player. */
    public static void awardMoonCrystalConversion(ServerLevel level, BlockPos pos) {
        Player nearest = level.getNearestPlayer(
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D,
                32.0D,
                entity -> entity instanceof ServerPlayer player
                        && !player.isSpectator()
                        && !(player instanceof FakePlayer));
        if (nearest instanceof ServerPlayer player) {
            player.awardStat(MOONCRYSTAL_CONVERTED, 1);
        }
    }

    /**
     * 判断某个方块状态是否属于“挖掘沙子数”的统计范围。
     *
     * <p>范围为：原版 {@code minecraft:sand}、{@code minecraft:red_sand}，以及沙金工艺自身的
     * 沙类方块标签 {@code #createsandustry:sand}（标签现在也同时包含原版沙子与红沙，见
     * {@code data/createsandustry/tags/block/sand.json}）。前两项仍单独列出，是为了即使数据包用
     * {@code replace:true} 覆盖该标签，原版沙子 / 红沙也一定继续计入。不使用「ID 是否包含 sand」这类名称判断，
     * 也不包含可疑沙子 / 可疑沙砾等具有独立考古机制的方块（它们由“考古次数”统计负责）。</p>
     */
    public static boolean isTrackedSand(BlockState state) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND) || state.is(ModBlockTags.SAND);
    }
}
