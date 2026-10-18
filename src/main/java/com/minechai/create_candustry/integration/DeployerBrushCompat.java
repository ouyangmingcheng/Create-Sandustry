package com.minechai.create_candustry.integration;

import com.minechai.create_candustry.event.ArchaeologyCompletedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;

/**
 * 让 Create 机械手用原版刷子“一次接触完成一次考古”。
 *
 * <p>原版刷掘完全由 {@code BrushItem#onUseTick} 在玩家持续使用物品时驱动，机械手只会调用一次
 * {@code Item#useOn}（进而调用 {@code BrushItem#useOn} → {@code Player#startUsingItem}），之后它的
 * 状态机就返回了，因此机械手既不会推进刷掘进度，也不会扣耐久、生成战利品。这里在
 * {@code BrushItem#useOn} 处拦截，直接执行一次等价的“完成刷掘”。</p>
 *
 * <p>战利品生成复用原版 {@link BrushableBlockEntity#unpackLootTable(Player)}：它读取目标方块
 * <b>自身</b>配置的战利品表（可疑方块会经过沙金工艺已有的全局考古池 Mixin，猫砂仍走自身专属表），
 * 不做任何硬编码随机。解析出的战利品通过机械手假玩家的物品栏回写，与 Create
 * {@code DeployerHandler#safeOnUse} / 实体掉落物捕获所用的
 * {@code Inventory#placeItemBackInInventory} 完全一致，从而进入机械手正常的分拣/溢出/输出流程。</p>
 *
 * <p>本类不引用任何 Create 类：机械手判定使用 NeoForge 的 {@link FakePlayer} 加类名字符串，
 * 因此未安装 Create 时相关判断只会返回 {@code false}，不会造成类加载错误。</p>
 */
public final class DeployerBrushCompat {
    /** Create Deployer 使用的假玩家类名（仅作字符串比较，避免硬依赖 Create）。 */
    private static final String DEPLOYER_FAKE_PLAYER_CLASS =
            "com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer";

    /** 原版刷掘完成时播放的客户端事件编号（与 {@code BrushableBlockEntity#brushingCompleted} 一致）。 */
    private static final int BRUSH_COMPLETED_EVENT = 3008;

    private DeployerBrushCompat() {
    }

    /**
     * 尝试由 Create 机械手一次接触完成一次考古。
     *
     * @return {@code true} 表示已处理（调用方应让 {@code useOn} 直接返回
     *         {@link InteractionResult#CONSUME}，跳过原版 startUsingItem 流程）；否则 {@code false}。
     */
    public static boolean tryCompleteArchaeology(UseOnContext context) {
        Level level = context.getLevel();
        // 只在服务端权威处理。
        if (level.isClientSide) {
            return false;
        }
        Player player = context.getPlayer();
        if (!isCreateDeployer(player)) {
            return false;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BrushableBlock brushableBlock)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof BrushableBlockEntity brushableBlockEntity)) {
            return false;
        }

        // 1) 用目标方块自身的战利品表解析战利品（可疑方块走全局考古池，猫砂走自身专属表）。
        brushableBlockEntity.unpackLootTable(player);
        ItemStack loot = brushableBlockEntity.getItem();
        if (!loot.isEmpty()) {
            // 与 Create 捕获方块掉落物的方式一致：回写进机械手（假玩家）的物品栏，
            // 随后由机械手自身的溢出/输出机制接管，而不是直接散落地面。
            player.getInventory().placeItemBackInInventory(loot.copy());
        }

        // 2) 原版刷掘完成表现 + 方块转换（等价于 BrushableBlockEntity#brushingCompleted，
        //    但战利品已改为回写而非生成世界掉落物实体）。
        level.levelEvent(BRUSH_COMPLETED_EVENT, pos, Block.getId(state));
        level.setBlock(pos, brushableBlock.getTurnsInto().defaultBlockState(), 3);

        // 3) 按原版刷子规则只消耗一次耐久（与 BrushItem#onUseTick 完成时一致）。
        ItemStack brushStack = context.getItemInHand();
        EquipmentSlot slot = brushStack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                ? EquipmentSlot.OFFHAND
                : EquipmentSlot.MAINHAND;
        brushStack.hurtAndBreak(1, player, slot);

        // 4) 一次考古已经真正完成：发出考古完成事件（同一次操作只发一次）。
        //    机械手本身是假玩家，FTB 等兼容模块只接受真实玩家，因此这里解析出可归属的真实玩家。
        ServerPlayer credited = resolveAttributablePlayer(player);
        if (credited != null) {
            NeoForge.EVENT_BUS.post(new ArchaeologyCompletedEvent(credited));
        }

        return true;
    }

    /**
     * 解析本次机械手考古应当计入进度的真实玩家。
     *
     * <p>Create 的 {@code DeployerFakePlayer#getUUID()} 返回其所有者（放置机械手的玩家）的 UUID，
     * 未设置所有者时返回一个固定的回退 UUID。这里用它查找在线的真实玩家：找到则归属本人，
     * 找不到（未设置所有者或所有者不在线）则返回 {@code null}，即不计数——绝不伪造玩家身份，
     * 也不会错误地给附近玩家增加进度。</p>
     */
    private static ServerPlayer resolveAttributablePlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer)) {
            return serverPlayer;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(player.getUUID());
    }

    private static boolean isCreateDeployer(Player player) {
        return player instanceof FakePlayer
                && DEPLOYER_FAKE_PLAYER_CLASS.equals(player.getClass().getName());
    }
}
