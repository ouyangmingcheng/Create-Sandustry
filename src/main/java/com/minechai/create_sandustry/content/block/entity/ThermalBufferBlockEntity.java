package com.minechai.create_sandustry.content.block.entity;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import java.util.List;

import com.minechai.create_sandustry.content.block.machine.ThermalBufferBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.burner.LitBlazeBurnerBlock;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * 热能缓冲器的连续储热逻辑。
 *
 * <p>实现热值保存、来源吸收、75% 缓冲区传导、自然流失及等级投影。
 * 红石激活时通过 {@link #isSealed()} 封闭所有对外热输出与自然流失。
 * 对外温度等级通过方块状态选择对应的静态纹理变体。</p>
 */
public class ThermalBufferBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final double MAX_HEAT = 1000.0D;
    public static final double CONDUCTION_FACTOR = 0.75D;
    public static final double INJECT_PER_TICK = 25.0D;
    public static final double LOSS_FACTOR_PER_TICK = 0.9995D;
    public static final double MIN_TRACKED_HEAT = 0.05D;

    public static final double LEVEL_1_HEAT = 300.0D;
    public static final double LEVEL_2_HEAT = 600.0D;
    public static final double LEVEL_3_HEAT = 900.0D;
    public static final double LEVEL_1_DROP = 270.0D;
    public static final double LEVEL_2_DROP = 540.0D;
    public static final double LEVEL_3_DROP = 810.0D;

    private static final double EPSILON = 1.0E-6D;
    private static final int CLIENT_SYNC_INTERVAL = 10;

    private double storedHeat;
    private int thermalLevel;
    private int syncCounter;
    private double lastSyncedHeat = -1.0D;

    public ThermalBufferBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.THERMAL_RELAY.get(), pos, state);
    }

    public ThermalBufferBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        // 当前没有 Create behaviour；保留 SmartBlockEntity 以兼容后续渲染/工具箱集成。
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        boolean changed = tickHeatTransfer();
        changed |= updateThermalLevel(false);
        if (changed) {
            this.setChanged();
        }

        this.syncCounter++;
        if (this.syncCounter >= CLIENT_SYNC_INTERVAL
                && Math.abs(this.storedHeat - this.lastSyncedHeat) >= 0.01D) {
            this.syncCounter = 0;
            this.lastSyncedHeat = this.storedHeat;
            this.sendData();
        }
    }

    private boolean tickHeatTransfer() {
        double incomingHeat = 0.0D;

        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = this.worldPosition.relative(direction);
            if (!this.level.isLoaded(neighborPos)) {
                continue;
            }

            BlockState neighborState = this.level.getBlockState(neighborPos);
            incomingHeat = Math.max(incomingHeat, getDirectSourceHeat(neighborState));

            if (neighborState.getBlock() instanceof ThermalBufferBlock
                    && this.level.getBlockEntity(neighborPos) instanceof ThermalBufferBlockEntity neighbor) {
                incomingHeat = Math.max(incomingHeat, neighbor.getOutputHeat() * CONDUCTION_FACTOR);
            }
        }

        double previousHeat = this.storedHeat;
        if (incomingHeat > this.storedHeat + EPSILON) {
            this.storedHeat = Math.min(incomingHeat, this.storedHeat + INJECT_PER_TICK);
        } else if (incomingHeat < this.storedHeat - EPSILON && canLoseHeat()) {
            this.storedHeat *= LOSS_FACTOR_PER_TICK;
            if (this.storedHeat < MIN_TRACKED_HEAT) {
                this.storedHeat = 0.0D;
            }
        }

        this.storedHeat = clampHeat(this.storedHeat);
        return Math.abs(previousHeat - this.storedHeat) > EPSILON;
    }

    /** 红石激活时封闭所有对外热输出，并停止自然流失。 */
    protected boolean isSealed() {
        return this.getBlockState().getValue(ThermalBufferBlock.POWERED);
    }

    /** 红石状态改变后立即刷新对外 HEAT_LEVEL。 */
    public void onSealedStateChanged(boolean powered) {
        this.updateThermalLevel(true);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.sendData();
        }
    }

    /** 缓冲区对其它缓冲区或外部接口实际可释放的热值。 */
    public double getOutputHeat() {
        return this.isSealed() ? 0.0D : this.storedHeat;
    }

    /** 下界类维度只关闭自然流失，不关闭未封闭状态下的主动传导。 */
    protected boolean isLosslessDimension() {
        return this.level != null && this.level.dimensionType().ultraWarm();
    }

    protected boolean canLoseHeat() {
        return !this.isSealed() && !this.isLosslessDimension();
    }

    public double getStoredHeat() {
        return this.storedHeat;
    }

    public int getThermalLevel() {
        return this.thermalLevel;
    }

    /** 用于从物品组件恢复热值，或由后续外部 API 注入。 */
    public void setStoredHeat(double heat) {
        this.storedHeat = clampHeat(heat);
        this.thermalLevel = getRawLevel(this.storedHeat);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            updateThermalLevel(true);
            this.sendData();
        }
    }

    public static double clampHeat(double heat) {
        if (!Double.isFinite(heat)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(MAX_HEAT, heat));
    }

    public static int getRawLevel(double heat) {
        if (heat >= LEVEL_3_HEAT) {
            return 3;
        }
        if (heat >= LEVEL_2_HEAT) {
            return 2;
        }
        if (heat >= LEVEL_1_HEAT) {
            return 1;
        }
        return 0;
    }

    private static int applyHysteresis(double heat, int currentLevel) {
        if (heat >= LEVEL_3_HEAT) {
            return 3;
        }
        if (currentLevel >= 3 && heat >= LEVEL_3_DROP) {
            return 3;
        }
        if (heat >= LEVEL_2_HEAT) {
            return 2;
        }
        if (currentLevel >= 2 && heat >= LEVEL_2_DROP) {
            return 2;
        }
        if (heat >= LEVEL_1_HEAT) {
            return 1;
        }
        if (currentLevel >= 1 && heat >= LEVEL_1_DROP) {
            return 1;
        }
        return 0;
    }

    private boolean updateThermalLevel(boolean force) {
        int nextLevel = applyHysteresis(this.storedHeat, this.thermalLevel);
        boolean levelChanged = nextLevel != this.thermalLevel;

        if (!force && !levelChanged && (this.level == null || this.level.isClientSide)) {
            return false;
        }

        this.thermalLevel = nextLevel;
        if (this.level == null || this.level.isClientSide) {
            return levelChanged;
        }

        // 即使热等级没有变化，也要校正方块状态中的 blaze 值，
        // 避免区块重载或红石状态变化后留下旧的 HEAT_LEVEL。
        HeatLevel exposedLevel = this.isSealed() ? HeatLevel.NONE : toHeatLevel(nextLevel);
        BlockState state = this.getBlockState();
        if (state.getValue(ThermalBufferBlock.HEAT_LEVEL) != exposedLevel) {
            this.level.setBlockAndUpdate(this.worldPosition,
                    state.setValue(ThermalBufferBlock.HEAT_LEVEL, exposedLevel));
            this.notifyUpdate();
            return true;
        }
        return levelChanged;
    }

    private static HeatLevel toHeatLevel(int level) {
        return switch (level) {
            case 1 -> HeatLevel.SMOULDERING;
            case 2 -> HeatLevel.KINDLED;
            case 3 -> HeatLevel.SEETHING;
            default -> HeatLevel.NONE;
        };
    }

    /**
     * 固定热源只向相邻缓冲区提供一次热势，不参与 0.75 的缓冲区传导衰减。
     * 热能缓冲器本身不在这里计算，避免绕过缓冲区之间的 75% 规则。
     */
    public static double getDirectSourceHeat(BlockState state) {
        if (state.getBlock() instanceof ThermalBufferBlock) {
            return 0.0D;
        }

        if (state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) {
            HeatLevel heatLevel = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
            return switch (heatLevel) {
                case SMOULDERING -> 300.0D;
                case FADING, KINDLED -> 700.0D;
                case SEETHING -> 1000.0D;
                default -> 0.0D;
            };
        }

        if (state.getBlock() instanceof LitBlazeBurnerBlock burner) {
            return state.getValue(LitBlazeBurnerBlock.FLAME_TYPE) == LitBlazeBurnerBlock.FlameType.REGULAR
                    ? 700.0D
                    : 1000.0D;
        }

        if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
            return 300.0D;
        }

        if (state.getBlock() instanceof CampfireBlock && state.getValue(CampfireBlock.LIT)) {
            return 300.0D;
        }

        if (state.getFluidState().isSourceOfType(Fluids.LAVA)) {
            return 500.0D;
        }

        if (state.is(Blocks.MAGMA_BLOCK)) {
            return 500.0D;
        }

        return 0.0D;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(indent(Component.translatable("goggles.createsandustry.thermal_relay.title")
                .withStyle(ChatFormatting.WHITE)));

        Component heatValue = Component.literal(Long.toString(Math.round(this.storedHeat)))
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal(" \u2668").withStyle(levelColor(this.thermalLevel)));
        tooltip.add(indent(Component.translatable("goggles.createsandustry.thermal_relay.heat")
                .withStyle(ChatFormatting.GRAY)
                .append(heatValue)));

        return true;
    }

    private static ChatFormatting levelColor(int level) {
        return switch (level) {
            case 1 -> ChatFormatting.GOLD;
            case 2 -> ChatFormatting.YELLOW;
            case 3 -> ChatFormatting.BLUE;
            default -> ChatFormatting.DARK_GRAY;
        };
    }

    private static Component indent(Component component) {
        return Component.literal("    ").append(component);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putDouble("StoredHeat", this.storedHeat);
        tag.putInt("ThermalLevel", this.thermalLevel);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        this.storedHeat = clampHeat(tag.getDouble("StoredHeat"));
        this.thermalLevel = tag.contains("ThermalLevel")
                ? Math.max(0, Math.min(3, tag.getInt("ThermalLevel")))
                : getRawLevel(this.storedHeat);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
