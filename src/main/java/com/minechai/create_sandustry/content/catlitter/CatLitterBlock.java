package com.minechai.create_sandustry.content.catlitter;

import com.minechai.create_sandustry.content.sand.SpecialSandBlock;
import com.minechai.create_sandustry.content.sand.SpecialSandEffects;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.CreateSandustry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import org.jetbrains.annotations.Nullable;

public class CatLitterBlock extends BrushableBlock {
    private static final String MEOWMECHANICS_MOD_ID = "laowu";
    private static final ResourceKey<LootTable> ARCHAEOLOGY_LOOT_TABLE = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "archaeology/cat_litter"));
    private static final ResourceKey<LootTable> ARCHAEOLOGY_LOOT_TABLE_FALLBACK = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "archaeology/cat_litter_fallback"));

    public CatLitterBlock(Properties properties) {
        super(Blocks.SAND, SoundEvents.BRUSH_SAND, SoundEvents.BRUSH_SAND_COMPLETED, properties);
    }


    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BrushableBlockEntity blockEntity = new BrushableBlockEntity(pos, state);
        blockEntity.setLootTable(selectArchaeologyLootTable(), 0L);
        return blockEntity;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (!level.isClientSide
                && !SpecialSandBlock.hasSilkTouch(level, tool)
                && level instanceof ServerLevel serverLevel) {
            SpecialSandEffects.triggerSpecialSandEffect(
                    serverLevel,
                    pos,
                    state,
                    SpecialSandEffects.Trigger.PLAYER_BREAK,
                    null);
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    public void onBrokenAfterFall(Level level, BlockPos pos, FallingBlockEntity fallingBlock) {
        super.onBrokenAfterFall(level, pos, fallingBlock);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            SpecialSandEffects.triggerSpecialSandEffect(
                    serverLevel,
                    pos,
                    fallingBlock.getBlockState(),
                    SpecialSandEffects.Trigger.FALLING_BLOCK_DISCARD,
                    null);
        }
    }

    public static void addBlockEntityType(BlockEntityTypeAddBlocksEvent event) {
        event.modify(BlockEntityType.BRUSHABLE_BLOCK, ModBlocks.CAT_LITTER.get());
    }

    public static void handleTrigger(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CAT_AMBIENT, SoundSource.BLOCKS,
                1.0F, 0.8F + level.random.nextFloat() * 0.4F);
    }

    private static ResourceKey<LootTable> selectArchaeologyLootTable() {
        return ModList.get().isLoaded(MEOWMECHANICS_MOD_ID)
                ? ARCHAEOLOGY_LOOT_TABLE
                : ARCHAEOLOGY_LOOT_TABLE_FALLBACK;
    }
}