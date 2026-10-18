package com.minechai.create_candustry.block;

import java.util.Optional;

import com.minechai.create_candustry.CreateSandustry;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class GoldenOakSaplingBlock extends SaplingBlock {
    private static final ResourceKey<ConfiguredFeature<?, ?>> GOLDEN_OAK_TREE = ResourceKey.create(
            Registries.CONFIGURED_FEATURE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "golden_oak"));

    private static final TreeGrower GOLDEN_OAK_GROWER = new TreeGrower(
            CreateSandustry.MODID + ":golden_oak",
            Optional.empty(),
            Optional.of(GOLDEN_OAK_TREE),
            Optional.empty());

    public static final MapCodec<GoldenOakSaplingBlock> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    TreeGrower.CODEC.fieldOf("tree").forGetter(block -> block.treeGrower),
                    propertiesCodec())
                    .apply(instance, GoldenOakSaplingBlock::new));

    public GoldenOakSaplingBlock(BlockBehaviour.Properties properties) {
        super(GOLDEN_OAK_GROWER, properties);
    }

    public GoldenOakSaplingBlock(TreeGrower treeGrower, BlockBehaviour.Properties properties) {
        super(treeGrower, properties);
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return CODEC;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.SAND)
                || state.is(ModBlocks.HARDENED_SAND.get())
                || super.mayPlaceOn(state, level, pos);
    }
}

