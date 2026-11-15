package com.minechai.create_sandustry.content.item;

import com.minechai.create_sandustry.content.sandheart.SandHeartManager;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 沙之心：右键使用后立刻增加一颗完整沙心。
 *
 * <p>沙心系统的内部单位是半颗沙心，因此这里一次增加
 * {@link SandHeartManager#UNITS_PER_HEART} 个单位。容量不足一颗完整沙心时拒绝使用，
 * 不消耗物品；已经填满生命条、处于沙化之兆时同样无法继续使用。</p>
 */
public final class SandHeartItem extends Item {
    public static final int HEARTS_PER_USE = 1;

    public SandHeartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        int unitsToAdd = HEARTS_PER_USE * SandHeartManager.UNITS_PER_HEART;
        int availableUnits = SandHeartManager.capacityUnits(player) - SandHeartManager.getUnits(player);
        if (availableUnits < unitsToAdd) {
            return InteractionResultHolder.fail(stack);
        }

        SandHeartManager.addUnits(player, unitsToAdd);
        stack.consume(HEARTS_PER_USE, player);
        return InteractionResultHolder.consume(stack);
    }
}
