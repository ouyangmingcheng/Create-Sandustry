package com.minechai.create_sandustry.content.event;

import com.minechai.create_sandustry.api.florinol.FlorinolPoweredTool;
import com.minechai.create_sandustry.api.florinol.FlorinolToolBinding;
import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;
import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergySnapshot;
import com.minechai.create_sandustry.content.block.entity.FlorinolBatteryBlockEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Common binding interactions and tooltip for Florinol-powered tools. */
public final class FlorinolToolBindingEvents {
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!FlorinolToolEnergy.isBindingCapable(stack)) {
            return;
        }
        if (player.isShiftKeyDown()) {
            if (FlorinolToolEnergy.unbind(stack)) {
                player.displayClientMessage(
                        Component.translatable("message.createsandustry.florinol_binding.unbound"), true);
                event.setCanceled(true);
            }
            return;
        }
        if (stack.getItem() instanceof FlorinolPoweredTool tool
                && !tool.canBindFlorinolBattery(stack)) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getBlockEntity(event.getPos()) instanceof FlorinolBatteryBlockEntity battery)) {
            return;
        }
        if (!battery.isAssembled()) {
            player.displayClientMessage(
                    Component.translatable("message.createsandustry.florinol_binding.inactive"), true);
            event.setCanceled(true);
            return;
        }

        FlorinolToolEnergy.bind(stack, level, battery);
        player.displayClientMessage(Component.translatable(
                "message.createsandustry.florinol_binding.bound",
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()), true);
        event.setCanceled(true);
    }

    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || event.getHand() != InteractionHand.MAIN_HAND
                || !player.isShiftKeyDown()) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (FlorinolToolEnergy.isBindingCapable(stack) && FlorinolToolEnergy.unbind(stack)) {
            player.displayClientMessage(
                    Component.translatable("message.createsandustry.florinol_binding.unbound"), true);
            event.setCanceled(true);
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        for (ItemStack stack : player.getInventory().items) {
            refreshBindingSnapshot(player, stack);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            refreshBindingSnapshot(player, stack);
        }
        for (ItemStack stack : player.getInventory().armor) {
            refreshBindingSnapshot(player, stack);
        }
    }

    private static void refreshBindingSnapshot(ServerPlayer player, ItemStack stack) {
        if (!stack.isEmpty() && FlorinolToolEnergy.isBindingCapable(stack)
                && FlorinolToolEnergy.hasBinding(stack)) {
            FlorinolToolEnergy.refreshEnergySnapshot(player.serverLevel(), stack);
        }
    }
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!FlorinolToolEnergy.isBindingCapable(stack)) {
            return;
        }
        FlorinolToolBinding binding = FlorinolToolEnergy.getBinding(stack);
        if (binding == null) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.createsandustry.florinol_binding.unbound_hint").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        removeFluidTooltipLines(event);
        FlorinolToolEnergySnapshot snapshot = FlorinolToolEnergy.getEnergySnapshot(stack);
        if (snapshot != null && snapshot.available()) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.createsandustry.florinol_binding.energy",
                    format(snapshot.stored()), format(snapshot.capacity()))
                    .withStyle(ChatFormatting.AQUA));
        } else {
            event.getToolTip().add(Component.translatable(
                    "tooltip.createsandustry.florinol_binding.energy_unloaded")
                    .withStyle(ChatFormatting.RED));
        }
        event.getToolTip().add(Component.translatable(
                "tooltip.createsandustry.florinol_binding.bound",
                binding.pos().getX(), binding.pos().getY(), binding.pos().getZ())
                .withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable(
                "tooltip.createsandustry.florinol_binding.dimension",
                binding.dimension().toString()).withStyle(ChatFormatting.GRAY));
        int cost = FlorinolToolEnergy.getOperationCost(stack);
        if (cost > 0) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.createsandustry.florinol_binding.cost", cost)
                    .withStyle(ChatFormatting.GRAY));
        }
        event.getToolTip().add(Component.translatable(
                "tooltip.createsandustry.florinol_binding.unbind_hint").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void removeFluidTooltipLines(ItemTooltipEvent event) {
        String fuel = Component.translatable("item_desc.fuel").getString();
        String water = Component.translatable("item_desc.water").getString();
        event.getToolTip().removeIf(line -> {
            String text = line.getString();
            return text.startsWith(fuel) || text.startsWith(water);
        });
    }

    private static String format(int value) {
        return String.format("%,d", value);
    }
    private FlorinolToolBindingEvents() {
    }
}