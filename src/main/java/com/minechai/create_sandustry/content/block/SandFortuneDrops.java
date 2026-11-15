package com.minechai.create_sandustry.content.block;

import com.minechai.create_sandustry.registry.ModBlockTags;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 时运挖掘处理：玩家用带「时运」附魔的工具挖掘沙子类方块时的两项额外规则。
 *
 * <ol>
 *     <li><b>概率用沙团替换普通沙子掉落</b>：每次挖掘只判定一次，时运 I → 10%、II → 20%、
 *         III 及以上 → 30%。命中时将首组 {@code minecraft:sand} / {@code minecraft:red_sand}
 *         掉落替换为 1 个 {@code createsandustry:sand_ball}；未命中时保留原掉落。
 *         其它既有掉落（精准采集掉方块本身、其它附加掉落）保持不变。没有时运时完全不介入。</li>
 *     <li><b>额外金粒</b>：时运 I → 0~3、II → 0~6、III 及以上 → 1~9（均匀整数，含两端）。</li>
 * </ol>
 *
 * <p>取用 NeoForge 的 {@link BlockDropsEvent}：它在方块<b>已被破坏</b>、战利品已确定但尚未加入世界时
 * 触发，并携带真正的破坏者、实际使用的工具以及可修改的掉落列表。因此只在服务端、只在玩家直接
 * 破坏时生效；爆炸、活塞、下落失败、刷子刷掘完成（{@code setBlock}）等路径破坏者为 {@code null}，
 * 会被跳过；一次破坏只触发一次事件 → 每项规则每次挖掘最多生效一次。</p>
 *
 * <p><b>猫砂</b>{@code createsandustry:cat_litter} 虽然属于沙子标签，但本类对它直接返回：
 * 既不产生额外金粒（恒为 0），也不把它的考古掉落替换成沙团；它的直接破坏猫叫、刷掘战利品、
 * 四阶段纹理与刷掘完成转沙子的机制全部保持不变。</p>
 */
public final class SandFortuneDrops {
    private SandFortuneDrops() {
    }

    /** 玩家有效挖掘沙子类方块时的附加掉落与替换。 */
    public static void onBlockDrops(BlockDropsEvent event) {
        if (event.isCanceled()) {
            return;
        }
        // 只处理玩家直接破坏：破坏者为真实玩家（排除机械手等假玩家）。
        if (!(event.getBreaker() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        ServerLevel level = event.getLevel();
        // 与沙子原有掉落保持一致：doTileDrops 关闭时不产生任何掉落物。
        if (!level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) || level.restoringBlockSnapshots) {
            return;
        }
        BlockState state = event.getState();
        if (!state.is(ModBlockTags.SAND)) {
            return;
        }
        // 猫砂：不参与本机制（无额外金粒，也不替换其考古掉落）。
        if (state.is(ModBlocks.CAT_LITTER.get())) {
            return;
        }

        int fortune = fortuneLevel(level, event.getTool());
        if (fortune <= 0) {
            // 没有时运：维持原有掉落行为。
            return;
        }

        // 1) 按时运等级概率将普通沙子 / 红沙掉落替换为 1 个沙团。
        replaceSandDrops(event.getDrops(), level.getRandom(), fortune);

        // 2) 额外金粒。
        int count = nuggetCount(level.getRandom(), fortune);
        if (count > 0) {
            event.getDrops().add(createDrop(level, event.getPos(), new ItemStack(Items.GOLD_NUGGET, count)));
        }
    }

    /** 每次挖掘最多抽取一次，按 10% / 20% / 30% 概率把首组沙子 / 红沙替换为 1 个沙团。 */
    private static void replaceSandDrops(java.util.List<ItemEntity> drops, RandomSource random, int fortuneLevel) {
        for (ItemEntity drop : drops) {
            ItemStack stack = drop.getItem();
            if (stack.is(Items.SAND) || stack.is(Items.RED_SAND)) {
                if (random.nextInt(10) < Math.min(fortuneLevel, 3)) {
                    drop.setItem(new ItemStack(ModItems.SAND_BALL.get(), 1));
                }
                return;
            }
        }
    }

    /** 与原版 {@code Block.popResource} 相同的落点算法，让附加掉落像普通掉落一样出现。 */
    private static ItemEntity createDrop(ServerLevel level, BlockPos pos, ItemStack stack) {
        double yOffset = EntityType.ITEM.getHeight() / 2.0;
        double xzOffset = EntityType.ITEM.getWidth() / 2.0;
        ItemEntity itemEntity = new ItemEntity(level,
                pos.getX() + 0.5 - xzOffset,
                pos.getY() + 0.5 - yOffset,
                pos.getZ() + 0.5 - xzOffset,
                stack);
        itemEntity.setDefaultPickUpDelay();
        return itemEntity;
    }

    /** 读取工具上的原版「时运」附魔等级；没有附魔或工具为空时返回 0。 */
    public static int fortuneLevel(Level level, ItemStack tool) {
        if (tool.isEmpty()) {
            return 0;
        }
        Optional<Holder.Reference<Enchantment>> fortune = level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(Enchantments.FORTUNE);
        return fortune.map(tool::getEnchantmentLevel).orElse(0);
    }

    /**
     * 按时运等级抽取额外金粒数量（均匀分布整数，含两端）：
     * I → {@code [0,3]}，II → {@code [0,6]}，III 及以上 → {@code [1,9]}，其它 → 0。
     */
    public static int nuggetCount(RandomSource random, int fortuneLevel) {
        if (fortuneLevel <= 0) {
            return 0;
        }
        if (fortuneLevel == 1) {
            return random.nextInt(4);
        }
        if (fortuneLevel == 2) {
            return random.nextInt(7);
        }
        // 时运 III 及更高等级统一按 III 级规则，避免越界。
        return 1 + random.nextInt(9);
    }
}
