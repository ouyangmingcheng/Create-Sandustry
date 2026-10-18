package com.minechai.create_candustry.block.entity;

import static java.lang.Math.abs;

import java.util.List;
import java.util.function.Consumer;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.minechai.create_candustry.block.machine.FlorinolBatteryBlock;
import com.minechai.create_candustry.block.machine.FlorinolBatteryBlock.Shape;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.infrastructure.config.AllConfigs;
import com.minechai.create_candustry.config.ModConfigs;

import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.nbt.NBTHelper;
import com.minechai.create_candustry.fluid.ModFluids;
import com.minechai.create_candustry.item.ModItems;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

public class FlorinolBatteryBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, IMultiBlockEntityContainer.Fluid {

	/** 多方块结构的固定水平占地宽度（3x3），宽度不可通过配置调整。 */
	private static final int FIXED_WIDTH = 3;

	private static int maxHeight() {
		return ModConfigs.FLORINOL_BATTERY_MAX_HEIGHT.get();
	}

	private static int maxBlocks() {
		return FIXED_WIDTH * FIXED_WIDTH * maxHeight();
	}

	private static int capacityPerBlock() {
		return ModConfigs.FLORINOL_BATTERY_CAPACITY_PER_BLOCK.get();
	}

	private static int mbPerSoil() {
		return ModConfigs.FLORINOL_BATTERY_MB_PER_SOIL.get();
	}

	private static int fePerMb() {
		return ModConfigs.FLORINOL_BATTERY_FE_PER_MB.get();
	}

	protected IFluidHandler fluidCapability;
	protected boolean forceFluidLevelUpdate;
	protected FluidTank tankInventory;
	protected BlockPos controller;
	protected BlockPos lastKnownPos;
	protected boolean updateConnectivity;
	protected boolean updateCapability;
	public boolean window;
	public int luminosity;
	protected int width;
	public int height;
	private int florinolSoilAmount;
	private int soilProgress;

	public BoilerData boiler;

	private static final int SYNC_RATE = 8;
	protected int syncCooldown;
	protected boolean queuedSync;

	// For rendering purposes only
	private LerpedFloat fluidLevel;

	public FlorinolBatteryBlockEntity(BlockPos pos, BlockState state) {
		this(ModBlockEntities.FLORINOL_BATTERY.get(), pos, state);
	}

	public FlorinolBatteryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
		super(type, pos, state);
		tankInventory = createInventory();
		forceFluidLevelUpdate = true;
		updateConnectivity = false;
		updateCapability = false;
		window = true;
		height = 1;
		width = 1;
		boiler = new BoilerData();
		refreshCapability();
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(
				Capabilities.FluidHandler.BLOCK,
				ModBlockEntities.FLORINOL_BATTERY.get(),
				(be, context) -> {
					if (be.fluidCapability == null)
						be.refreshCapability();
					return be.fluidCapability;
				}
		);
		event.registerBlockEntity(
				Capabilities.EnergyStorage.BLOCK,
				ModBlockEntities.FLORINOL_BATTERY.get(),
				(be, context) -> be.new BatteryEnergyStorage()
		);
		event.registerBlockEntity(
				Capabilities.ItemHandler.BLOCK,
				ModBlockEntities.FLORINOL_BATTERY.get(),
				(be, context) -> be.new BatterySoilItemHandler()
		);
	}

	protected SmartFluidTank createInventory() {
		return new SmartFluidTank(getCapacityMultiplier(), this::onFluidStackChanged) {
			@Override
			public boolean isFluidValid(FluidStack stack) {
				return isFlorinol(stack);
			}
		};
	}

	public void updateConnectivity() {
		updateConnectivity = false;
		if (level.isClientSide)
			return;
		if (!isController())
			return;
		ConnectivityHandler.formMulti(this);
	}

	@Override
	public void tick() {
		super.tick();
		if (syncCooldown > 0) {
			syncCooldown--;
			if (syncCooldown == 0 && queuedSync)
				sendData();
		}

		if (lastKnownPos == null)
			lastKnownPos = getBlockPos();
		else if (!lastKnownPos.equals(worldPosition) && worldPosition != null) {
			onPositionChanged();
			return;
		}

		if (updateCapability) {
			updateCapability = false;
			refreshCapability();
		}
		if (updateConnectivity)
			updateConnectivity();
		if (fluidLevel != null)
			fluidLevel.tickChaser();
		if (isController())
			boiler.tick(this);
	}

	@Override
	public void lazyTick() {
		super.lazyTick();
		if (isController())
			boiler.updateOcclusion(this);
	}

	@Override
	public BlockPos getLastKnownPos() {
		return lastKnownPos;
	}

	@Override
	public boolean isController() {
		return controller == null || worldPosition.getX() == controller.getX()
			&& worldPosition.getY() == controller.getY() && worldPosition.getZ() == controller.getZ();
	}

	@Override
	public void initialize() {
		super.initialize();
		sendData();
		if (level.isClientSide)
			invalidateRenderBoundingBox();
	}

	private void onPositionChanged() {
		removeController(true);
		lastKnownPos = worldPosition;
	}

	protected void onFluidStackChanged(FluidStack newFluidStack) {
		if (!hasLevel())
			return;

		FluidType attributes = newFluidStack.getFluid()
			.getFluidType();
		int luminosity = (int) (attributes.getLightLevel(newFluidStack) / 1.2f);
		boolean reversed = attributes.isLighterThanAir();
		int maxY = (int) ((getFillState() * height) + 1);

		for (int yOffset = 0; yOffset < height; yOffset++) {
			boolean isBright = reversed ? (height - yOffset <= maxY) : (yOffset < maxY);
			int actualLuminosity = isBright ? luminosity : luminosity > 0 ? 1 : 0;

			for (int xOffset = 0; xOffset < width; xOffset++) {
				for (int zOffset = 0; zOffset < width; zOffset++) {
					BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
					FlorinolBatteryBlockEntity tankAt = ConnectivityHandler.partAt(getType(), level, pos);
					if (tankAt == null)
						continue;
					level.updateNeighbourForOutputSignal(pos, tankAt.getBlockState()
						.getBlock());
					if (tankAt.luminosity == actualLuminosity)
						continue;
					tankAt.setLuminosity(actualLuminosity);
				}
			}
		}

		if (!level.isClientSide) {
			setChanged();
			sendData();
		}

		if (isVirtual()) {
			if (fluidLevel == null)
				fluidLevel = LerpedFloat.linear()
					.startWithValue(getFillState());
			fluidLevel.chase(getFillState(), .5f, Chaser.EXP);
		}
	}

	protected void setLuminosity(int luminosity) {
		if (level.isClientSide)
			return;
		if (this.luminosity == luminosity)
			return;
		this.luminosity = luminosity;
		sendData();
	}

	@SuppressWarnings("unchecked")
	@Override
	public FlorinolBatteryBlockEntity getControllerBE() {
		if (isController() || !hasLevel())
			return this;
		BlockEntity blockEntity = level.getBlockEntity(controller);
		if (blockEntity instanceof FlorinolBatteryBlockEntity)
			return (FlorinolBatteryBlockEntity) blockEntity;
		return null;
	}

	public void applyFluidTankSize(int blocks) {
		tankInventory.setCapacity(blocks * getCapacityMultiplier());
		int overflow = tankInventory.getFluidAmount() - tankInventory.getCapacity();
		if (overflow > 0) {
			FluidStack excess = tankInventory.drain(overflow, FluidAction.EXECUTE);
			dropExcessFluidAsBuckets(excess);
		}
		forceFluidLevelUpdate = true;
	}

	private void dropExcessFluidAsBuckets(FluidStack excess) {
		if (excess.isEmpty() || level == null || level.isClientSide)
			return;
		int buckets = (excess.getAmount() + mbPerSoil() - 1) / mbPerSoil();
		for (int i = 0; i < buckets; i++)
			Block.popResource(level, worldPosition, new ItemStack(ModItems.FLORINOL_BUCKET.get()));
	}

	public void removeController(boolean keepFluids) {
		if (level.isClientSide)
			return;
		if (keepFluids && isController())
			distributeSoilToRemainingMembers();
		updateConnectivity = true;
		if (!keepFluids)
			applyFluidTankSize(1);
		controller = null;
		width = 1;
		height = 1;
		boiler.clear();
		onFluidStackChanged(tankInventory.getFluid());

		BlockState state = getBlockState();
		if (FlorinolBatteryBlock.isTank(state)) {
			state = state.setValue(FlorinolBatteryBlock.BOTTOM, true);
			state = state.setValue(FlorinolBatteryBlock.TOP, true);
			state = state.setValue(FlorinolBatteryBlock.SHAPE, window ? Shape.WINDOW : Shape.PLAIN);
			getLevel().setBlock(worldPosition, state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
		}

		refreshCapability();
		setChanged();
		sendData();
	}

	public void toggleWindows() {
		FlorinolBatteryBlockEntity be = getControllerBE();
		if (be == null)
			return;
		if (be.boiler.isActive())
			return;
		be.setWindows(!be.window);
	}

	public void updateBoilerTemperature() {
		FlorinolBatteryBlockEntity be = getControllerBE();
		if (be == null)
			return;
		if (!be.boiler.isActive())
			return;
		be.boiler.needsHeatLevelUpdate = true;
	}

	public void sendDataImmediately() {
		syncCooldown = 0;
		queuedSync = false;
		sendData();
	}

	@Override
	public void sendData() {
		if (syncCooldown > 0) {
			queuedSync = true;
			return;
		}
		super.sendData();
		queuedSync = false;
		syncCooldown = SYNC_RATE;
	}

	public void setWindows(boolean window) {
		this.window = window;
		for (int yOffset = 0; yOffset < height; yOffset++) {
			for (int xOffset = 0; xOffset < width; xOffset++) {
				for (int zOffset = 0; zOffset < width; zOffset++) {

					BlockPos pos = this.worldPosition.offset(xOffset, yOffset, zOffset);
					BlockState blockState = level.getBlockState(pos);
					if (!FlorinolBatteryBlock.isTank(blockState))
						continue;

					Shape shape = Shape.PLAIN;
					if (window) {
						// SIZE 1: Every tank has a window
						if (width == 1)
							shape = Shape.WINDOW;
						// SIZE 2: Every tank has a corner window
						if (width == 2)
							shape = xOffset == 0 ? zOffset == 0 ? Shape.WINDOW_NW : Shape.WINDOW_SW
								: zOffset == 0 ? Shape.WINDOW_NE : Shape.WINDOW_SE;
						// SIZE 3: Tanks in the center have a window
						if (width == 3 && abs(abs(xOffset) - abs(zOffset)) == 1)
							shape = Shape.WINDOW;
					}

					level.setBlock(pos, blockState.setValue(FlorinolBatteryBlock.SHAPE, shape), Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE | Block.UPDATE_KNOWN_SHAPE);
					level.getChunkSource()
						.getLightEngine()
						.checkBlock(pos);
				}
			}
		}
	}

	public void updateBoilerState() {
		if (!isController())
			return;

		boolean wasBoiler = boiler.isActive();
		boolean changed = boiler.evaluate(this);

		if (wasBoiler != boiler.isActive()) {
			if (boiler.isActive())
				setWindows(false);

			for (int yOffset = 0; yOffset < height; yOffset++)
				for (int xOffset = 0; xOffset < width; xOffset++)
					for (int zOffset = 0; zOffset < width; zOffset++)
						if (level.getBlockEntity(
							worldPosition.offset(xOffset, yOffset, zOffset)) instanceof FlorinolBatteryBlockEntity fbe)
							fbe.refreshCapability();
		}

		if (changed) {
			notifyUpdate();
			boiler.checkPipeOrganAdvancement(this);
		}
	}

	@Override
	public void setController(BlockPos controller) {
		if (level.isClientSide && !isVirtual())
			return;
		if (controller.equals(this.controller))
			return;
		this.controller = controller;
		refreshCapability();
		setChanged();
		sendData();
	}

	void refreshCapability() {
		fluidCapability = handlerForCapability();
		invalidateCapabilities();
	}

	private IFluidHandler handlerForCapability() {
		return isController() ? (boiler.isActive() ? boiler.createHandler() : new BatteryFluidHandler())
				: ((getControllerBE() != null) ? getControllerBE().handlerForCapability() : new FluidTank(0));
	}

	@Override
	public BlockPos getController() {
		return isController() ? worldPosition : controller;
	}

	@Override
	protected AABB createRenderBoundingBox() {
		if (isController())
			return super.createRenderBoundingBox().expandTowards(width - 1, height - 1, width - 1);
		else
			return super.createRenderBoundingBox();
	}

	@Nullable
	public FlorinolBatteryBlockEntity getOtherFlorinolBatteryBlockEntity(Direction direction) {
		BlockEntity otherBE = level.getBlockEntity(worldPosition.relative(direction));
		if (otherBE instanceof FlorinolBatteryBlockEntity)
			return (FlorinolBatteryBlockEntity) otherBE;
		return null;
	}

	@Override
	public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
		FlorinolBatteryBlockEntity controllerBE = getControllerBE();
		if (controllerBE == null)
			return false;
		appendFlorinolGoggleTooltip(tooltip, controllerBE);
		if (controllerBE.boiler.addToGoggleTooltip(tooltip, isPlayerSneaking, controllerBE.getTotalTankSize()))
			return true;
		containedFluidTooltip(tooltip, isPlayerSneaking,
			level.getCapability(Capabilities.FluidHandler.BLOCK, controllerBE.getBlockPos(), null));
		return true;
	}

	@Override
	protected void read(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		super.read(compound, registries, clientPacket);
		florinolSoilAmount = Math.max(0, compound.getInt("FlorinolSoilAmount"));
		soilProgress = Math.max(0, compound.getInt("SoilProgress"));

		BlockPos controllerBefore = controller;
		int prevSize = width;
		int prevHeight = height;
		int prevLum = luminosity;

		updateConnectivity = compound.contains("Uninitialized");
		luminosity = compound.getInt("Luminosity");

		lastKnownPos = null;
		if (compound.contains("LastKnownPos"))
			lastKnownPos = NBTHelper.readBlockPos(compound, "LastKnownPos");

		controller = null;
		if (compound.contains("Controller"))
			controller = NBTHelper.readBlockPos(compound, "Controller");

		if (isController()) {
			window = compound.getBoolean("Window");
			width = compound.getInt("Size");
			height = compound.getInt("Height");
			tankInventory.setCapacity(getTotalTankSize() * getCapacityMultiplier());

			tankInventory.readFromNBT(registries, compound.getCompound("TankContent"));
			if (tankInventory.getSpace() < 0) {
				FluidStack excess = tankInventory.drain(-tankInventory.getSpace(), FluidAction.EXECUTE);
				dropExcessFluidAsBuckets(excess);
			}
		}

		boiler.read(compound.getCompound("Boiler"), width * width * height);

		if (compound.contains("ForceFluidLevel") || fluidLevel == null)
			fluidLevel = LerpedFloat.linear()
				.startWithValue(getFillState());

		updateCapability = true;

		if (!clientPacket)
			return;

		boolean changeOfController = !Objects.equals(controllerBefore, controller);
		if (changeOfController || prevSize != width || prevHeight != height) {
			if (hasLevel())
				level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 16);
			if (isController())
				tankInventory.setCapacity(getCapacityMultiplier() * getTotalTankSize());
			invalidateRenderBoundingBox();
		}
		if (isController()) {
			float fillState = getFillState();
			if (compound.contains("ForceFluidLevel") || fluidLevel == null)
				fluidLevel = LerpedFloat.linear()
					.startWithValue(fillState);
			fluidLevel.chase(fillState, 0.5f, Chaser.EXP);
		}
		if (luminosity != prevLum && hasLevel())
			level.getChunkSource()
				.getLightEngine()
				.checkBlock(worldPosition);

		if (compound.contains("LazySync"))
			fluidLevel.chase(fluidLevel.getChaseTarget(), 0.125f, Chaser.EXP);
	}

	public float getFillState() {
		return (float) tankInventory.getFluidAmount() / tankInventory.getCapacity();
	}

	@Override
	public void write(CompoundTag compound, HolderLookup.Provider registries, boolean clientPacket) {
		compound.putInt("FlorinolSoilAmount", florinolSoilAmount);
		compound.putInt("SoilProgress", soilProgress);
		if (updateConnectivity)
			compound.putBoolean("Uninitialized", true);
		compound.put("Boiler", boiler.write());
		if (lastKnownPos != null)
			compound.put("LastKnownPos", NbtUtils.writeBlockPos(lastKnownPos));
		if (!isController())
			compound.put("Controller", NbtUtils.writeBlockPos(controller));
		if (isController()) {
			compound.putBoolean("Window", window);
			compound.put("TankContent", tankInventory.writeToNBT(registries, new CompoundTag()));
			compound.putInt("Size", width);
			compound.putInt("Height", height);
		}
		compound.putInt("Luminosity", luminosity);
		super.write(compound, registries, clientPacket);

		if (!clientPacket)
			return;
		if (forceFluidLevelUpdate)
			compound.putBoolean("ForceFluidLevel", true);
		if (queuedSync)
			compound.putBoolean("LazySync", true);
		forceFluidLevelUpdate = false;
	}

	@Override
	public void writeSafe(CompoundTag compound, HolderLookup.Provider registries) {
		compound.putInt("FlorinolSoilAmount", florinolSoilAmount);
		compound.putInt("SoilProgress", soilProgress);
		if (isController()) {
			compound.putBoolean("Window", window);
			compound.putInt("Size", width);
			compound.putInt("Height", height);
		}
	}

	@Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
		registerAwardables(behaviours, AllAdvancements.STEAM_ENGINE_MAXED, AllAdvancements.PIPE_ORGAN);
	}

	public FluidTank getTankInventory() {
		return tankInventory;
	}

	public int getTotalTankSize() {
		return width * width * height;
	}

	public static int getMaxSize() {
		return FIXED_WIDTH;
	}

	public static int getCapacityMultiplier() {
		return capacityPerBlock();
	}

	public static int getMaxHeight() {
		return AllConfigs.server().fluids.fluidTankMaxHeight.get();
	}

	public LerpedFloat getFluidLevel() {
		return fluidLevel;
	}

	public void setFluidLevel(LerpedFloat fluidLevel) {
		this.fluidLevel = fluidLevel;
	}

	@Override
	public void preventConnectivityUpdate() {
		updateConnectivity = false;
	}

	@Override
	public void notifyMultiUpdated() {
		BlockState state = this.getBlockState();
		if (FlorinolBatteryBlock.isTank(state)) { // safety
			state = state.setValue(FlorinolBatteryBlock.BOTTOM, getController().getY() == getBlockPos().getY());
			state = state.setValue(FlorinolBatteryBlock.TOP, getController().getY() + height - 1 == getBlockPos().getY());
			level.setBlock(getBlockPos(), state, Block.UPDATE_CLIENTS | Block.UPDATE_INVISIBLE);
		}
		if (isController())
			setWindows(window);
		onFluidStackChanged(tankInventory.getFluid());
		updateBoilerState();
		setChanged();
		if (isController())
			collectSoilFromMembers();
	}

	@Override
	public void setExtraData(@Nullable Object data) {
		if (data instanceof Boolean)
			window = (boolean) data;
	}

	@Override
	@Nullable
	public Object getExtraData() {
		return window;
	}

	@Override
	public Object modifyExtraData(Object data) {
		if (data instanceof Boolean windows) {
			windows |= window;
			return windows;
		}
		return data;
	}

	@Override
	public Direction.Axis getMainConnectionAxis() {
		return Direction.Axis.Y;
	}

	@Override
	public int getMaxLength(Direction.Axis longAxis, int width) {
		if (longAxis == Direction.Axis.Y)
			return Math.max(1, maxHeight());
		return getMaxWidth();
	}

	@Override
	public int getMaxWidth() {
		return FIXED_WIDTH;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void setHeight(int height) {
		this.height = height;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public void setWidth(int width) {
		this.width = width;
	}

	@Override
	public boolean hasTank() {
		return true;
	}

	@Override
	public int getTankSize(int tank) {
		return getCapacityMultiplier();
	}

	@Override
	public void setTankSize(int tank, int blocks) {
		applyFluidTankSize(blocks);
	}

	@Override
	public IFluidTank getTank(int tank) {
		return tankInventory;
	}

	@Override
	public FluidStack getFluid(int tank) {
		return tankInventory.getFluid()
			.copy();
	}
	private static boolean isFlorinol(FluidStack stack) {
		return !stack.isEmpty() && (stack.getFluid() == ModFluids.FLORINOL.get()
				|| stack.getFluid() == ModFluids.FLOWING_FLORINOL.get());
	}

	public int getTotalCapacityMb() {
		return Math.min(getTotalTankSize(), maxBlocks()) * capacityPerBlock();
	}

	public int getStoredFlorinolMb() {
		return isFlorinol(tankInventory.getFluid()) ? tankInventory.getFluidAmount() : 0;
	}

	public int getFlorinolSoilAmount() {
		return florinolSoilAmount;
	}

	public int getSoilProgress() {
		return soilProgress;
	}

	private int getUsedCapacityMb() {
		return getStoredFlorinolMb() + florinolSoilAmount * mbPerSoil() + soilProgress;
	}

	private int fillFlorinol(FluidStack resource, IFluidHandler.FluidAction action) {
		if (!isFlorinol(resource))
			return 0;
		int free = Math.max(0, getTotalCapacityMb() - getUsedCapacityMb());
		int accepted = Math.min(resource.getAmount(), free);
		if (accepted <= 0)
			return 0;
		if (action.execute()) {
			int filled = tankInventory.fill(new FluidStack(ModFluids.FLORINOL.get(), accepted), action);
			if (filled > 0)
				notifyBatteryChanged();
			return filled;
		}
		return accepted;
	}
	private FluidStack drainBattery(FluidStack resource, IFluidHandler.FluidAction action) {
		if (resource.isEmpty() || tankInventory.isEmpty()
				|| !FluidStack.isSameFluidSameComponents(tankInventory.getFluid(), resource))
			return FluidStack.EMPTY;
		FluidStack drained = tankInventory.drain(resource.getAmount(), action);
		if (action.execute() && !drained.isEmpty())
			notifyBatteryChanged();
		return drained;
	}

	private FluidStack drainBattery(int maxDrain, IFluidHandler.FluidAction action) {
		if (maxDrain <= 0 || tankInventory.isEmpty())
			return FluidStack.EMPTY;
		FluidStack drained = tankInventory.drain(maxDrain, action);
		if (action.execute() && !drained.isEmpty())
			notifyBatteryChanged();
		return drained;
	}

	private int extractFe(int maxExtract, boolean simulate) {
		if (!isFlorinol(tankInventory.getFluid()))
			return 0;
		int requestedMb = Math.max(0, maxExtract) / fePerMb();
		int amount = Math.min(requestedMb, tankInventory.getFluidAmount());
		if (amount <= 0)
			return 0;
		if (simulate)
			return amount * fePerMb();

		FluidStack drained = tankInventory.drain(amount, IFluidHandler.FluidAction.EXECUTE);
		if (drained.isEmpty())
			return 0;
		soilProgress += drained.getAmount();
		settleSoil();
		notifyBatteryChanged();
		return drained.getAmount() * fePerMb();
	}

	private void settleSoil() {
		while (soilProgress >= mbPerSoil()) {
			soilProgress -= mbPerSoil();
			florinolSoilAmount++;
		}
	}

	private void notifyBatteryChanged() {
		setChanged();
		sendData();
	}

	private void appendFlorinolGoggleTooltip(List<Component> tooltip, FlorinolBatteryBlockEntity controller) {
		tooltip.add(Component.literal("    ").append(Component.translatable("goggles.createsandustry.florinol_battery.title")));
		tooltip.add(Component.translatable("goggles.createsandustry.florinol_battery.energy_label").withStyle(ChatFormatting.GRAY));
		tooltip.add(buildEnergyBar(controller));
	}

	private MutableComponent buildEnergyBar(FlorinolBatteryBlockEntity controller) {
		int capacity = Math.max(1, controller.getTotalCapacityMb());
		int blueMb = Math.min(controller.getStoredFlorinolMb(), capacity);
		int purpleMb = controller.florinolSoilAmount * mbPerSoil() + controller.soilProgress;
		purpleMb = Math.min(purpleMb, capacity - blueMb);

		int segments = 10;
		int blueSegments = Math.min(segments, Math.round(blueMb * segments / (float) capacity));
		int purpleSegments = Math.min(segments - blueSegments, Math.round(purpleMb * segments / (float) capacity));
		int emptySegments = Math.max(0, segments - blueSegments - purpleSegments);

		MutableComponent bar = Component.literal("[");
		bar.append(Component.literal("⚡".repeat(blueSegments)).withStyle(ChatFormatting.AQUA));
		bar.append(Component.literal("█".repeat(purpleSegments)).withStyle(ChatFormatting.DARK_PURPLE));
		bar.append(Component.literal("░".repeat(emptySegments)).withStyle(ChatFormatting.DARK_GRAY));
		bar.append(Component.literal("]"));
		return bar;
	}
	private int getLocalSoilUsedMb() {
		return florinolSoilAmount * mbPerSoil() + soilProgress;
	}

	private void setLocalSoilUsedMb(int amount) {
		int safe = Math.max(0, amount);
		florinolSoilAmount = safe / mbPerSoil();
		soilProgress = safe % mbPerSoil();
	}

	private void collectSoilFromMembers() {
		if (level == null || level.isClientSide || !isController())
			return;
		int[] total = {0};
		forEachStructurePart(part -> {
			total[0] += part.getLocalSoilUsedMb();
			if (part != this)
				part.setLocalSoilUsedMb(0);
		});
		setLocalSoilUsedMb(total[0]);
		notifyBatteryChanged();
	}

	private void distributeSoilToRemainingMembers() {
		List<FlorinolBatteryBlockEntity> members = new ArrayList<>();
		forEachStructurePart(part -> {
			if (part != this || !isRemoved())
				members.add(part);
		});

		if (members.isEmpty()) {
			dropLocalResources();
			return;
		}

		int total = getLocalSoilUsedMb();
		if (total <= 0)
			return;

		int share = total / members.size();
		int remainder = total % members.size();
		for (int i = 0; i < members.size(); i++)
			members.get(i).setLocalSoilUsedMb(share + (i < remainder ? 1 : 0));
		if (!members.contains(this))
			setLocalSoilUsedMb(0);
	}

	private void dropLocalResources() {
		if (level == null || level.isClientSide)
			return;

		int soilItems = florinolSoilAmount + (soilProgress > 0 ? 1 : 0);
		for (int i = 0; i < soilItems; i++)
			Block.popResource(level, worldPosition, new ItemStack(ModItems.FLORINOL_SOIL.get()));

		if (isFlorinol(tankInventory.getFluid())) {
			int buckets = (tankInventory.getFluidAmount() + mbPerSoil() - 1) / mbPerSoil();
			for (int i = 0; i < buckets; i++)
				Block.popResource(level, worldPosition, new ItemStack(ModItems.FLORINOL_BUCKET.get()));
			tankInventory.setFluid(FluidStack.EMPTY);
		}
		florinolSoilAmount = 0;
		soilProgress = 0;
	}

	private void forEachStructurePart(Consumer<FlorinolBatteryBlockEntity> consumer) {
		if (level == null)
			return;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				for (int z = 0; z < width; z++) {
					BlockEntity blockEntity = level.getBlockEntity(worldPosition.offset(x, y, z));
					if (blockEntity instanceof FlorinolBatteryBlockEntity battery)
						consumer.accept(battery);
				}
			}
		}
	}

	private class BatteryFluidHandler implements IFluidHandler {
		@Override
		public int getTanks() {
			return 1;
		}

		@Override
		public FluidStack getFluidInTank(int tank) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? FluidStack.EMPTY : controller.tankInventory.getFluid().copy();
		}

		@Override
		public int getTankCapacity(int tank) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? 0 : controller.getTotalCapacityMb();
		}

		@Override
		public boolean isFluidValid(int tank, FluidStack stack) {
			return isFlorinol(stack);
		}

		@Override
		public int fill(FluidStack resource, FluidAction action) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? 0 : controller.fillFlorinol(resource, action);
		}

		@Override
		public FluidStack drain(FluidStack resource, FluidAction action) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? FluidStack.EMPTY : controller.drainBattery(resource, action);
		}

		@Override
		public FluidStack drain(int maxDrain, FluidAction action) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? FluidStack.EMPTY : controller.drainBattery(maxDrain, action);
		}
	}

	private class BatteryEnergyStorage implements IEnergyStorage {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			return 0;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? 0 : controller.extractFe(maxExtract, simulate);
		}

		@Override
		public int getEnergyStored() {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? 0 : controller.getStoredFlorinolMb() * fePerMb();
		}

		@Override
		public int getMaxEnergyStored() {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			return controller == null ? 0 : controller.getTotalCapacityMb() * fePerMb();
		}

		@Override
		public boolean canExtract() {
			return true;
		}

		@Override
		public boolean canReceive() {
			return false;
		}
	}


	private class BatterySoilItemHandler implements IItemHandler {
		@Override
		public int getSlots() {
			return 1;
		}

		@Override
		public ItemStack getStackInSlot(int slot) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			if (controller == null || slot != 0 || controller.florinolSoilAmount <= 0)
				return ItemStack.EMPTY;
			return new ItemStack(ModItems.FLORINOL_SOIL.get(), Math.min(64, controller.florinolSoilAmount));
		}

		@Override
		public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
			return stack;
		}

		@Override
		public ItemStack extractItem(int slot, int amount, boolean simulate) {
			FlorinolBatteryBlockEntity controller = getControllerBE();
			if (controller == null || slot != 0 || amount <= 0 || controller.florinolSoilAmount <= 0)
				return ItemStack.EMPTY;
			int extracted = Math.min(Math.min(amount, 64), controller.florinolSoilAmount);
			if (!simulate) {
				controller.florinolSoilAmount -= extracted;
				controller.notifyBatteryChanged();
			}
			return new ItemStack(ModItems.FLORINOL_SOIL.get(), extracted);
		}

		@Override
		public int getSlotLimit(int slot) {
			return 64;
		}

		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return false;
		}
	}

}





