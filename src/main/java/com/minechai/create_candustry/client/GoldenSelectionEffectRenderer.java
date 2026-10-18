package com.minechai.create_candustry.client;

import com.minechai.create_candustry.effect.ModMobEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/**
 * Renders the Golden Selection status effect icon using the vanilla gold nugget
 * item texture, avoiding any copied texture file.
 */
public final class GoldenSelectionEffectRenderer implements IClientMobEffectExtensions {
    private static final ResourceLocation GOLD_NUGGET_SPRITE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "item/gold_nugget");

    private static TextureAtlasSprite goldNuggetSprite() {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(GOLD_NUGGET_SPRITE);
    }

    @Override
    public boolean renderInventoryIcon(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen,
                                       GuiGraphics guiGraphics, int x, int y, int blitOffset) {
        guiGraphics.blit(x, y + 7, blitOffset, 18, 18, goldNuggetSprite());
        return true;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphics guiGraphics, int x, int y, float z, float alpha) {
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(x + 3, y + 3, 0, 18, 18, goldNuggetSprite());
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        return true;
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterClientExtensionsEvent.class, event ->
                event.registerMobEffect(new GoldenSelectionEffectRenderer(), ModMobEffects.GOLDEN_SELECTION.get()));
    }

    private GoldenSelectionEffectRenderer() {
    }
}
