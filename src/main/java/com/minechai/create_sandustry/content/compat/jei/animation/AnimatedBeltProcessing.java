package com.minechai.create_sandustry.content.compat.jei.animation;

import com.minechai.create_sandustry.content.client.event.SiftingBeltClientEvents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlope;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * JEI-only pseudo-2D side section of a cased Create belt.
 *
 * <p>The models are viewed straight along their side; only a 90 degree Y
 * rotation is used to turn the belt from local +Z into screen-space horizontal.
 * No pitch or yaw is applied, so the result reads as a flat cross-section rather
 * than a perspective diorama.</p>
 */
public final class AnimatedBeltProcessing extends AnimatedKinetics {
    private static final int SCALE = 20;
    private static final int ITEM_SIZE = 12;
    private static final double BELT_SURFACE_Y = 13.0D / 16.0D;
    private static final double BELT_RENDER_Y_OFFSET = 1.0D / 256.0D;
    private static final double START_Z = -1.5D;
    private static final double MIDDLE_Z = -0.5D;
    private static final double END_Z = 0.5D;
    private static final double START_PULLEY_Z = START_Z;
    private static final double END_PULLEY_Z = END_Z;
    private static final double ITEM_TRAVEL_MIN_Z = -1.35D;
    private static final double ITEM_TRAVEL_MAX_Z = 1.35D;
    private static final float ITEM_CYCLE_TICKS = 110.0F;

    /*
     * Use baked BlockStates instead of ad-hoc PartialModel.of(...) instances.
     * The casing family is not exposed through AllPartialModels; creating a
     * PartialModel after model registration produces a missing-model placeholder.
     */
    private static final BlockState BRASS_CASING_START = casing(BeltPart.START);
    private static final BlockState BRASS_CASING_MIDDLE = casing(BeltPart.MIDDLE);
    private static final BlockState BRASS_CASING_END = casing(BeltPart.END);

    private static final PartialModel BELT_START = AllPartialModels.BELT_START;
    private static final PartialModel BELT_MIDDLE = AllPartialModels.BELT_MIDDLE;
    private static final PartialModel BELT_END = AllPartialModels.BELT_END;
    private static final PartialModel BELT_PULLEY = AllPartialModels.BELT_PULLEY;

    @Override
    public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
        draw(graphics, xOffset, yOffset, ItemStack.EMPTY);
    }

    /**
     * Draws the belt and, when present, one input stack moving along the belt.
     * The stack is display-only and is never mutated, consumed, or inserted into
     * a level.
     */
    public void draw(GuiGraphics graphics, int xOffset, int yOffset, ItemStack input) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(xOffset, yOffset, 200.0F);
            // The model is authored along +Z. Side-on view + Y rotation gives a
            // pseudo-2D horizontal cross-section without perspective distortion.
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));

            renderCasing(graphics, BRASS_CASING_START, START_Z);
            renderCasing(graphics, BRASS_CASING_MIDDLE, MIDDLE_Z);
            renderCasing(graphics, BRASS_CASING_END, END_Z);
            renderChuteMouth(graphics, START_Z);
            renderChuteMouth(graphics, MIDDLE_Z);
            renderChuteMouth(graphics, END_Z);

            renderPulley(graphics, START_PULLEY_Z);
            renderPulley(graphics, END_PULLEY_Z);

            renderBelt(graphics, BELT_START, START_Z);
            renderBelt(graphics, BELT_MIDDLE, MIDDLE_Z);
            renderBelt(graphics, BELT_END, END_Z);
        } finally {
            poseStack.popPose();
        }

        if (!input.isEmpty()) {
            renderInputItem(graphics, input, xOffset, yOffset);
        }
    }

    private void renderCasing(GuiGraphics graphics, BlockState casing, double z) {
        blockElement(casing)
                .atLocal(0.0D, 0.0D, z)
                .scale(SCALE)
                .render(graphics);
    }

    private void renderBelt(GuiGraphics graphics, PartialModel belt, double z) {
        blockElement(belt)
                .atLocal(0.0D, BELT_RENDER_Y_OFFSET, z)
                .scale(SCALE)
                .render(graphics);
    }

    private void renderPulley(GuiGraphics graphics, double z) {
        // Spin around the pulley authoring axis first, then rotate that axle
        // across the belt. This keeps the shaft direction fixed.
        blockElement(BELT_PULLEY)
                .rotateBlock(0.0D, -getCurrentAngle(), 90.0D)
                .atLocal(0.0D, 0.0D, z)
                .scale(SCALE)
                .render(graphics);
    }

    private void renderChuteMouth(GuiGraphics graphics, double z) {
        blockElement(SiftingBeltClientEvents.CHUTE_MOUTH)
                .atLocal(0.0D, 0.0D, z)
                .scale(SCALE)
                .render(graphics);
    }

    private void renderInputItem(GuiGraphics graphics, ItemStack input, int xOffset, int yOffset) {
        float cycle = (AnimationTickHolder.getRenderTime() % ITEM_CYCLE_TICKS) / ITEM_CYCLE_TICKS;
        double beltZ = ITEM_TRAVEL_MIN_Z + cycle * (ITEM_TRAVEL_MAX_Z - ITEM_TRAVEL_MIN_Z);

        int itemX = xOffset + (int) Math.round(beltZ * SCALE) - ITEM_SIZE / 2;
        int itemY = yOffset - (int) Math.ceil((BELT_SURFACE_Y + BELT_RENDER_Y_OFFSET) * SCALE)
                - ITEM_SIZE + 1;

        BakedModel model = Minecraft.getInstance().getItemRenderer()
                .getModel(input, null, null, 0);
        TextureAtlasSprite sprite = model.getParticleIcon();

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        try {
            poseStack.translate(itemX, itemY, 200.0F);
            // Particle icons are flat sprites. This deliberately avoids the
            // isometric block-item model for block inputs such as wet sand.
            graphics.blit(0, 0, 0, ITEM_SIZE, ITEM_SIZE, sprite);
        } finally {
            poseStack.popPose();
        }
    }

    private static BlockState casing(BeltPart part) {
        return AllBlocks.BELT.getDefaultState()
                .setValue(BeltBlock.SLOPE, BeltSlope.HORIZONTAL)
                .setValue(BeltBlock.PART, part)
                .setValue(BeltBlock.CASING, true)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
    }
}
