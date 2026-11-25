package com.minechai.create_sandustry.content.thermalbuffer;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import java.util.List;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.content.thermalbuffer.ThermalBufferBlockEntity;
import com.minechai.create_sandustry.registry.ModDataComponents;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * 热能缓冲器。
 *
 * <p>方块只保存对外的热等级，真实连续热值保存在
 * {@link ThermalBufferBlockEntity} 中。热值在正常掉落和放置流程中通过
 * {@link ModDataComponents#THERMAL_HEAT} 保留。</p>
 */
public class ThermalBufferBlock extends Block implements IBE<ThermalBufferBlockEntity> {
    /** 与 Create 共用的热等级属性，工作盆可直接识别。 */
    public static final EnumProperty<HeatLevel> HEAT_LEVEL = BlazeBurnerBlock.HEAT_LEVEL;
    /** 红石激活时封闭热输出，并阻止自然热值流失。 */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public ThermalBufferBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
                .setValue(HEAT_LEVEL, HeatLevel.NONE)
                .setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HEAT_LEVEL, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(HEAT_LEVEL, HeatLevel.NONE)
                .setValue(POWERED, powered);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            updatePoweredState(level, pos, state);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            updatePoweredState(level, pos, state);
        }
    }

    private static void updatePoweredState(Level level, BlockPos pos, BlockState state) {
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            state = state.setValue(POWERED, powered);
            level.setBlockAndUpdate(pos, state);
        }
        BlockState currentState = level.getBlockState(pos);
        if (currentState.getBlock() instanceof ThermalBufferBlock) {
            final BlockState current = currentState;
            if (level.getBlockEntity(pos) instanceof ThermalBufferBlockEntity buffer) {
                buffer.onSealedStateChanged(current.getValue(POWERED));
            }
        }
    }

    @Override
    public Class<ThermalBufferBlockEntity> getBlockEntityClass() {
        return ThermalBufferBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends ThermalBufferBlockEntity> getBlockEntityType() {
        return ModBlockEntities.THERMAL_RELAY.get();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof ThermalBufferBlockEntity buffer)) {
            return;
        }

        double storedHeat = stack.getOrDefault(ModDataComponents.THERMAL_HEAT.get(), 0.0D);
        buffer.setStoredHeat(storedHeat);
    }

    /**
     * 只有真实方块掉落会携带热值。创造模式中键选取刻意保持冷态，
     * 避免同一个热物品被反复放置时向新缓冲区写入旧热值。
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ThermalBufferBlockEntity buffer)) {
            return drops;
        }

        double storedHeat = buffer.getStoredHeat();
        for (ItemStack drop : drops) {
            if (drop.is(this.asItem())) {
                drop.set(ModDataComponents.THERMAL_HEAT.get(), storedHeat);
            }
        }
        return drops;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }
}
