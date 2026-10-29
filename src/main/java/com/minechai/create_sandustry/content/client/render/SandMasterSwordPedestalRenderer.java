package com.minechai.create_sandustry.content.client.render;

import com.minechai.create_sandustry.content.block.entity.SandMasterSwordPedestalBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 在剑座中央绘制独立的完整沙噬剑。
 *
 * <p>物品模型位于 XY 平面，内置剑贴图本身是斜置的。这里先绕 Y 轴对齐方块朝向，再绕 Z 轴旋转
 * -135°，让剑柄朝上、剑尖朝下并垂直插入中央剑槽；最后缩放到约 0.56 倍。使用 GUI 展示上下文
 * 是为了绕开手持物品的显示变换，让剑座中的剑尺寸和角度只由本渲染器控制。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SandMasterSwordPedestalRenderer implements BlockEntityRenderer<SandMasterSwordPedestalBlockEntity> {
    private static final float SWORD_CENTER_Y = 0.66F;
    private static final float SWORD_SCALE = 0.56F;

    private final ItemRenderer itemRenderer;

    public SandMasterSwordPedestalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SandMasterSwordPedestalBlockEntity pedestal, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack sword = pedestal.getSword();
        if (sword.isEmpty()) {
            return;
        }

        // 朝向在插剑时记录；剑座本身先放在哪个方向不会影响剑面。
        float yRotation = -pedestal.getSwordFacing().toYRot();

        poseStack.pushPose();
        poseStack.translate(0.5F, SWORD_CENTER_Y, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotation));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-135.0F));
        poseStack.scale(SWORD_SCALE, SWORD_SCALE, SWORD_SCALE);
        this.itemRenderer.renderStatic(sword, ItemDisplayContext.GUI, packedLight, packedOverlay, poseStack,
                bufferSource, pedestal.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SandMasterSwordPedestalBlockEntity pedestal) {
        return new AABB(pedestal.getBlockPos()).inflate(0.2D).expandTowards(0.0D, 0.35D, 0.0D);
    }
}