package com.minechai.create_sandustry.mixin.client;

import java.util.Collection;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.minechai.create_sandustry.client.render.CreativeTabSectionRenderer;
import com.minechai.create_sandustry.registry.ModCreativeTabs;
import com.minechai.create_sandustry.registry.ModCreativeTabs.ScreenLayout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    @Shadow private static CreativeModeTab selectedTab;
    @Shadow private float scrollOffs;
    @Unique private ScreenLayout createsandustry$layout;

    @Inject(method = "selectTab", at = @At("HEAD"))
    private void createsandustry$resetLayout(CreativeModeTab tab, CallbackInfo ci) {
        createsandustry$layout = null;
    }

    @ModifyExpressionValue(method = "selectTab", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/CreativeModeTab;getDisplayItems()Ljava/util/Collection;"))
    private Collection<ItemStack> createsandustry$selectLayout(Collection<ItemStack> items) {
        return createsandustry$applyLayout(items);
    }

    @ModifyVariable(method = "refreshCurrentTabContents", at = @At("HEAD"), argsOnly = true)
    private Collection<ItemStack> createsandustry$refreshLayout(Collection<ItemStack> items) {
        return createsandustry$applyLayout(items);
    }

    @Unique
    private Collection<ItemStack> createsandustry$applyLayout(Collection<ItemStack> items) {
        if (selectedTab != ModCreativeTabs.MAIN.get()) return items;
        createsandustry$layout = ModCreativeTabs.createScreenLayout(items);
        return createsandustry$layout.items();
    }

    @Inject(method = "renderLabels", at = @At("TAIL"))
    private void createsandustry$renderSections(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (selectedTab == ModCreativeTabs.MAIN.get() && createsandustry$layout != null) {
            CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
            CreativeTabSectionRenderer.render(graphics, createsandustry$layout, screen.getMenu().items.size(), scrollOffs);
        }
    }
}
