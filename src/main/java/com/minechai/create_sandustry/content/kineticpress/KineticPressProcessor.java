package com.minechai.create_sandustry.content.kineticpress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModRecipeTypes;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/** Server state machine with up to 16 independent concurrent processing tasks per batch. */
public final class KineticPressProcessor {
    public static final int MAX_BATCH_ITEMS = 16;
    public static final int INPUT_IDLE_TICKS = 10;
    private static final String TASK_LIST_TAG = "ActiveTasks";

    private final KineticPressBlockEntity blockEntity;
    private final KineticPressInputHandler inputHandler;
    private final KineticPressOutputHandler outputHandler;
    private final KineticPressOutputManager outputManager;
    private final KineticPressSounds sounds = new KineticPressSounds();

    private final List<ActiveTask> activeTasks = new ArrayList<>(MAX_BATCH_ITEMS);
    private List<KineticPressAnimationSnapshot> clientAnimations = List.of();
    private final KineticPressAnimationTimeline clientTimeline = new KineticPressAnimationTimeline();
    private long clientAnimationRevision = Long.MIN_VALUE;
    private final List<ItemStack> pendingOutputs = new ArrayList<>();

    private int inputIdleTicks;
    private boolean batchLocked;
    private int nextTaskId = 1;
    private final List<CachedRecipe> recipeCache = new ArrayList<>();
    private Collection<RecipeHolder<?>> cachedRecipeSet;
    private long recipeLookupCount;
    private int nextInputSlot;
    private int batchTotal;
    private int batchCompleted;
    private boolean hasClientDiagnostics;
    private RunState clientRunState = RunState.IDLE;
    private int clientInputCount;
    private int clientActiveTaskCount;
    private int clientRemainingTicks;
    private EnergyDiagnostics clientEnergyDiagnostics = new EnergyDiagnostics(0, 0, 0, 0);

    public KineticPressProcessor(KineticPressBlockEntity blockEntity) {
        this.blockEntity = blockEntity;
        this.inputHandler = new KineticPressInputHandler(blockEntity);
        this.outputHandler = new KineticPressOutputHandler(blockEntity);
        this.outputManager = new KineticPressOutputManager(blockEntity, outputHandler);
    }

    public void tick() {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        boolean powered = blockEntity.isPowered();
        outputManager.tick(powered);
        if (powered || !blockEntity.isStructureValid()) {
            blockEntity.syncWorkingState();
            return;
        }

        if (isCollecting()) {
            inputIdleTicks--;
            if (inputIdleTicks <= 0) {
                inputIdleTicks = 0;
                batchLocked = true;
                markChangedAndSync();
            }
        }

        flushPendingOutputs();
        boolean completed = tickActiveTasks();
        flushPendingOutputs();
        boolean started = batchLocked && startAffordableTasks(level);
        sounds.tick(level, blockEntity.getBlockPos(), started, completed);

        if (batchLocked && activeTasks.isEmpty() && pendingOutputs.isEmpty()
                && !hasProcessableInput(level)) {
            endBatch();
        }
        blockEntity.syncWorkingState();
    }

    /** Advances only the bounded display clock, never authoritative task progress. */
    public void tickClientAnimation() {
        clientTimeline.tick();
    }

    private boolean tickActiveTasks() {
        if (activeTasks.isEmpty()) {
            return false;
        }

        boolean changed = false;
        boolean completed = false;
        Iterator<ActiveTask> iterator = activeTasks.iterator();
        while (iterator.hasNext()) {
            ActiveTask task = iterator.next();
            task.progress++;
            changed = true;
            if (task.progress >= task.totalTime) {
                for (ItemStack result : task.rolledResults) {
                    if (!result.isEmpty()) {
                        pendingOutputs.add(result.copy());
                    }
                }
                iterator.remove();
                batchCompleted++;
                completed = true;
            }
        }

        if (changed) {
            blockEntity.setChanged();
            if (completed) blockEntity.sendDataImmediately();
            else blockEntity.sendData();
        }
        return completed;
    }

    private boolean startAffordableTasks(Level level) {
        if (!blockEntity.isStructureValid() || activeTasks.size() >= MAX_BATCH_ITEMS) {
            return false;
        }

        boolean started = false;
        int slotCount = Math.min(MAX_BATCH_ITEMS, inputHandler.getSlots());
        int startSlot = Math.floorMod(nextInputSlot, slotCount);
        // Examine each queue slot at most once. The next pass starts after the
        // last accepted candidate, so a temporarily blocked slot cannot hold up
        // affordable recipes elsewhere in a mixed batch.
        for (int checked = 0; checked < slotCount && activeTasks.size() < MAX_BATCH_ITEMS; checked++) {
            int slot = (startSlot + checked) % slotCount;
            ItemStack stack = inputHandler.getInternal(slot);
            if (stack.isEmpty() || stack.getCount() != 1) {
                continue;
            }

            RecipeHolder<KineticPressRecipe> holder = findRecipe(level, stack);
            if (holder == null) {
                continue;
            }
            KineticPressRecipe recipe = holder.value();
            int energyCost = recipe.calculateEnergyCost(blockEntity.getWorkingHeight(),
                    ModConfigs.KINETIC_PRESS_EXTRA_ENERGY_PER_HEIGHT.get());

            if (!blockEntity.hasEnergy(energyCost)
                    || !canReserveOutputCapacity(recipe)) {
                continue;
            }

            // Finish all allocation and random-result preparation while the
            // queue still owns the input and no FE has been deducted.
            ActiveTask task = new ActiveTask();
            task.id = nextTaskId;
            task.inputItem = stack.copy();
            task.recipeId = holder.id();
            task.progress = 0;
            task.totalTime = Math.max(1, recipe.processingTime());
            task.workingHeight = blockEntity.getWorkingHeight();
            task.energyCost = energyCost;
            task.requiredHeight = recipe.requiredHeight();
            task.baseEnergy = recipe.baseEnergy();
            task.maximumOutputs.addAll(recipe.getMaximumOutputStacks());
            for (ItemStack result : recipe.rollResults(level.random)) {
                if (!result.isEmpty()) {
                    task.rolledResults.add(result.copy());
                }
            }

            // Server-thread commit: removeOne has no callbacks and activeTasks
            // has space preallocated for all 16 tasks. No recipe code, random
            // roll or item copying is invoked after the energy deduction.
            if (!ItemStack.matches(task.inputItem, inputHandler.getInternal(slot))
                    || !blockEntity.hasEnergy(energyCost)
                    || !blockEntity.consumeEnergy(energyCost)) {
                continue;
            }
            task.inputItem = inputHandler.removeOne(slot);
            activeTasks.add(task);
            nextTaskId++;
            nextInputSlot = (slot + 1) % slotCount;
            started = true;
        }

        if (started) {
            markChangedAndSync();
        }
        return started;
    }

    private boolean canReserveOutputCapacity(KineticPressRecipe recipe) {
        List<ItemStack> reserved = new ArrayList<>(pendingOutputs);
        for (ActiveTask task : activeTasks) {
            reserved.addAll(task.maximumOutputs);
        }
        reserved.addAll(recipe.getMaximumOutputStacks());
        return outputHandler.canAcceptAll(reserved);
    }

    private boolean hasProcessableInput(Level level) {
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack stack = inputHandler.getInternal(slot);
            if (!stack.isEmpty() && findRecipe(level, stack) != null) {
                return true;
            }
        }
        return false;
    }

    private RecipeHolder<KineticPressRecipe> findRecipe(Level level, ItemStack stack) {
        // RecipeManager replaces its immutable byName map on reload and replaceRecipes.
        // ImmutableMap caches its values view, so this token is stable between reloads.
        Collection<RecipeHolder<?>> recipes = level.getRecipeManager().getRecipes();
        if (recipes != cachedRecipeSet) {
            recipeCache.clear();
            cachedRecipeSet = recipes;
        }
        for (CachedRecipe cached : recipeCache) {
            if (ItemStack.isSameItemSameComponents(cached.sample(), stack)) return cached.recipe();
        }
        SingleRecipeInput input = new SingleRecipeInput(stack.copyWithCount(1));
        recipeLookupCount++;
        Optional<RecipeHolder<KineticPressRecipe>> recipe =
                level.getRecipeManager().getRecipeFor(ModRecipeTypes.KINETIC_PRESSING.get(), input, level);
        RecipeHolder<KineticPressRecipe> result = recipe.orElse(null);
        if (recipeCache.size() >= 64) recipeCache.removeFirst();
        recipeCache.add(new CachedRecipe(stack.copyWithCount(1), result));
        return result;
    }

    private record CachedRecipe(ItemStack sample, RecipeHolder<KineticPressRecipe> recipe) {}

    public void invalidateRecipeCache() {
        recipeCache.clear();
        cachedRecipeSet = null;
    }

    public long getRecipeLookupCount() {
        return recipeLookupCount;
    }

    /** Recipe eligibility is independent of FE, height and currently free output space. */
    public boolean matchesInput(ItemStack stack) {
        Level level = blockEntity.getLevel();
        return level != null && !level.isClientSide && !stack.isEmpty()
                && findRecipe(level, stack) != null;
    }

    /** Returns only unstarted input; running tasks and completed products retain ownership. */
    public int returnQueuedInputs(Player player) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide || player.level() != level) {
            return 0;
        }

        List<ItemStack> returned = new ArrayList<>();
        int count = 0;
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack stack = inputHandler.removeOne(slot);
            if (!stack.isEmpty()) {
                returned.add(stack);
                count += stack.getCount();
            }
        }
        // Remove ownership from every queue slot before calling external inventory/drop APIs.
        inputIdleTicks = 0;
        batchLocked = !activeTasks.isEmpty() || !pendingOutputs.isEmpty();
        batchTotal = Math.max(batchCompleted + activeTasks.size(), batchTotal - count);
        markChangedAndSync();
        blockEntity.syncWorkingState();
        for (ItemStack stack : returned) {
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
        player.getInventory().setChanged();
        return count;
    }

    private void flushPendingOutputs() {
        if (pendingOutputs.isEmpty()) {
            return;
        }

        List<ItemStack> remainders = new ArrayList<>();
        for (ItemStack output : pendingOutputs) {
            ItemStack remainder = outputHandler.insertInternal(output);
            if (!remainder.isEmpty()) {
                remainders.add(remainder);
            }
        }
        pendingOutputs.clear();
        pendingOutputs.addAll(remainders);
        blockEntity.setChanged();
    }

    private void endBatch() {
        batchLocked = false;
        inputIdleTicks = 0;
        markChangedAndSync();
    }

    public void onInputInserted() {
        if (!batchLocked && inputIdleTicks == 0 && activeTasks.isEmpty() && pendingOutputs.isEmpty()) {
            batchCompleted = 0;
            batchTotal = inputHandler.countItems();
        } else {
            batchTotal = Math.max(batchTotal + 1,
                    batchCompleted + activeTasks.size() + inputHandler.countItems());
        }
        if (inputHandler.countItems() >= MAX_BATCH_ITEMS) {
            inputIdleTicks = 0;
            batchLocked = true;
        } else {
            inputIdleTicks = INPUT_IDLE_TICKS;
        }
        markChangedAndSync();
    }

    public boolean canAcceptInput() {
        Level level = blockEntity.getLevel();
        return level != null && !level.isClientSide
                && !blockEntity.isPowered()
                && blockEntity.isStructureValid()
                && !batchLocked
                && inputHandler.countItems() < MAX_BATCH_ITEMS;
    }

    public boolean isCollecting() {
        return !batchLocked && inputIdleTicks > 0 && inputHandler.countItems() > 0;
    }

    public boolean isWorking() {
        return isCollecting() || isProcessing();
    }

    public boolean isHeightChangeLocked() {
        return isProcessing();
    }

    public enum RunState {
        IDLE, COLLECTING, WAITING_ENERGY, WAITING_OUTPUT, RUNNING,
        INVALID_STRUCTURE, PAUSED, ENERGY_LIMIT, INVALID_INPUT
    }

    /** Priority: pause, structure, running, collecting, output, energy, invalid input, idle. */
    public RunState getRunState() {
        if (usesClientDiagnostics()) return clientRunState;
        if (blockEntity.isPowered()) return RunState.PAUSED;
        if (!blockEntity.isStructureValid()) return RunState.INVALID_STRUCTURE;
        if (isProcessing()) return RunState.RUNNING;
        if (isCollecting()) return RunState.COLLECTING;
        if (!pendingOutputs.isEmpty()) return RunState.WAITING_OUTPUT;
        Level level = blockEntity.getLevel();
        if (level == null) return RunState.IDLE;
        boolean valid = false, outputBlocked = false, energyBlocked = false, overCapacity = false;
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack stack = inputHandler.getInternal(slot);
            if (stack.isEmpty()) continue;
            RecipeHolder<KineticPressRecipe> holder = findRecipe(level, stack);
            if (holder == null) continue;
            valid = true;
            int cost = holder.value().calculateEnergyCost(blockEntity.getWorkingHeight(),
                    ModConfigs.KINETIC_PRESS_EXTRA_ENERGY_PER_HEIGHT.get());
            outputBlocked |= !canReserveOutputCapacity(holder.value());
            overCapacity |= cost > KineticPressBlockEntity.MAX_ENERGY;
            energyBlocked |= !blockEntity.hasEnergy(cost);
        }
        if (outputBlocked) return RunState.WAITING_OUTPUT;
        if (overCapacity) return RunState.ENERGY_LIMIT;
        if (energyBlocked) return RunState.WAITING_ENERGY;
        if (!valid && !inputHandler.isEmpty()) return RunState.INVALID_INPUT;
        return RunState.IDLE;
    }

    public int getInputCount() {
        return usesClientDiagnostics() ? clientInputCount : inputHandler.countItems();
    }

    private boolean usesClientDiagnostics() {
        Level level = blockEntity.getLevel();
        return hasClientDiagnostics && (level == null || level.isClientSide);
    }

    public int getBatchTotal() {
        return batchTotal;
    }

    public int getBatchCompleted() {
        return batchCompleted;
    }

    /** -1 means completion depends on waiting conditions and has no honest ETA. */
    public int getRemainingTicks() {
        if (usesClientDiagnostics()) return clientRemainingTicks;
        if (blockEntity.isPowered() || !blockEntity.isStructureValid()
                || !inputHandler.isEmpty() || !pendingOutputs.isEmpty()) return -1;
        int remaining = 0;
        for (ActiveTask task : activeTasks) remaining = Math.max(remaining, Math.max(0, task.totalTime - task.progress));
        return remaining;
    }

    public record EnergyDiagnostics(int requiredEnergy, int extraEnergy, int deficit, int overCapacityEnergy) {}

    /** For queued work, show the cheapest next candidate and separately flag any over-capacity recipe. */
    public EnergyDiagnostics getEnergyDiagnostics() {
        if (usesClientDiagnostics()) return clientEnergyDiagnostics;
        Level level = blockEntity.getLevel();
        if (level == null) return new EnergyDiagnostics(0, 0, 0, 0);
        int required = Integer.MAX_VALUE, extra = 0, overCapacity = 0;
        boolean found = false;
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack stack = inputHandler.getInternal(slot);
            if (stack.isEmpty()) continue;
            RecipeHolder<KineticPressRecipe> holder = findRecipe(level, stack);
            if (holder == null) continue;
            KineticPressRecipe recipe = holder.value();
            int cost = recipe.calculateEnergyCost(blockEntity.getWorkingHeight(),
                    ModConfigs.KINETIC_PRESS_EXTRA_ENERGY_PER_HEIGHT.get());
            if (!found || cost < required) {
                required = cost;
                extra = Math.max(0, cost - recipe.baseEnergy());
            }
            found = true;
            if (cost > KineticPressBlockEntity.MAX_ENERGY && (overCapacity == 0 || cost < overCapacity)) overCapacity = cost;
        }
        if (!found) {
            for (ActiveTask task : activeTasks) extra = Math.max(extra, Math.max(0, task.energyCost - task.baseEnergy));
            return new EnergyDiagnostics(0, extra, 0, 0);
        }
        return new EnergyDiagnostics(required, extra, Math.max(0, required - blockEntity.getEnergyStored()), overCapacity);
    }

    public int getActiveTaskCount() {
        if (usesClientDiagnostics()) return clientActiveTaskCount;
        return blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide
                ? clientAnimations.size()
                : activeTasks.size();
    }

    public int getProcessingProgress() {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            int progress = 0;
            for (KineticPressAnimationSnapshot animation : clientAnimations) {
                progress = Math.max(progress, animation.progress());
            }
            return progress;
        }
        int progress = 0;
        for (ActiveTask task : activeTasks) {
            progress = Math.max(progress, task.progress);
        }
        return progress;
    }

    public int getProcessingTotalTime() {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            int total = 0;
            for (KineticPressAnimationSnapshot animation : clientAnimations) {
                total = Math.max(total, animation.totalTime());
            }
            return total;
        }
        int total = 0;
        for (ActiveTask task : activeTasks) {
            total = Math.max(total, task.totalTime);
        }
        return total;
    }

    public int getWorkingHeightSnapshot() {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            return clientAnimations.isEmpty() ? 0 : clientAnimations.getFirst().workingHeight();
        }
        return activeTasks.isEmpty() ? 0 : activeTasks.getFirst().workingHeight;
    }

    public boolean isProcessing() {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            return !clientAnimations.isEmpty() || !clientTimeline.isEmpty();
        }
        return !activeTasks.isEmpty();
    }

    public ItemStack getCurrentItem() {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            return clientAnimations.isEmpty() ? ItemStack.EMPTY : clientAnimations.getFirst().stack();
        }
        return activeTasks.isEmpty() ? ItemStack.EMPTY : activeTasks.getFirst().inputItem;
    }

    public List<KineticPressAnimationSnapshot> getAnimationSnapshots() {
        return blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide
                ? clientAnimations
                : Collections.emptyList();
    }

    public List<KineticPressAnimationRenderState> getAnimationRenderStates() {
        if (blockEntity.getLevel() == null || !blockEntity.getLevel().isClientSide) {
            return Collections.emptyList();
        }
        return clientTimeline.renderStates();
    }

    public boolean hasActiveAnimationFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            for (KineticPressAnimationRenderState animation : clientTimeline.renderStates()) {
                if (ItemStack.isSameItemSameComponents(stack, animation.stack())) {
                    return true;
                }
            }
            return false;
        }
        for (ActiveTask task : activeTasks) {
            if (ItemStack.isSameItemSameComponents(stack, task.inputItem)) {
                return true;
            }
        }
        return false;
    }

    public int getAnimationMaxWorkingHeight() {
        if (blockEntity.getLevel() != null && blockEntity.getLevel().isClientSide) {
            int height = 0;
            for (KineticPressAnimationRenderState animation : clientTimeline.renderStates()) {
                height = Math.max(height, animation.workingHeight());
            }
            return height;
        }
        int height = 0;
        for (ActiveTask task : activeTasks) {
            height = Math.max(height, task.workingHeight);
        }
        return height;
    }

    public KineticPressInputHandler getInputHandler() {
        return inputHandler;
    }

    public KineticPressOutputHandler getOutputHandler() {
        return outputHandler;
    }

    /** Transfers ownership once: unfinished tasks return only their original input. */
    public List<ItemStack> drainForRemoval() {
        List<ItemStack> recovered = new ArrayList<>();
        ItemStack uncommittedDrop = outputManager.resolveForRemoval();
        if (!uncommittedDrop.isEmpty()) recovered.add(uncommittedDrop);
        for (int slot = 0; slot < inputHandler.getSlots(); slot++) {
            ItemStack input = inputHandler.removeOne(slot);
            if (!input.isEmpty()) recovered.add(input.copy());
        }
        for (ActiveTask task : activeTasks) {
            if (!task.inputItem.isEmpty()) recovered.add(task.inputItem.copy());
        }
        activeTasks.clear();
        for (ItemStack result : pendingOutputs) {
            if (!result.isEmpty()) recovered.add(result.copy());
        }
        pendingOutputs.clear();
        for (int slot = 0; slot < outputHandler.getSlots(); slot++) {
            ItemStack output = outputHandler.extractInternal(slot, Integer.MAX_VALUE);
            if (!output.isEmpty()) recovered.add(output);
        }
        batchLocked = false;
        inputIdleTicks = 0;
        batchTotal = 0;
        batchCompleted = 0;
        return recovered;
    }

    public int getInputIdleTicks() {
        return inputIdleTicks;
    }

    public boolean isBatchLocked() {
        return batchLocked;
    }

    public int getEnergySpent() {
        int spent = 0;
        for (ActiveTask task : activeTasks) {
            spent += task.energyCost;
        }
        return spent;
    }

    public int getRequiredHeightSnapshot() {
        return activeTasks.isEmpty() ? 0 : activeTasks.getFirst().requiredHeight;
    }

    public int getBaseEnergySnapshot() {
        return activeTasks.isEmpty() ? 0 : activeTasks.getFirst().baseEnergy;
    }

    public ResourceLocation getCurrentRecipeId() {
        return activeTasks.isEmpty() ? null : activeTasks.getFirst().recipeId;
    }

    private void markChangedAndSync() {
        blockEntity.setChanged();
        blockEntity.sendDataImmediately();
    }

    public void write(CompoundTag tag, HolderLookup.Provider provider, boolean clientPacket) {
        if (clientPacket) {
            EnergyDiagnostics energy = getEnergyDiagnostics();
            tag.putString("Status", getRunState().name());
            tag.putInt("InputCount", getInputCount());
            tag.putInt("ActiveTaskCount", activeTasks.size());
            tag.putInt("BatchTotal", batchTotal);
            tag.putInt("BatchCompleted", batchCompleted);
            tag.putInt("RemainingTicks", getRemainingTicks());
            tag.putInt("RequiredEnergy", energy.requiredEnergy());
            tag.putInt("ExtraEnergy", energy.extraEnergy());
            tag.putInt("EnergyDeficit", energy.deficit());
            tag.putInt("OverCapacityEnergy", energy.overCapacityEnergy());
            Level level = blockEntity.getLevel();
            tag.putLong("AnimationRevision", level == null ? 0L : level.getGameTime());
            tag.putBoolean("AnimationRunning", level != null && !blockEntity.isPowered()
                    && blockEntity.isStructureValid() && !activeTasks.isEmpty());
            ListTag animations = new ListTag();
            for (ActiveTask task : activeTasks) {
                CompoundTag taskTag = new CompoundTag();
                taskTag.putInt("Id", task.id);
                taskTag.put("Item", task.inputItem.saveOptional(provider));
                taskTag.putInt("Progress", task.progress);
                taskTag.putInt("Total", task.totalTime);
                taskTag.putInt("WorkingHeight", task.workingHeight);
                animations.add(taskTag);
            }
            tag.put("Animations", animations);
            return;
        }

        tag.put("InputItems", inputHandler.serializeNBT(provider));
        tag.put("OutputItems", outputHandler.serializeNBT(provider));
        tag.putInt("InputIdleTicks", inputIdleTicks);
        tag.putBoolean("BatchLocked", batchLocked);
        tag.putInt("NextTaskId", nextTaskId);
        tag.putInt("NextInputSlot", nextInputSlot);
        tag.putInt("BatchTotal", batchTotal);
        tag.putInt("BatchCompleted", batchCompleted);

        ListTag tasks = new ListTag();
        for (ActiveTask task : activeTasks) {
            tasks.add(task.save(provider));
        }
        tag.put(TASK_LIST_TAG, tasks);
        tag.put("PendingOutputs", saveStacks(pendingOutputs, provider));
        outputManager.write(tag, provider);
    }

    public void read(CompoundTag tag, HolderLookup.Provider provider, boolean clientPacket) {
        if (clientPacket) {
            long revision = tag.getLong("AnimationRevision");
            if (revision < clientAnimationRevision) return;
            clientAnimationRevision = revision;
            hasClientDiagnostics = tag.contains("Status");
            try {
                clientRunState = RunState.valueOf(tag.getString("Status"));
            } catch (IllegalArgumentException exception) {
                clientRunState = RunState.IDLE;
            }
            clientInputCount = Math.max(0, tag.getInt("InputCount"));
            clientActiveTaskCount = Math.max(0, tag.getInt("ActiveTaskCount"));
            batchCompleted = Math.max(0, tag.getInt("BatchCompleted"));
            batchTotal = Math.max(batchCompleted, tag.getInt("BatchTotal"));
            clientRemainingTicks = Math.max(-1, tag.getInt("RemainingTicks"));
            clientEnergyDiagnostics = new EnergyDiagnostics(Math.max(0, tag.getInt("RequiredEnergy")),
                    Math.max(0, tag.getInt("ExtraEnergy")), Math.max(0, tag.getInt("EnergyDeficit")),
                    Math.max(0, tag.getInt("OverCapacityEnergy")));
            List<KineticPressAnimationSnapshot> animations = new ArrayList<>();
            ListTag list = tag.getList("Animations", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag animationTag = list.getCompound(i);
                ItemStack item = ItemStack.parseOptional(provider, animationTag.getCompound("Item"));
                int total = Math.max(0, animationTag.getInt("Total"));
                if (!item.isEmpty() && total > 0) {
                    animations.add(new KineticPressAnimationSnapshot(
                            animationTag.getInt("Id"), item,
                            Math.max(0, animationTag.getInt("Progress")), total,
                            Math.max(0, animationTag.getInt("WorkingHeight"))));
                }
            }
            clientAnimations = List.copyOf(animations);
            clientTimeline.accept(clientAnimations, revision, tag.getBoolean("AnimationRunning"));
            return;
        }

        hasClientDiagnostics = false;
        inputHandler.deserializeNBT(provider, tag.getCompound("InputItems"));
        invalidateRecipeCache();
        outputHandler.deserializeNBT(provider, tag.getCompound("OutputItems"));
        inputIdleTicks = Math.max(0, tag.getInt("InputIdleTicks"));
        batchLocked = tag.getBoolean("BatchLocked");
        nextTaskId = Math.max(1, tag.getInt("NextTaskId"));
        nextInputSlot = Math.floorMod(tag.getInt("NextInputSlot"), inputHandler.getSlots());

        activeTasks.clear();
        ListTag tasks = tag.getList(TASK_LIST_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < tasks.size(); i++) {
            ActiveTask task = ActiveTask.load(tasks.getCompound(i), provider);
            if (task != null) {
                activeTasks.add(task);
                nextTaskId = Math.max(nextTaskId, task.id + 1);
            }
        }
        if (!tag.contains(TASK_LIST_TAG, Tag.TAG_LIST) && tag.getBoolean("Processing")) {
            ActiveTask legacy = ActiveTask.loadLegacy(tag, provider, nextTaskId);
            if (legacy != null) {
                activeTasks.add(legacy);
                nextTaskId = Math.max(nextTaskId + 1, legacy.id + 1);
                batchLocked = true;
            }
        }
        loadStacksInto(pendingOutputs, tag.getList("PendingOutputs", Tag.TAG_COMPOUND), provider);
        outputManager.read(tag, provider);
        // Older saves cannot reveal how many products have already left the machine.
        // Infer only the work still owned by the input queue and active task list.
        batchCompleted = Math.max(0, tag.getInt("BatchCompleted"));
        batchTotal = Math.max(batchCompleted + activeTasks.size() + inputHandler.countItems(),
                tag.contains("BatchTotal") ? tag.getInt("BatchTotal") : 0);
    }

    private static ListTag saveStacks(List<ItemStack> stacks, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                list.add(stack.saveOptional(provider));
            }
        }
        return list;
    }

    private static void loadStacksInto(List<ItemStack> target, ListTag list, HolderLookup.Provider provider) {
        target.clear();
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(provider, list.getCompound(i));
            if (!stack.isEmpty()) {
                target.add(stack);
            }
        }
    }

    private static final class ActiveTask {
        int id;
        ItemStack inputItem = ItemStack.EMPTY;
        ResourceLocation recipeId;
        int progress;
        int totalTime;
        int workingHeight;
        int energyCost;
        int requiredHeight;
        int baseEnergy;
        final List<ItemStack> rolledResults = new ArrayList<>();
        final List<ItemStack> maximumOutputs = new ArrayList<>();

        CompoundTag save(HolderLookup.Provider provider) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("Id", id);
            tag.put("Item", inputItem.saveOptional(provider));
            if (recipeId != null) {
                tag.putString("Recipe", recipeId.toString());
            }
            tag.putInt("Progress", progress);
            tag.putInt("Total", totalTime);
            tag.putInt("WorkingHeight", workingHeight);
            tag.putInt("EnergyCost", energyCost);
            tag.putInt("RequiredHeight", requiredHeight);
            tag.putInt("BaseEnergy", baseEnergy);
            tag.put("RolledResults", saveStacks(rolledResults, provider));
            tag.put("MaximumOutputs", saveStacks(maximumOutputs, provider));
            return tag;
        }

        static ActiveTask load(CompoundTag tag, HolderLookup.Provider provider) {
            ItemStack item = ItemStack.parseOptional(provider, tag.getCompound("Item"));
            int total = Math.max(0, tag.getInt("Total"));
            if (item.isEmpty() || total <= 0) {
                return null;
            }
            ActiveTask task = new ActiveTask();
            task.id = tag.getInt("Id");
            task.inputItem = item.copyWithCount(1);
            task.recipeId = tag.contains("Recipe")
                    ? ResourceLocation.tryParse(tag.getString("Recipe"))
                    : null;
            task.progress = Math.max(0, tag.getInt("Progress"));
            task.totalTime = total;
            task.workingHeight = Math.max(0, tag.getInt("WorkingHeight"));
            task.energyCost = Math.max(0, tag.getInt("EnergyCost"));
            task.requiredHeight = Math.max(0, tag.getInt("RequiredHeight"));
            task.baseEnergy = Math.max(0, tag.getInt("BaseEnergy"));
            loadStacksInto(task.rolledResults, tag.getList("RolledResults", Tag.TAG_COMPOUND), provider);
            loadStacksInto(task.maximumOutputs, tag.getList("MaximumOutputs", Tag.TAG_COMPOUND), provider);
            return task;
        }

        /**
         * Migrates the pre-parallel single-task save format. The old task already
         * paid its FE and locked its rolled results, so it resumes as one active
         * task without rerolling or charging again.
         */
        static ActiveTask loadLegacy(CompoundTag tag, HolderLookup.Provider provider, int fallbackId) {
            ItemStack item = ItemStack.parseOptional(provider, tag.getCompound("CurrentItem"));
            int total = Math.max(0, tag.getInt("ProcessingTotalTime"));
            if (item.isEmpty() || total <= 0) {
                return null;
            }

            ActiveTask task = new ActiveTask();
            task.id = Math.max(1, fallbackId);
            task.inputItem = item.copyWithCount(1);
            task.recipeId = tag.contains("CurrentRecipe")
                    ? ResourceLocation.tryParse(tag.getString("CurrentRecipe"))
                    : null;
            task.progress = Math.max(0, tag.getInt("ProcessingProgress"));
            task.totalTime = total;
            task.workingHeight = Math.max(0, tag.getInt("WorkingHeightSnapshot"));
            task.energyCost = Math.max(0, tag.getInt("EnergyCostSnapshot"));
            task.requiredHeight = Math.max(0, tag.getInt("RequiredHeightSnapshot"));
            task.baseEnergy = Math.max(0, tag.getInt("BaseEnergySnapshot"));
            loadStacksInto(task.rolledResults,
                    tag.getList("RolledResults", Tag.TAG_COMPOUND), provider);
            for (ItemStack result : task.rolledResults) {
                task.maximumOutputs.add(result.copy());
            }
            return task;
        }
    }
}
