package com.minechai.create_sandustry.content.block.entity;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.content.block.machine.FlorinolBatteryBlock;
import com.minechai.create_sandustry.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModFluids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.decoration.encasing.CasingBlock;
import com.simibubi.create.content.decoration.palettes.AllPaletteBlocks;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * 弗洛林电池（总控方块）BlockEntity。
 *
 * <p>负责检测由「机壳（所有 Create 机壳，含沙机壳） + 填充壁（框架玻璃 / 岩浆块） + 总控自身」
 * 围成的中空多方块结构，并统计内部空腔中的弗洛林电池液（source 格）与弗洛林土壤。
 *
 * <p>每向外输出「1 格弗洛林电池液」对应的 FE，就把内部 1 格弗洛林液原位替换为弗洛林土壤，
 * 并触发一次结构更新（refresh）。土壤可被火焰 / 岩浆块等热源熔回弗洛林液，形成循环。
 */
public class FlorinolBatteryBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {

    /** 1 格 = 1000 mB（与 Forge 流体一格容积一致）。 */
    private static final int MB_PER_CELL = 1000;

    /** 当前处于「已组装」状态的总控方块实体集合，用于在方块被破坏/放置时及时失效。 */
    private static final Set<FlorinolBatteryBlockEntity> ACTIVE =
            Collections.newSetFromMap(new WeakHashMap<>());

    private boolean assembled;
    private BlockPos minCorner;
    private BlockPos maxCorner;
    private int interiorCapacity;   // 内部空腔总格数
    private int florinolCells;      // 当前弗洛林液格数
    private int soilCells;          // 当前弗洛林土壤格数
    private int scanCooldown;
    private boolean needsValidation;

    public FlorinolBatteryBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FLORINOL_BATTERY.get(), pos, state);
    }

    public FlorinolBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.needsValidation = true;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        if (needsValidation) {
            needsValidation = false;
            validate();
        } else if (--scanCooldown <= 0) {
            scanCooldown = ModConfigs.FLORINOL_BATTERY_SCAN_INTERVAL.get();
            validate();
        }
    }

    /** 放置/邻接变化时请求尽快重新校验。 */
    public void requestValidation() {
        this.needsValidation = true;
    }

    @Override
    public void destroy() {
        super.destroy();
        ACTIVE.remove(this);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        ACTIVE.remove(this);
    }

    public boolean isAssembled() {
        return assembled;
    }

    public int getFlorinolCells() {
        return florinolCells;
    }

    public int getSoilCells() {
        return soilCells;
    }

    public int getInteriorCapacity() {
        return interiorCapacity;
    }

    public int getEnergyStored() {
        return florinolCells * fePerCell();
    }

    public int getMaxEnergyStored() {
        return interiorCapacity * fePerCell();
    }

    /**
     * 根据已组装的电池状态选择图集区块：
     * inactive -> (0,0)-(15,15)；
     * empty / 1/3 / 2/3 / full -> 16..79 的四个 FE 区块；
     * soil -> (80,0)-(95,15) 的土壤填满区块。
     */
    private FlorinolBatteryBlock.DisplayState computeDisplayState() {
        if (!assembled)
            return FlorinolBatteryBlock.DisplayState.INACTIVE;
        if (interiorCapacity <= 0)
            return FlorinolBatteryBlock.DisplayState.EMPTY;
        if (soilCells >= interiorCapacity)
            return FlorinolBatteryBlock.DisplayState.SOIL;
        if (florinolCells <= 0)
            return FlorinolBatteryBlock.DisplayState.EMPTY;

        long cells = florinolCells;
        long capacity = interiorCapacity;
        // 按“当前处于第几个三分之一”显示：
        // (0, 1/3] -> ONE_THIRD，(1/3, 2/3] -> TWO_THIRDS，(2/3, 1] -> FULL。
        if (cells * 3 <= capacity)
            return FlorinolBatteryBlock.DisplayState.ONE_THIRD;
        if (cells * 3 <= capacity * 2)
            return FlorinolBatteryBlock.DisplayState.TWO_THIRDS;
        return FlorinolBatteryBlock.DisplayState.FULL;
    }

    private static int fePerCell() {
        return MB_PER_CELL * ModConfigs.FLORINOL_BATTERY_FE_PER_MB.get();
    }

    /**
     * 从任意骨架机壳出发，沿壳（机壳 + 填充壁 + 总控）flood-fill 找到唯一的总控方块实体；
     * 找不到或存在多个总控时返回 null。
     */
    public static FlorinolBatteryBlockEntity findController(Level level, BlockPos start) {
        if (level == null || level.isClientSide)
            return null;
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        FlorinolBatteryBlockEntity found = null;
        int controllers = 0;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (!visited.add(p))
                continue;
            BlockState s = level.getBlockState(p);
            if (isController(s)) {
                controllers++;
                if (level.getBlockEntity(p) instanceof FlorinolBatteryBlockEntity be)
                    found = be;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!visited.contains(n) && isShell(level.getBlockState(n)))
                    queue.add(n);
            }
        }
        return controllers == 1 ? found : null;
    }

    // ===== 破坏/放置事件：及时失效 =====

    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            notifyBlockChanged(level, event.getPos());
    }

    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            notifyBlockChanged(level, event.getPos());
    }

    /** 内部液体/土壤互相转化后，通知包围盒内已组装的电池立即重新校验。 */
    public static void notifyInteriorChanged(ServerLevel level, BlockPos pos) {
        notifyBlockChanged(level, pos);
    }

    private static void notifyBlockChanged(ServerLevel level, BlockPos pos) {
        for (FlorinolBatteryBlockEntity be : ACTIVE) {
            if (be.level == level && be.containsInBox(pos))
                be.requestValidation();
        }
    }

    private boolean containsInBox(BlockPos pos) {
        return minCorner != null && maxCorner != null
                && pos.getX() >= minCorner.getX() && pos.getX() <= maxCorner.getX()
                && pos.getY() >= minCorner.getY() && pos.getY() <= maxCorner.getY()
                && pos.getZ() >= minCorner.getZ() && pos.getZ() <= maxCorner.getZ();
    }


    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        if (!assembled)
            return false;
        tooltip.add(indent(Component.translatable("goggles.createsandustry.florinol_battery.title")
                .withStyle(ChatFormatting.WHITE)));
        tooltip.add(indent(Component.translatable("goggles.createsandustry.florinol_battery.energy.stored")
                .withStyle(ChatFormatting.GRAY)));
        tooltip.add(indent(Component.literal(format(getEnergyStored())).append("⚡")
                .withStyle(ChatFormatting.AQUA)));
        tooltip.add(indent(Component.translatable("goggles.createsandustry.florinol_battery.energy.capacity")
                .withStyle(ChatFormatting.GRAY)));
        tooltip.add(indent(Component.literal(format(getMaxEnergyStored())).append("⚡")
                .withStyle(ChatFormatting.AQUA)));
        tooltip.add(indent(buildEnergyBar()));
        return true;
    }

    private static Component indent(Component component) {
        return Component.literal("    ").append(component);
    }

    private Component buildEnergyBar() {
        int capacity = Math.max(1, interiorCapacity);
        int blue = Math.min(florinolCells, capacity);
        int purple = Math.min(soilCells, capacity - blue);
        int empty = Math.max(0, capacity - blue - purple);

        int segments = 10;
        int blueSeg = Math.min(segments, Math.round(blue * segments / (float) capacity));
        int purpleSeg = Math.min(segments - blueSeg, Math.round(purple * segments / (float) capacity));
        int emptySeg = Math.max(0, segments - blueSeg - purpleSeg);

        return Component.literal("[")
                .append(Component.literal("⚡".repeat(blueSeg)).withStyle(ChatFormatting.AQUA))
                .append(Component.literal("█".repeat(purpleSeg)).withStyle(ChatFormatting.DARK_PURPLE))
                .append(Component.literal("░".repeat(emptySeg)).withStyle(ChatFormatting.DARK_GRAY))
                .append("]");
    }

    private static String format(int value) {
        return String.format("%,d", value);
    }

    // ===== FE 输出：整格放电，弗洛林液 → 弗洛林土壤 =====

    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!assembled || !(level instanceof ServerLevel sl))
            return 0;
        int fePer = fePerCell();
        int cells = Math.min(maxExtract / fePer, florinolCells);
        int result = cells * fePer;
        if (result <= 0)
            return 0;
        if (simulate)
            return result;

        int remaining = cells;
        for (int y = minCorner.getY() + 1; y <= maxCorner.getY() && remaining > 0; y++)
            for (int x = minCorner.getX() + 1; x < maxCorner.getX() && remaining > 0; x++)
                for (int z = minCorner.getZ() + 1; z < maxCorner.getZ() && remaining > 0; z++) {
                    BlockPos c = new BlockPos(x, y, z);
                    if (sl.getFluidState(c).getType() == ModFluids.FLORINOL.get()) {
                        sl.setBlock(c, ModBlocks.FLORINOL_SOIL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
                        remaining--;
                    }
                }
        refresh();
        return result;
    }

    /** 每次「液 ↔ 土」转化后进行更新。 */
    private void refresh() {
        validate();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
            sendData();
        }
    }

    // ===== 多方块检测 =====

    private void validate() {
        if (!(level instanceof ServerLevel sl))
            return;

        boolean wasAssembled = assembled;
        int wasFlorinol = florinolCells;
        int wasSoil = soilCells;
        int wasCapacity = interiorCapacity;
        BlockPos wasMin = minCorner;
        BlockPos wasMax = maxCorner;

        // 先假设结构已失效
        assembled = false;
        minCorner = null;
        maxCorner = null;
        florinolCells = 0;
        soilCells = 0;
        interiorCapacity = 0;

        // 1) 从总控 flood-fill 壳（机壳 + 填充壁 + 总控）
        Set<BlockPos> shell = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        boolean hasCasing = false;
        int controllers = 0;
        while (!queue.isEmpty()) {
            BlockPos p = queue.poll();
            if (!shell.add(p))
                continue;
            BlockState s = sl.getBlockState(p);
            if (isCasing(s))
                hasCasing = true;
            if (isController(s))
                controllers++;
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!shell.contains(n) && isShell(sl.getBlockState(n)))
                    queue.add(n);
            }
        }

        boolean valid = hasCasing && controllers == 1;
        int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;

        // 2) 包围盒
        if (valid) {
            minX = Integer.MAX_VALUE; minY = Integer.MAX_VALUE; minZ = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE; maxY = Integer.MIN_VALUE; maxZ = Integer.MIN_VALUE;
            for (BlockPos p : shell) {
                minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
                minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            }
            int w = maxX - minX + 1, h = maxY - minY + 1, d = maxZ - minZ + 1;
            if (w < 3 || h < 3 || d < 3)
                valid = false;
        }

        // 3) 形状校验：边界 = 壳，内部 = 无壳
        if (valid) {
            for (int x = minX; x <= maxX && valid; x++)
                for (int y = minY; y <= maxY && valid; y++)
                    for (int z = minZ; z <= maxZ && valid; z++) {
                        boolean boundary = y == minY || x == minX || x == maxX || z == minZ || z == maxZ;
                        boolean shellHere = isShell(sl.getBlockState(new BlockPos(x, y, z)));
                        if (boundary ? !shellHere : shellHere)
                            valid = false;
                    }
        }

        // 4) 统计内部弗洛林液与弗洛林土壤
        if (valid) {
            int florinol = 0;
            int soil = 0;
            for (int x = minX + 1; x < maxX; x++)
                for (int z = minZ + 1; z < maxZ; z++)
                    for (int y = minY + 1; y <= maxY; y++) {
                        BlockPos c = new BlockPos(x, y, z);
                        if (sl.getFluidState(c).getType() == ModFluids.FLORINOL.get())
                            florinol++;
                        else if (sl.getBlockState(c).is(ModBlocks.FLORINOL_SOIL.get()))
                            soil++;
                    }
            florinolCells = florinol;
            soilCells = soil;
            interiorCapacity = (maxX - minX - 1) * (maxZ - minZ - 1) * (maxY - minY);
            minCorner = new BlockPos(minX, minY, minZ);
            maxCorner = new BlockPos(maxX, maxY, maxZ);
            assembled = true;
        }

        // 维护 ACTIVE
        if (assembled)
            ACTIVE.add(this);
        else
            ACTIVE.remove(this);

        // 仅在状态变化时同步：sendData 通知客户端（Jade/护目镜），invalidateCapabilities 刷新能力缓存
        boolean changed = assembled != wasAssembled
                || florinolCells != wasFlorinol
                || soilCells != wasSoil
                || interiorCapacity != wasCapacity
                || !java.util.Objects.equals(minCorner, wasMin)
                || !java.util.Objects.equals(maxCorner, wasMax);
        if (changed) {
            setChanged();
            sendData();
            sl.invalidateCapabilities(worldPosition);
        }

        // 视觉状态独立于原始数值更新：只改方块状态，避免触发邻接更新和额外校验。
        FlorinolBatteryBlock.DisplayState displayState = computeDisplayState();
        BlockState currentState = getBlockState();
        if (currentState.getValue(FlorinolBatteryBlock.DISPLAY_STATE) != displayState) {
            sl.setBlock(worldPosition,
                    currentState.setValue(FlorinolBatteryBlock.DISPLAY_STATE, displayState),
                    Block.UPDATE_CLIENTS);
        }
    }

    // ===== 谓词 =====

    /** 机壳：所有 Create 机壳（含本模组的沙机壳），作为圈定范围的骨架。 */
    private static boolean isCasing(BlockState s) {
        return s.getBlock() instanceof CasingBlock;
    }

    /** 填充壁：框架玻璃 + 岩浆块 + 热能缓冲器。 */
    private static boolean isFillerWall(BlockState s) {
        return s.is(AllPaletteBlocks.FRAMED_GLASS.get())
                || s.is(Blocks.MAGMA_BLOCK)
                || s.is(ModBlocks.THERMAL_RELAY.get());
    }

    private static boolean isController(BlockState s) {
        return s.is(ModBlocks.FLORINOL_BATTERY.get());
    }

    private static boolean isShell(BlockState s) {
        return isCasing(s) || isFillerWall(s) || isController(s);
    }

    // ===== 序列化 =====

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putBoolean("Assembled", assembled);
        tag.putInt("FlorinolCells", florinolCells);
        tag.putInt("SoilCells", soilCells);
        tag.putInt("InteriorCapacity", interiorCapacity);
        if (minCorner != null)
            tag.putLong("MinCorner", minCorner.asLong());
        if (maxCorner != null)
            tag.putLong("MaxCorner", maxCorner.asLong());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        assembled = tag.getBoolean("Assembled");
        florinolCells = tag.getInt("FlorinolCells");
        soilCells = tag.getInt("SoilCells");
        interiorCapacity = tag.getInt("InteriorCapacity");
        if (tag.contains("MinCorner"))
            minCorner = BlockPos.of(tag.getLong("MinCorner"));
        if (tag.contains("MaxCorner"))
            maxCorner = BlockPos.of(tag.getLong("MaxCorner"));
    }
}