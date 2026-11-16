package com.minechai.create_sandustry.api.florinol;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.ToIntFunction;

import com.minechai.create_sandustry.content.florinolbattery.FlorinolBatteryBlockEntity;
import com.minechai.create_sandustry.registry.ModDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/**
 * Public binding and energy API for tools powered by a Florinol battery.
 *
 * <p>Bindings store dimension, controller position, and the controller's persistent
 * UUID. No distance check is performed. A battery is only usable while its chunk and
 * block entity are loaded; when it unloads, the tool temporarily stops working.</p>
 */
public final class FlorinolToolEnergy {
    /** Create: Stuff 'N Additions portable drill registry name. */
    public static final ResourceLocation PORTABLE_DRILL_ID =
            ResourceLocation.fromNamespaceAndPath("create_sa", "portable_drill");

    private static final Map<Item, ToIntFunction<ItemStack>> EXTERNAL_COST_PROVIDERS =
            new IdentityHashMap<>();

    /** Standard upstream fuel operation used by Create SA equipment. */
    public static final int FUEL_MB_PER_OPERATION = 10;
    /** Unified conversion used by every Florinol-powered Create SA item. */
    public static final int FE_PER_10_MB_FUEL = 100;

    /** Converts an upstream fuel amount to the unified FE cost. */
    public static int getFeCostForFuelMb(int fuelMb) {
        return Math.max(0, fuelMb) * FE_PER_10_MB_FUEL / FUEL_MB_PER_OPERATION;
    }

    public static int getStandardOperationCost() {
        return getFeCostForFuelMb(FUEL_MB_PER_OPERATION);
    }

    /** Upstream portable drill consumes 10 mB of fuel per broken block. */
    public static final int PORTABLE_DRILL_FUEL_MB_PER_OPERATION = FUEL_MB_PER_OPERATION;

    public static boolean isPortableDrill(ItemStack stack) {
        return PORTABLE_DRILL_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /**
     * Registers a non-inheritable item (for example, an item from another mod) as
     * Florinol-bindable. Call during common setup after items are registered.
     */
    public static void registerExternalTool(Item item, ToIntFunction<ItemStack> costProvider) {
        if (item != null && costProvider != null) {
            EXTERNAL_COST_PROVIDERS.put(item, costProvider);
        }
    }

    public static boolean isBindingCapable(ItemStack stack) {
        return isPortableDrill(stack)
                || stack.getItem() instanceof FlorinolPoweredTool
                || EXTERNAL_COST_PROVIDERS.containsKey(stack.getItem());
    }

    /** Returns the FE cost for one operation, or 0 when the item is not supported. */
    public static int getOperationCost(ItemStack stack) {
        if (stack.getItem() instanceof FlorinolPoweredTool tool) {
            return Math.max(0, tool.getFlorinolFeCostPerOperation(stack));
        }
        ToIntFunction<ItemStack> externalCost = EXTERNAL_COST_PROVIDERS.get(stack.getItem());
        if (externalCost != null) {
            return Math.max(0, externalCost.applyAsInt(stack));
        }
        if (isPortableDrill(stack)) {
            return getFeCostForFuelMb(PORTABLE_DRILL_FUEL_MB_PER_OPERATION);
        }
        return 0;
    }

    public static FlorinolToolBinding getBinding(ItemStack stack) {
        return stack.get(ModDataComponents.FLORINOL_TOOL_BINDING.get());
    }

    public static boolean hasBinding(ItemStack stack) {
        return getBinding(stack) != null;
    }

    public static void bind(ItemStack stack, ServerLevel batteryLevel,
                            FlorinolBatteryBlockEntity battery) {
        stack.set(ModDataComponents.FLORINOL_TOOL_BINDING.get(),
                new FlorinolToolBinding(batteryLevel.dimension().location(),
                        battery.getBlockPos(), battery.getBatteryId()));
        // FE mode must not satisfy legacy tagFuel/tagWater checks.
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putDouble("tagFuel", 0.0D);
            tag.putDouble("tagWater", 0.0D);
        });
        refreshEnergySnapshot(batteryLevel, stack);
    }

    public static boolean unbind(ItemStack stack) {
        if (!hasBinding(stack)) {
            return false;
        }
        stack.remove(ModDataComponents.FLORINOL_TOOL_BINDING.get());
        stack.remove(ModDataComponents.FLORINOL_TOOL_ENERGY.get());
        return true;
    }

    /** Resolves the bound controller if its dimension and chunk are currently loaded. */
    public static FlorinolBatteryBlockEntity getBoundBattery(Level toolLevel, ItemStack stack) {
        FlorinolToolBinding binding = getBinding(stack);
        if (binding == null || toolLevel.getServer() == null) {
            return null;
        }
        MinecraftServer server = toolLevel.getServer();
        ServerLevel batteryLevel = server.getLevel(binding.dimensionKey());
        if (batteryLevel == null || !batteryLevel.hasChunkAt(binding.pos())) {
            return null;
        }
        if (!(batteryLevel.getBlockEntity(binding.pos()) instanceof FlorinolBatteryBlockEntity battery)) {
            return null;
        }
        return battery.getBatteryId().equals(binding.batteryId()) ? battery : null;
    }

    public static FlorinolToolEnergySnapshot getEnergySnapshot(ItemStack stack) {
        return stack.get(ModDataComponents.FLORINOL_TOOL_ENERGY.get());
    }

    /** Refreshes the synchronized tooltip/bar snapshot from the bound battery. */
    public static void refreshEnergySnapshot(Level toolLevel, ItemStack stack) {
        FlorinolBatteryBlockEntity battery = getBoundBattery(toolLevel, stack);
        FlorinolToolEnergySnapshot previous = getEnergySnapshot(stack);
        FlorinolToolEnergySnapshot next;
        if (battery == null) {
            int capacity = previous == null ? 0 : previous.capacity();
            next = new FlorinolToolEnergySnapshot(0, capacity, false);
        } else {
            next = new FlorinolToolEnergySnapshot(battery.getEnergyStored(),
                    battery.getMaxEnergyStored(), battery.isAssembled());
        }
        if (!next.equals(previous)) {
            stack.set(ModDataComponents.FLORINOL_TOOL_ENERGY.get(), next);
        }
    }

    public static int getAvailableEnergy(Level toolLevel, ItemStack stack) {
        FlorinolBatteryBlockEntity battery = getBoundBattery(toolLevel, stack);
        return battery != null && battery.isAssembled() ? battery.getEnergyStored() : 0;
    }

    public static boolean canConsume(Level toolLevel, ItemStack stack, int feAmount) {
        if (feAmount <= 0) {
            return true;
        }
        FlorinolBatteryBlockEntity battery = getBoundBattery(toolLevel, stack);
        return battery != null && battery.isAssembled()
                && battery.extractEnergyPrecise(feAmount, true) == feAmount;
    }

    /** Atomically consumes the requested FE from the bound battery. */
    public static boolean tryConsume(Level toolLevel, ItemStack stack, int feAmount) {
        if (feAmount <= 0) {
            return true;
        }
        FlorinolBatteryBlockEntity battery = getBoundBattery(toolLevel, stack);
        if (battery == null || !battery.isAssembled()
                || battery.extractEnergyPrecise(feAmount, true) != feAmount) {
            return false;
        }
        boolean consumed = battery.extractEnergyPrecise(feAmount, false) == feAmount;
        if (consumed) {
            refreshEnergySnapshot(toolLevel, stack);
        }
        return consumed;
    }

    private FlorinolToolEnergy() {
    }
}