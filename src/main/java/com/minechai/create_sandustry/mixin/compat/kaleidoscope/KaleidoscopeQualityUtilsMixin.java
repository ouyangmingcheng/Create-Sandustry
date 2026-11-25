package com.minechai.create_sandustry.mixin.compat.kaleidoscope;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModDataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopecookery.item.quality.QualityUtils", remap = false)
public abstract class KaleidoscopeQualityUtilsMixin {
    @Unique
    private static final Set<ItemStack> ROLLED_STACKS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    @Inject(method = "setQuality(Lnet/minecraft/world/item/ItemStack;Lcom/github/ysbbbbbb/kaleidoscopecookery/item/quality/Quality;)V", at = @At("RETURN"))
    private static void createsandustry$applyGoldenSelection(ItemStack stack, @Coerce Object quality, CallbackInfo ci) {
        if (stack.isEmpty() || createsandustry$isRecipeItem(stack)) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getRunningThread() != Thread.currentThread()) {
            return;
        }

        if (!(quality instanceof Enum<?> value && "SUPERB".equals(value.name()))) {
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
    /** Keep this optional integration loadable without Cookery on the compile classpath. */
    @Unique
    private static boolean createsandustry$isRecipeItem(ItemStack stack) {
        for (Class<?> type = stack.getItem().getClass(); type != null; type = type.getSuperclass()) {
            if ("com.github.ysbbbbbb.kaleidoscopecookery.item.RecipeItem".equals(type.getName())) {
                return true;
            }
        }
        return false;
    }
}