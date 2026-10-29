package com.minechai.create_sandustry.mixin.kaleidoscope;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem;
import com.github.ysbbbbbb.kaleidoscopecookery.item.quality.Quality;
import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModDataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils", remap = false)
public abstract class KaleidoscopeQualityUtilsMixin {
    private static final Set<ItemStack> ROLLED_STACKS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    @Inject(method = "setQuality", at = @At("RETURN"))
    private static void createsandustry$applyGoldenSelection(ItemStack stack, Quality quality, CallbackInfo ci) {
        if (stack.isEmpty() || stack.getItem() instanceof RecipeItem) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getRunningThread() != Thread.currentThread()) {
            return;
        }

        if (quality != Quality.SUPERB) {
            stack.remove(ModDataComponents.GOLDEN_SELECTION.get());
            return;
        }

        if (!ModConfigs.KALEIDOSCOPE_GOLDEN_SELECTION_ENABLED.get() || !ROLLED_STACKS.add(stack)) {
            return;
        }

        if (server.overworld().random.nextFloat() < ModConfigs.GOLDEN_SELECTION_CHANCE.get()) {
            stack.set(ModDataComponents.GOLDEN_SELECTION.get(), true);
        }
    }
}