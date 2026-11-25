package com.minechai.create_sandustry.content.thermalbuffer;

import java.util.List;
import java.util.Locale;

import com.minechai.create_sandustry.content.thermalbuffer.ThermalBufferBlockEntity;
import com.minechai.create_sandustry.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** 热能缓冲器物品，显示随物品一起保存的连续热值。 */
public class ThermalBufferBlockItem extends BlockItem {
    public ThermalBufferBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        double storedHeat = stack.getOrDefault(ModDataComponents.THERMAL_HEAT.get(), 0.0D);
        storedHeat = Math.max(0.0D, Math.min(ThermalBufferBlockEntity.MAX_HEAT, storedHeat));

        tooltipComponents.add(Component.translatable(
                        "tooltip.createsandustry.thermal_relay.stored_heat",
                        String.format(Locale.ROOT, "%.1f", storedHeat),
                        ThermalBufferBlockEntity.MAX_HEAT)
                .withStyle(ChatFormatting.GRAY));

        if (storedHeat > 0.0D) {
            tooltipComponents.add(Component.translatable(
                            "tooltip.createsandustry.thermal_relay.level",
                            ThermalBufferBlockEntity.getRawLevel(storedHeat))
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
