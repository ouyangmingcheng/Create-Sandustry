package com.minechai.create_sandustry.compat.createstuffadditions;

import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergy;
import com.minechai.create_sandustry.api.florinol.FlorinolToolEnergySnapshot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/** FE mode for Create SA brass/netherite jetpack and exoskeleton chestplates. */
public final class BrassArmorFlorinolCompat {
    public static final ResourceLocation BRASS_JETPACK_ID =
            ResourceLocation.fromNamespaceAndPath("create_sa", "brass_jetpack_chestplate");
    public static final ResourceLocation BRASS_EXOSKELETON_ID =
            ResourceLocation.fromNamespaceAndPath("create_sa", "brass_exoskeleton_chestplate");
    public static final ResourceLocation NETHERITE_JETPACK_ID =
            ResourceLocation.fromNamespaceAndPath("create_sa", "netherite_jetpack_chestplate");
    public static final ResourceLocation NETHERITE_EXOSKELETON_ID =
            ResourceLocation.fromNamespaceAndPath("create_sa", "netherite_exoskeleton_chestplate");

    /** Each upstream armor work interval consumes 10 mB of fuel. */
    public static final int FUEL_MB_PER_INTERVAL = 10;

    public static void registerTools() {
        registerTool(BRASS_JETPACK_ID);
        registerTool(BRASS_EXOSKELETON_ID);
        registerTool(NETHERITE_JETPACK_ID);
        registerTool(NETHERITE_EXOSKELETON_ID);
    }

    private static void registerTool(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item != null && item != Items.AIR) {
            FlorinolToolEnergy.registerExternalTool(item, BrassArmorFlorinolCompat::getFeCost);
        }
    }

    public static int getFeCost(ItemStack stack) {
        return FlorinolToolEnergy.getFeCostForFuelMb(FUEL_MB_PER_INTERVAL);
    }

    public static boolean isSupportedArmor(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return BRASS_JETPACK_ID.equals(id) || BRASS_EXOSKELETON_ID.equals(id)
                || NETHERITE_JETPACK_ID.equals(id) || NETHERITE_EXOSKELETON_ID.equals(id);
    }

    public static boolean canOperate(LevelAccessor world, ItemStack stack) {
        if (!isSupportedArmor(stack) || !FlorinolToolEnergy.hasBinding(stack)) {
            return false;
        }
        int cost = getFeCost(stack);
        if (world.isClientSide()) {
            FlorinolToolEnergySnapshot snapshot = FlorinolToolEnergy.getEnergySnapshot(stack);
            return snapshot != null && snapshot.available() && snapshot.stored() >= cost;
        }
        return world instanceof Level level && FlorinolToolEnergy.canConsume(level, stack, cost);
    }

    public static FluidStack redirectFluid(IFluidHandlerItem handler, int tank,
                                           LevelAccessor world, ItemStack stack) {
        if (canOperate(world, stack)) {
            Fluid fluid = tank == 0 ? Fluids.LAVA : Fluids.WATER;
            return new FluidStack(fluid, 1);
        }
        return handler.getFluidInTank(tank);
    }

    public static FluidStack redirectDrain(ItemStack stack, FluidStack fluid, boolean simulate,
                                           LevelAccessor world) {
        if (!isSupportedArmor(stack) || !FlorinolToolEnergy.hasBinding(stack)) {
            return drainFluid(stack, fluid, simulate);
        }
        if (world.isClientSide()) {
            return fluid.copy();
        }
        if (fluid.getFluid() == Fluids.WATER && world instanceof Level level) {
            return FlorinolToolEnergy.tryConsume(level, stack, getFeCost(stack))
                    ? fluid.copy()
                    : FluidStack.EMPTY;
        }
        // The second upstream drain is the lava tank; FE mode replaces it entirely.
        return fluid.copy();
    }

    private static FluidStack drainFluid(ItemStack stack, FluidStack fluid, boolean simulate) {
        IFluidHandlerItem handler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return FluidStack.EMPTY;
        }
        return handler.drain(fluid, simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE);
    }

    public static void onCriticalHit(CriticalHitEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!BRASS_EXOSKELETON_ID.equals(BuiltInRegistries.ITEM.getKey(chest.getItem()))
                || !player.getMainHandItem().isEmpty()
                || !canOperate(player.serverLevel(), chest)) {
            return;
        }
        CustomData customData = chest.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        if (customData.copyTag().getDouble("tagFuel") > 0.0D
                && customData.copyTag().getDouble("tagWater") > 0.0D) {
            return; // Upstream will apply the same hit effect for a legacy fluid-powered chestplate.
        }

        Vec3 look = player.getLookAngle();
        event.getTarget().setDeltaMovement(event.getTarget().getDeltaMovement().add(look.scale(0.5D)));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE,
                SoundSource.NEUTRAL, 0.25F, 3.0F);
    }

    private BrassArmorFlorinolCompat() {
    }
}