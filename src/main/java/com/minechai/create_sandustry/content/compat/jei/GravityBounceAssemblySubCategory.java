package com.minechai.create_sandustry.content.compat.jei;

import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.sequenced.SequencedRecipe;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** A compact slime-bounce animation embedded in Create's original sequenced-assembly category. */
public final class GravityBounceAssemblySubCategory extends SequencedAssemblySubCategory {
    private final ItemStack slime = new ItemStack(Blocks.SLIME_BLOCK);

    public GravityBounceAssemblySubCategory() {
        super(35);
    }

    @Override
    public void draw(SequencedRecipe<?> recipe, GuiGraphics graphics, double mouseX, double mouseY, int index) {
        float phase = (AnimationTickHolder.getRenderTime() % 40) / 40.0F;
        int height = Math.round(24 * 4 * phase * (1 - phase));
        graphics.pose().pushPose();
        graphics.pose().translate(18, 69, 0);
        graphics.pose().scale(1.1F, 1.1F, 1);
        graphics.renderItem(slime, -8, -8);
        graphics.pose().popPose();
        graphics.renderItem(ModItems.INCOMPLETE_GRAVITY_MECHANISM.get().getDefaultInstance(), 10, 43 - height);
    }
}
