package com.minechai.create_sandustry.content.kineticpress;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.minechai.create_sandustry.registry.ModBlockEntities;
import com.minechai.create_sandustry.registry.ModTags;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;

import net.createmod.catnip.math.VecHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Server-authoritative Kinetic Press base, FE storage, binding and processing host. */
public class KineticPressBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation, Container {
    public static final int MAX_ENERGY = 3_000;
    public static final int STRUCTURE_SCAN_INTERVAL = 20;
    public static final int CLIENT_SYNC_INTERVAL = 4;

    private static final String WORKING_HEIGHT_TAG = "workingHeight";
    private static final String ENERGY_TAG = "energy";
    private static final String STRUCTURE_VALID_TAG = "structureValid";
    private static final String STRUCTURE_FAILURE_TAG = "structureFailure";
    private static final String BOUND_INPUT_TAG = "BoundInput";
    private static final String BOUND_INPUT_TYPE_TAG = "BoundInputType";

    private final KineticPressEnergyStorage energyStorage;
    private final KineticPressProcessor processor;
    public KineticPressValueBehaviour workingHeightBehaviour;

    private int workingHeight = KineticPressStructure.DEFAULT_WORKING_HEIGHT;
    private int energyStored;
    private boolean structureValid;
    private KineticPressStructure.FailureReason structureFailure = KineticPressStructure.FailureReason.NONE;
    private BlockPos blockingPosition;
    private BlockPos boundInputPos;
    private ResourceLocation boundInputType;
    private boolean contentsReleased;
    private boolean syncDirty;
    private boolean syncUrgent;
    private long lastClientSyncTick = Long.MIN_VALUE;
    private long lastAcceptedClientRevision = Long.MIN_VALUE;
    private int clientSyncCount;
    private KineticPressProcessor.RunState lastRunState;
    private boolean lookupUnloaded;

    public KineticPressBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.KINETIC_PRESS.get(), pos, state);
    }

    public KineticPressBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.energyStorage = new KineticPressEnergyStorage(this);
        this.processor = new KineticPressProcessor(this);
        setLazyTickRate(STRUCTURE_SCAN_INTERVAL);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        workingHeightBehaviour = new KineticPressValueBehaviour(
                Component.translatable("block.createsandustry.kinetic_press"),
                this,
                new WorkingHeightValueBox());
        workingHeightBehaviour.between(
                KineticPressStructure.MIN_WORKING_HEIGHT,
                KineticPressStructure.MAX_WORKING_HEIGHT);
        workingHeightBehaviour.value = KineticPressStructure.DEFAULT_WORKING_HEIGHT;
        workingHeightBehaviour.withFormatter(value -> String.valueOf(value));
        behaviours.add(workingHeightBehaviour);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) {
            return;
        }
        if (level.isClientSide) {
            processor.tickClientAnimation();
            return;
        }
        processor.tick();
        KineticPressProcessor.RunState currentState = processor.getRunState();
        if (currentState != lastRunState) {
            lastRunState = currentState;
            sendDataImmediately();
        }
        flushClientSync();
    }

    /** Create already coalesces blockChanged calls; additionally throttle routine snapshots. */
    @Override
    public void sendData() {
        syncDirty = true;
    }

    public void sendDataImmediately() {
        syncDirty = true;
        syncUrgent = true;
    }

    private void flushClientSync() {
        if (level == null || level.isClientSide || !syncDirty) return;
        long now = level.getGameTime();
        if (!syncUrgent && lastClientSyncTick != Long.MIN_VALUE
                && now - lastClientSyncTick < CLIENT_SYNC_INTERVAL) return;
        super.sendData();
        clientSyncCount++;
        lastClientSyncTick = now;
        syncDirty = false;
        syncUrgent = false;
    }

    /** Number of chunk refresh requests, not a measurement of actual network packets. */
    public int getClientSyncCount() {
        return clientSyncCount;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        requestStructureCheck();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lookupUnloaded = false;
        refreshLookup();
    }

    @Override
    public void onChunkUnloaded() {
        lookupUnloaded = true;
        KineticPressLookup.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    public void invalidate() {
        lookupUnloaded = true;
        KineticPressLookup.remove(this);
        super.invalidate();
    }

    @Override
    public void remove() {
        lookupUnloaded = true;
        KineticPressLookup.remove(this);
        super.remove();
    }

    private void refreshLookup() {
        if (!lookupUnloaded && level != null) KineticPressLookup.update(this);
    }

    /** Called by Create only for real block removal, never ordinary chunk unload. */
    @Override
    public void destroy() {
        lookupUnloaded = true;
        KineticPressLookup.remove(this);
        releaseContentsForRemoval();
        super.destroy();
    }

    public void releaseContentsForRemoval() {
        if (level == null || level.isClientSide || contentsReleased) return;
        contentsReleased = true;
        for (ItemStack stack : processor.drainForRemoval()) {
            net.minecraft.world.Containers.dropItemStack(level,
                    worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                    worldPosition.getZ() + 0.5D, stack);
        }
        setChanged();
    }

    public void requestStructureCheck() {
        if (level == null || level.isClientSide) {
            return;
        }

        KineticPressStructure.ValidationResult result =
                KineticPressStructure.validate(level, worldPosition, workingHeight);
        boolean changed = result.valid() != structureValid
                || result.reason() != structureFailure
                || !Objects.equals(result.blockingPosition(), blockingPosition);

        structureValid = result.valid();
        structureFailure = result.reason();
        blockingPosition = result.blockingPosition();

        if (boundInputPos != null
                && !level.getBlockState(boundInputPos).is(ModTags.BlockTags.KINETIC_PRESS_INPUTS)) {
            clearBindingInternal();
            changed = true;
        }
        if (structureValid && result.inputPosition() != null
                && (!Objects.equals(boundInputPos, result.inputPosition())
                || !Objects.equals(boundInputType, blockId(level.getBlockState(result.inputPosition()))))) {
            bindInputInternal(result.inputPosition(), level.getBlockState(result.inputPosition()));
            changed = true;
        }

        if (changed) {
            setChanged();
            sendDataImmediately();
        }
        syncWorkingState();
        refreshLookup();
    }

    public boolean trySetWorkingHeight(Player player, int newHeight) {
        int clamped = Mth.clamp(newHeight,
                KineticPressStructure.MIN_WORKING_HEIGHT,
                KineticPressStructure.MAX_WORKING_HEIGHT);
        if (clamped == workingHeight) {
            return true;
        }
        if (level == null || level.isClientSide) {
            return false;
        }
        if (processor.isHeightChangeLocked()) {
            player.displayClientMessage(Component.translatable(
                    "message.createsandustry.kinetic_press.height_change_locked")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }

        if (boundInputPos != null && !hasBoundInput()) {
            clearBindingInternal();
        }
        if (hasBoundInput()) {
            KineticPressBindingHandler.MoveResult result =
                    KineticPressBindingHandler.moveBoundInput(this, clamped, player);
            if (!result.success()) {
                showMoveFailure(player, result);
                return false;
            }
        }

        workingHeight = clamped;
        if (workingHeightBehaviour != null) {
            workingHeightBehaviour.value = clamped;
        }
        setChanged();
        sendDataImmediately();
        requestStructureCheck();
        return true;
    }

    private static void showMoveFailure(Player player,
                                         KineticPressBindingHandler.MoveResult result) {
        Component reason = switch (result.failure()) {
            case NO_PERMISSION -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.no_permission");
            case MISSING_INPUT -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.missing_input");
            case OUT_OF_WORLD -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.out_of_world");
            case TARGET_OCCUPIED -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.target_occupied");
            case PATH_BLOCKED -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.path_blocked");
            case CANNOT_SURVIVE -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.cannot_survive");
            case MOVE_FAILED, NONE -> Component.translatable(
                    "gui.createsandustry.kinetic_press.move.failed");
        };
        String pos = result.pos() == null ? "-" : result.pos().toShortString();
        player.displayClientMessage(Component.translatable(
                "message.createsandustry.kinetic_press.input_move_failed", reason, pos)
                .withStyle(ChatFormatting.RED), true);
    }

    @Override
    public int getContainerSize() {
        return KineticPressOutputHandler.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (!processor.getOutputHandler().getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return processor.getOutputHandler().getStackInSlot(slot).copy();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return processor.getOutputHandler().extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return processor.getOutputHandler().extractItem(slot, Integer.MAX_VALUE, false);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        // Vanilla Container access is output-extraction only.
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < getContainerSize(); i++) {
            processor.getOutputHandler().setStackInSlot(i, ItemStack.EMPTY);
        }
    }
    public boolean isPowered() {
        if (level == null) {
            return false;
        }
        BlockState state = level.getBlockState(worldPosition);
        return (state.hasProperty(KineticPressBlock.POWERED)
                && state.getValue(KineticPressBlock.POWERED))
                || level.hasNeighborSignal(worldPosition);
    }
    public void syncWorkingState() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean working = processor != null && processor.isWorking();
        BlockState state = level.getBlockState(worldPosition);
        if (state.getBlock() instanceof KineticPressBlock
                && state.getValue(KineticPressBlock.WORKING) != working) {
            level.setBlock(worldPosition,
                    state.setValue(KineticPressBlock.WORKING, working),
                    Block.UPDATE_CLIENTS);
        }
    }

    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (level == null || level.isClientSide || maxReceive <= 0) {
            return 0;
        }
        int accepted = Math.min(maxReceive, MAX_ENERGY - energyStored);
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            energyStored += accepted;
            setChanged();
            sendData();
        }
        return accepted;
    }

    public int extractEnergy(int maxExtract, boolean simulate) {
        return 0;
    }

    public boolean hasEnergy(int amount) {
        return amount >= 0 && energyStored >= amount;
    }

    public boolean consumeEnergy(int amount) {
        if (level == null || level.isClientSide || amount < 0 || energyStored < amount) {
            return false;
        }
        if (amount == 0) {
            return true;
        }
        energyStored -= amount;
        setChanged();
        sendData();
        return true;
    }

    public int getEnergyStored() {
        return energyStored;
    }

    public int getMaxEnergyStored() {
        return MAX_ENERGY;
    }

    public boolean canReceiveEnergy() {
        return level != null && !level.isClientSide && energyStored < MAX_ENERGY;
    }

    public boolean canExtractEnergy() {
        return false;
    }

    public boolean canAcceptInput() {
        return processor != null && processor.canAcceptInput();
    }

    public void onInputInserted() {
        processor.onInputInserted();
    }

    public int returnQueuedInputs(Player player) {
        if (level == null || level.isClientSide) {
            return 0;
        }
        int count = processor.returnQueuedInputs(player);
        player.displayClientMessage(Component.translatable(count > 0
                ? "message.createsandustry.kinetic_press.returned_inputs"
                : "message.createsandustry.kinetic_press.no_queued_inputs", count), true);
        if (count > 0) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
        }
        return count;
    }

    public void onOutputChanged() {
        if (level != null && !level.isClientSide) {
            setChanged();
            sendData();
        }
    }

    public int getWorkingHeight() {
        return workingHeight;
    }

    public BlockPos getInputPosition() {
        return KineticPressStructure.getInputPosition(worldPosition, workingHeight);
    }

    public boolean isStructureValid() {
        return structureValid;
    }

    public KineticPressStructure.FailureReason getStructureFailure() {
        return structureFailure;
    }

    public BlockPos getBlockingPosition() {
        return blockingPosition;
    }

    public KineticPressEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public KineticPressInputHandler getInputHandler() {
        return processor.getInputHandler();
    }

    public KineticPressOutputHandler getOutputHandler() {
        return processor.getOutputHandler();
    }

    public KineticPressProcessor getProcessor() {
        return processor;
    }

    public BlockPos getBoundInputPos() {
        return boundInputPos;
    }

    public ResourceLocation getBoundInputType() {
        return boundInputType;
    }

    public boolean hasBoundInput() {
        return level != null && boundInputPos != null
                && level.getBlockState(boundInputPos).is(ModTags.BlockTags.KINETIC_PRESS_INPUTS);
    }

    public boolean isBoundToInput(BlockPos inputPos) {
        return inputPos != null && inputPos.equals(boundInputPos);
    }

    public void bindInput(BlockPos pos, BlockState state) {
        bindInputInternal(pos, state);
        setChanged();
        sendDataImmediately();
    }

    private void bindInputInternal(BlockPos pos, BlockState state) {
        boundInputPos = pos.immutable();
        boundInputType = blockId(state);
        refreshLookup();
    }

    private void clearBindingInternal() {
        boundInputPos = null;
        boundInputType = null;
        refreshLookup();
    }

    private static ResourceLocation blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock());
    }

    public static KineticPressBlockEntity findForBoundChute(Level level, BlockPos chutePos) {
        return KineticPressLookup.findForInput(level, chutePos);
    }

    public List<KineticPressAnimationSnapshot> getAnimationSnapshots() {
        return processor.getAnimationSnapshots();
    }

    public List<KineticPressAnimationRenderState> getAnimationRenderStates() {
        return processor.getAnimationRenderStates();
    }

    public boolean hasActiveAnimationFor(ItemStack stack) {
        return processor.hasActiveAnimationFor(stack);
    }

    public int getAnimationMaxWorkingHeight() {
        return processor.getAnimationMaxWorkingHeight();
    }

    public int getProcessingTotalTime() {
        return processor.getProcessingTotalTime();
    }

    public boolean hasActiveAnimation() {
        return processor.isProcessing();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(indent(Component.translatable("block.createsandustry.kinetic_press")
                .withStyle(ChatFormatting.WHITE)));
        tooltip.add(indent(Component.translatable("gui.createsandustry.kinetic_press.goggles.energy",
                energyStored, MAX_ENERGY).withStyle(ChatFormatting.AQUA)));
        tooltip.add(indent(Component.translatable("gui.createsandustry.kinetic_press.goggles.height",
                workingHeight).withStyle(ChatFormatting.GRAY)));
        tooltip.add(indent(Component.translatable("gui.createsandustry.kinetic_press.goggles.status",
                Component.translatable("gui.createsandustry.kinetic_press.state."
                        + processor.getRunState().name().toLowerCase(Locale.ROOT)))
                .withStyle(structureValid ? ChatFormatting.YELLOW : ChatFormatting.RED)));
        if (processor.getBatchTotal() > 0) {
            tooltip.add(indent(Component.translatable("gui.createsandustry.kinetic_press.goggles.batch",
                    processor.getBatchCompleted(), processor.getBatchTotal()).withStyle(ChatFormatting.GRAY)));
        }
        if (!structureValid && structureFailure != KineticPressStructure.FailureReason.NONE) {
            tooltip.add(indent(Component.translatable("gui.createsandustry.kinetic_press.goggles.structure_failure",
                    Component.translatable("gui.createsandustry.kinetic_press.structure."
                            + structureFailure.name().toLowerCase(Locale.ROOT)),
                    blockingPosition == null ? "-" : blockingPosition.toShortString()).withStyle(ChatFormatting.RED)));
        }
        return true;
    }

    private static Component indent(Component component) {
        return Component.literal("    ").append(component);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt(WORKING_HEIGHT_TAG, workingHeight);
        tag.putInt(ENERGY_TAG, energyStored);
        tag.putBoolean(STRUCTURE_VALID_TAG, structureValid);
        tag.putString(STRUCTURE_FAILURE_TAG, structureFailure.name());
        if (boundInputPos != null) {
            tag.putLong(BOUND_INPUT_TAG, boundInputPos.asLong());
        }
        if (blockingPosition != null) tag.putLong("BlockingPosition", blockingPosition.asLong());
        if (!clientPacket) {
            if (boundInputType != null) {
                tag.putString(BOUND_INPUT_TYPE_TAG, boundInputType.toString());
            }
        }
        processor.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket) {
            long revision = tag.getLong("AnimationRevision");
            if (revision < lastAcceptedClientRevision) return;
            lastAcceptedClientRevision = revision;
        }
        super.read(tag, registries, clientPacket);

        workingHeight = tag.contains(WORKING_HEIGHT_TAG)
                ? Mth.clamp(tag.getInt(WORKING_HEIGHT_TAG),
                        KineticPressStructure.MIN_WORKING_HEIGHT,
                        KineticPressStructure.MAX_WORKING_HEIGHT)
                : KineticPressStructure.DEFAULT_WORKING_HEIGHT;
        if (workingHeightBehaviour != null) {
            workingHeightBehaviour.value = workingHeight;
        }
        energyStored = Mth.clamp(tag.getInt(ENERGY_TAG), 0, MAX_ENERGY);
        structureValid = tag.getBoolean(STRUCTURE_VALID_TAG);
        structureFailure = readFailureReason(tag.getString(STRUCTURE_FAILURE_TAG));

        boundInputPos = tag.contains(BOUND_INPUT_TAG)
                ? BlockPos.of(tag.getLong(BOUND_INPUT_TAG)) : null;
        blockingPosition = tag.contains("BlockingPosition")
                ? BlockPos.of(tag.getLong("BlockingPosition")) : null;
        if (!clientPacket) {
            boundInputType = tag.contains(BOUND_INPUT_TYPE_TAG)
                    ? ResourceLocation.tryParse(tag.getString(BOUND_INPUT_TYPE_TAG))
                    : null;
        }
        processor.read(tag, registries, clientPacket);
        refreshLookup();
    }

    private static KineticPressStructure.FailureReason readFailureReason(String name) {
        try {
            return KineticPressStructure.FailureReason.valueOf(name);
        } catch (IllegalArgumentException exception) {
            return KineticPressStructure.FailureReason.NONE;
        }
    }

    /** Revalidate only bases that can own a block changed in their vertical column. */
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) {
            return;
        }
        KineticPressLookup.notifyColumnChange(level, event.getPos());
    }

    private static final class WorkingHeightValueBox extends ValueBoxTransform.Sided {
        @Override
        protected Vec3 getSouthLocation() {
            return VecHelper.voxelSpace(8.0, 12.0, 15.5);
        }

        @Override
        protected boolean isSideActive(BlockState state, Direction direction) {
            return direction.getAxis().isHorizontal();
        }

        @Override
        public float getScale() {
            return 0.6F;
        }
    }
}
