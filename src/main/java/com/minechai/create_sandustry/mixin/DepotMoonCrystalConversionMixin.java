package com.minechai.create_sandustry.mixin;

import com.minechai.create_sandustry.content.mooncrystal.MoonCrystalConversion;
import com.minechai.create_sandustry.content.mooncrystal.MoonCrystalConversionCache;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModStats;
import com.simibubi.create.content.logistics.depot.DepotBehaviour;
import com.simibubi.create.content.logistics.depot.DepotItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DepotItemHandler.class)
public abstract class DepotMoonCrystalConversionMixin {
    @Shadow
    private DepotBehaviour behaviour;

    @Unique
    private MoonCrystalConversionCache createsandustry$moonConversionCache = new MoonCrystalConversionCache();

    @Inject(method = "extractItem", at = @At("RETURN"), cancellable = true)
    private void createsandustry$convertMoonCrystal(int slot, int amount, boolean simulate,
                                                    CallbackInfoReturnable<ItemStack> cir) {
        ItemStack extracted = cir.getReturnValue();
        if (slot != 0 || extracted.isEmpty()) {
            return;
        }

        Level level = behaviour.blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BlockPos pos = behaviour.blockEntity.getBlockPos();
        BlockState state = behaviour.blockEntity.getBlockState();
        if (!MoonCrystalConversion.canConvert(level, pos, state, extracted)) {
            createsandustry$moonConversionCache.clear();
            return;
        }

        ItemStack result = createsandustry$moonConversionCache.resolve(level, extracted);
        if (!simulate && extracted.getCount() > 1) {
            ItemStack remainder = ((IItemHandler) (Object) this).insertItem(
                    0,
                    extracted.copyWithCount(extracted.getCount() - 1),
                    false);
            if (!remainder.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5D, pos.getY() + 1.0D,
                        pos.getZ() + 0.5D, remainder);
            }
        }
        cir.setReturnValue(result.copyWithCount(1));
        if (!simulate) {
            if (result.is(ModItems.MOONCRYSTAL.get()) && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                ModStats.awardMoonCrystalConversion(serverLevel, pos);
            }
            createsandustry$moonConversionCache.clear();
        }
    }
}
