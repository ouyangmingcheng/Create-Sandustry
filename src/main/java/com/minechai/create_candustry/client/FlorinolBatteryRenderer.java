package com.minechai.create_candustry.client;

import com.minechai.create_candustry.CreateSandustry;
import com.minechai.create_candustry.block.entity.FlorinolBatteryBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;

import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.platform.NeoForgeCatnipServices;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.FluidRenderHelper;
import net.createmod.catnip.render.PonderRenderTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Direct port of Create's FluidTankRenderer.  No tank geometry, liquid-level
 * interpolation, boiler rendering or render bounding-box logic is redesigned.
 */
public class FlorinolBatteryRenderer extends SafeBlockEntityRenderer<FlorinolBatteryBlockEntity> {
    public FlorinolBatteryRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    protected void renderSafe(FlorinolBatteryBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        if (!be.isController()) {
            return;
        }
        boolean window = be.getExtraData() instanceof Boolean windowValue && windowValue;
        if (!window) {
            if (be.boiler.isActive()) {
                renderAsBoiler(be, partialTicks, ms, buffer, light, overlay);
            }
            return;
        }

        float capHeight = 1 / 4f;
        float tankHullWidth = 1 / 16f + 1 / 128f;
        float minPuddleHeight = 1 / 16f;
        float totalHeight = be.getHeight() - 2 * capHeight - minPuddleHeight;
        if (totalHeight <= 0) {
            return;
        }

        int capacity = Math.max(1, be.getTotalCapacityMb());
        int florinolMb = Math.max(0, Math.min(be.getStoredFlorinolMb(), capacity));
        int soilMb = Math.max(0, Math.min(be.getFlorinolSoilAmount() * 1000 + be.getSoilProgress(), capacity - florinolMb));
        if (florinolMb <= 0 && soilMb <= 0) {
            return;
        }

        float width = be.getWidth();
        float xMin = tankHullWidth;
        float xMax = xMin + width - 2 * tankHullWidth;
        float zMin = tankHullWidth;
        float zMax = zMin + width - 2 * tankHullWidth;
        float interiorBottom = capHeight + minPuddleHeight;
        float soilHeight = totalHeight * (soilMb / (float) capacity);
        float florinolHeight = totalHeight * (florinolMb / (float) capacity);
        float soilMinY = interiorBottom;
        float soilMaxY = soilMinY + soilHeight;
        float florinolMinY = soilMaxY;
        float florinolMaxY = florinolMinY + florinolHeight;

        if (soilMb > 0) {
            renderSoilBox(xMin, soilMinY, zMin, xMax, soilMaxY, zMax, ms, buffer, light);
        }

        if (florinolMb > 0) {
            FluidStack fluidStack = be.getTankInventory().getFluid();
            if (!fluidStack.isEmpty()) {
                NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(fluidStack,
                        xMin, florinolMinY, zMin, xMax, florinolMaxY, zMax,
                        buffer, ms, light, false, true);
            }
        }
    }

    private void renderSoilBox(float xMin, float yMin, float zMin, float xMax, float yMax, float zMax,
                               PoseStack ms, MultiBufferSource buffer, int light) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "block/florinol_soil"));
        VertexConsumer builder = buffer.getBuffer(PonderRenderTypes.fluid());
        int color = 0xFFFFFFFF;
        FluidRenderHelper.renderTiledFace(Direction.NORTH, xMin, yMin, xMax, yMax, zMin, builder, ms, light, color, sprite, 1);
        FluidRenderHelper.renderTiledFace(Direction.SOUTH, xMin, yMin, xMax, yMax, zMax, builder, ms, light, color, sprite, 1);
        FluidRenderHelper.renderTiledFace(Direction.WEST, zMin, yMin, zMax, yMax, xMin, builder, ms, light, color, sprite, 1);
        FluidRenderHelper.renderTiledFace(Direction.EAST, zMin, yMin, zMax, yMax, xMax, builder, ms, light, color, sprite, 1);
        FluidRenderHelper.renderTiledFace(Direction.UP, xMin, zMin, xMax, zMax, yMax, builder, ms, light, color, sprite, 1);
    }

    protected void renderAsBoiler(FlorinolBatteryBlockEntity be, float partialTicks, PoseStack ms,
                                  MultiBufferSource buffer, int light, int overlay) {
        BlockState blockState = be.getBlockState();
        VertexConsumer vb = buffer.getBuffer(RenderType.cutout());
        ms.pushPose();
        var msr = TransformStack.of(ms);
        msr.translate(be.getWidth() / 2f, 0.5, be.getWidth() / 2f);

        float dialPivotY = 6f / 16;
        float dialPivotZ = 8f / 16;
        float progress = be.boiler.gauge.getValue(partialTicks);

        for (Direction d : Iterate.horizontalDirections) {
            if (be.boiler.occludedDirections[d.get2DDataValue()]) {
                continue;
            }
            ms.pushPose();
            float yRot = -d.toYRot() - 90;
            CachedBuffers.partial(AllPartialModels.BOILER_GAUGE, blockState)
                    .rotateYDegrees(yRot)
                    .uncenter()
                    .translate(be.getWidth() / 2f - 6 / 16f, 0, 0)
                    .light(light)
                    .renderInto(ms, vb);
            CachedBuffers.partial(AllPartialModels.BOILER_GAUGE_DIAL, blockState)
                    .rotateYDegrees(yRot)
                    .uncenter()
                    .translate(be.getWidth() / 2f - 6 / 16f, 0, 0)
                    .translate(0, dialPivotY, dialPivotZ)
                    .rotateXDegrees(-145 * progress + 90)
                    .translate(0, -dialPivotY, -dialPivotZ)
                    .light(light)
                    .renderInto(ms, vb);
            ms.popPose();
        }

        ms.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(FlorinolBatteryBlockEntity be) {
        return be.isController();
    }
}
