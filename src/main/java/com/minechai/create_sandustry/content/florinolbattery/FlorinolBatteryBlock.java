package com.minechai.create_sandustry.content.florinolbattery;

import com.minechai.create_sandustry.content.florinolbattery.FlorinolBatteryBlockEntity;
import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 弗洛林电池（总控方块）。
 *
 * <p>本身是一个普通方块，方块实体 {@link FlorinolBatteryBlockEntity} 负责检测由机壳 + 填充壁
 * 围成的中空多方块结构。总控可放置在壳上任意位置（墙 / 底 / 角 / 顶沿）。
 */
public class FlorinolBatteryBlock extends Block implements IBE<FlorinolBatteryBlockEntity> {
    /**
     * 纯视觉状态，用于从 96x16 图集里选择 16x16 的纹理区块。
     * 电池的 FE / 液体 / 土壤数值仍保存在 BlockEntity 中。
     */
    public static final EnumProperty<DisplayState> DISPLAY_STATE =
            EnumProperty.create("display", DisplayState.class);

    public FlorinolBatteryBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(DISPLAY_STATE, DisplayState.INACTIVE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISPLAY_STATE);
    }

    @Override
    public Class<FlorinolBatteryBlockEntity> getBlockEntityClass() {
        return FlorinolBatteryBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends FlorinolBatteryBlockEntity> getBlockEntityType() {
        return ModBlockEntities.FLORINOL_BATTERY.get();
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // 显示状态切换本身只是在同一方块上改 UV，不需要重新扫描多方块结构。
        if (!level.isClientSide && !isDisplayStateOnlyChange(state, oldState))
            withBlockEntityDo(level, pos, FlorinolBatteryBlockEntity::requestValidation);
    }

    private static boolean isDisplayStateOnlyChange(BlockState state, BlockState oldState) {
        return state.getBlock() == oldState.getBlock()
                && oldState.hasProperty(DISPLAY_STATE)
                && state.getValue(DISPLAY_STATE) != oldState.getValue(DISPLAY_STATE);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide)
            withBlockEntityDo(level, pos, FlorinolBatteryBlockEntity::requestValidation);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        IBE.onRemove(state, level, pos, newState);
    }

    /** 图集中的六个 16x16 区块。 */
    public enum DisplayState implements StringRepresentable {
        INACTIVE("inactive"),
        EMPTY("empty"),
        ONE_THIRD("one_third"),
        TWO_THIRDS("two_thirds"),
        FULL("full"),
        SOIL("soil");

        private final String name;

        DisplayState(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}