package com.minechai.create_candustry.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.foundation.block.connected.CTModel;

import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelData.Builder;
import net.neoforged.neoforge.client.model.data.ModelProperty;

/**
 * Direct port of Create's FluidTankModel.  It keeps Create's exact internal
 * face culling and CTModel sprite-shifting behaviour; only the sprite entries
 * point at createsandustry copies of Create's tank textures.
 */
public class FlorinolBatteryModel extends CTModel {
    protected static final ModelProperty<CullData> CULL_PROPERTY = new ModelProperty<>();

    public static FlorinolBatteryModel standard(BakedModel originalModel) {
        return new FlorinolBatteryModel(originalModel,
                FlorinolBatterySprites.SIDE,
                FlorinolBatterySprites.TOP,
                FlorinolBatterySprites.INNER);
    }

    private FlorinolBatteryModel(BakedModel originalModel,
                                 com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry side,
                                 com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry top,
                                 com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry inner) {
        super(originalModel, new FlorinolBatteryCTBehaviour(side, top, inner));
    }

    @Override
    protected ModelData.Builder gatherModelData(Builder builder, BlockAndTintGetter world, BlockPos pos,
                                                BlockState state, ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        CullData cullData = new CullData();
        for (Direction direction : Iterate.horizontalDirections) {
            cullData.setCulled(direction, ConnectivityHandler.isConnected(world, pos, pos.relative(direction)));
        }
        return builder.with(CULL_PROPERTY, cullData);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                    ModelData extraData, RenderType renderType) {
        if (side != null) {
            return Collections.emptyList();
        }

        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Iterate.directions) {
            if (extraData.has(CULL_PROPERTY) && extraData.get(CULL_PROPERTY).isCulled(direction)) {
                continue;
            }
            quads.addAll(super.getQuads(state, direction, random, extraData, renderType));
        }
        quads.addAll(super.getQuads(state, null, random, extraData, renderType));
        return quads;
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        // Equivalent to Create's registration .addLayer(() -> RenderType::cutoutMipped).
        // This is the NeoForge 1.21.1 model-driven API and keeps the window alpha.
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack itemStack, boolean fabulous) {
        // Keep the inventory item window transparent as well.
        return List.of(RenderType.cutoutMipped());
    }

    private static class CullData {
        private final boolean[] culledFaces;

        private CullData() {
            culledFaces = new boolean[4];
            Arrays.fill(culledFaces, false);
        }

        private void setCulled(Direction face, boolean cull) {
            if (face.getAxis().isVertical()) {
                return;
            }
            culledFaces[face.get2DDataValue()] = cull;
        }

        private boolean isCulled(Direction face) {
            if (face.getAxis().isVertical()) {
                return false;
            }
            return culledFaces[face.get2DDataValue()];
        }
    }
}
