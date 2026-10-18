package com.minechai.create_candustry.client;

import com.minechai.create_candustry.entity.LivingSand;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;

@OnlyIn(Dist.CLIENT)
public class LivingSandRenderer extends EntityRenderer<LivingSand> {
    private final BlockRenderDispatcher dispatcher;

    public LivingSandRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.dispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(LivingSand entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        BlockState state = Blocks.SAND.defaultBlockState();
        Level level = entity.level();
        BlockPos renderPos = BlockPos.containing(entity.getX(), entity.getBoundingBox().maxY, entity.getZ());

        poseStack.pushPose();
        poseStack.translate(-0.5D, 0.0D, -0.5D);

        var model = this.dispatcher.getBlockModel(state);
        RandomSource random = RandomSource.create(state.getSeed(renderPos));
        for (var renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            this.dispatcher.getModelRenderer().tesselateBlock(
                    level,
                    model,
                    state,
                    renderPos,
                    poseStack,
                    buffer.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    RandomSource.create(),
                    state.getSeed(renderPos),
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    renderType);
        }

        poseStack.popPose();
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LivingSand entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
