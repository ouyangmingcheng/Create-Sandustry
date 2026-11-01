package com.minechai.create_sandustry.mixin.createstuffadditions;

import com.minechai.create_sandustry.content.compat.createstuffadditions.BrassArmorFlorinolCompat;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces brass exoskeleton fluid checks and drains with FE while it is bound. */
@Pseudo
@Mixin(targets = "net.mcreator.createstuffadditions.procedures.BrassExoskeletonBodyTickEventProcedure", remap = false)
public abstract class BrassExoskeletonFlorinolMixin {
    @Redirect(method = "execute", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/capability/IFluidHandlerItem;getFluidInTank(I)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private static FluidStack createsandustry$feFluidCheck(IFluidHandlerItem handler, int tank,
                                                            LevelAccessor world, double x, double y,
                                                            double z, Entity entity, ItemStack stack) {
        return BrassArmorFlorinolCompat.redirectFluid(handler, tank, world, stack);
    }

    @Redirect(method = "execute", at = @At(value = "INVOKE",
            target = "Lnet/mcreator/createstuffadditions/item/CustomFluidHandlerItemStack;drainTank(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/FluidStack;Z)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private static FluidStack createsandustry$feDrain(ItemStack stack, FluidStack fluid,
                                                      boolean simulate, LevelAccessor world,
                                                      double x, double y, double z, Entity entity,
                                                      ItemStack itemstack) {
        return BrassArmorFlorinolCompat.redirectDrain(stack, fluid, simulate, world);
    }
}