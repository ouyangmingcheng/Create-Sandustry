package com.minechai.create_sandustry.content.kineticpress;

import java.util.ArrayList;
import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Exercises common animation data without loading client-only rendering classes on the server. */
@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressSyncGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressSyncGameTests() {}

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void sixteenAnimationTasksPredictIndependently(GameTestHelper h) {
        var timeline = new KineticPressAnimationTimeline();
        List<KineticPressAnimationSnapshot> snapshots = new ArrayList<>();
        for (int id = 1; id <= 16; id++) snapshots.add(snapshot(id, id, 60));
        timeline.accept(snapshots, 1, true);
        for (int tick = 0; tick < 3; tick++) timeline.tick();
        h.assertTrue(timeline.renderStates().size() == 16, "Every parallel task must have an independent render state");
        for (var state : timeline.renderStates()) {
            h.assertTrue(state.currentProgress() == state.id() + 3
                            && state.previousProgress() <= state.currentProgress(),
                    "Prediction must preserve each task's own server progress: " + state.id());
            h.assertTrue(state.progressAt(0.5F) >= state.previousProgress()
                            && state.progressAt(0.5F) <= state.currentProgress(),
                    "Partial tick interpolation must remain within task endpoints");
        }
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void predictionBudgetIsBoundedAndCannotCompleteUnconfirmedTask(GameTestHelper h) {
        var timeline = new KineticPressAnimationTimeline();
        timeline.accept(List.of(snapshot(1, 0, 100)), 1, true);
        for (int tick = 0; tick < 20; tick++) timeline.tick();
        h.assertTrue(current(timeline) == 4, "No packet may permit more than four predicted ticks");
        timeline.accept(List.of(snapshot(1, 0, 100)), 1, true);
        for (int tick = 0; tick < 10; tick++) timeline.tick();
        h.assertTrue(current(timeline) == 4, "Repeated snapshots must not reset the prediction budget");
        timeline.accept(List.of(snapshot(1, 4, 100)), 2, true);
        for (int tick = 0; tick < 4; tick++) timeline.tick();
        h.assertTrue(current(timeline) == 8, "A newer confirmed progress must permit the next bounded interval");
        timeline.accept(List.of(snapshot(1, 99, 100)), 3, true);
        for (int tick = 0; tick < 20; tick++) timeline.tick();
        h.assertTrue(current(timeline) == 99 && !timeline.isEmpty(),
                "Client prediction must not finish or remove an unconfirmed task");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void pausedAnimationFreezesAndResumeNeverMovesBackwards(GameTestHelper h) {
        var timeline = new KineticPressAnimationTimeline();
        timeline.accept(List.of(snapshot(1, 10, 40)), 1, true);
        for (int tick = 0; tick < 4; tick++) timeline.tick();
        double predicted = current(timeline);
        timeline.accept(List.of(snapshot(1, 12, 40)), 2, false);
        for (int tick = 0; tick < 20; tick++) timeline.tick();
        h.assertTrue(current(timeline) == predicted,
                "Pause must hold the visible endpoint even when server correction is behind prediction");
        var paused = timeline.renderStates().getFirst();
        h.assertTrue(paused.previousProgress() == paused.currentProgress(), "Paused interpolation must freeze completely");
        timeline.accept(List.of(snapshot(1, 14, 40)), 3, true);
        timeline.tick();
        var resumed = timeline.renderStates().getFirst();
        h.assertTrue(resumed.previousProgress() >= predicted && resumed.currentProgress() > predicted,
                "Resume must advance from its held endpoint without snapping backwards");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void staleRevisionCannotRewindOrResurrectAndClearDropsHistory(GameTestHelper h) {
        var timeline = new KineticPressAnimationTimeline();
        timeline.accept(List.of(snapshot(1, 10, 40)), 5, true);
        timeline.tick();
        double progress = current(timeline);
        timeline.accept(List.of(snapshot(1, 1, 40)), 4, false);
        h.assertTrue(current(timeline) >= progress, "Old revisions must not rewind current animation");
        timeline.accept(List.of(), 6, false);
        timeline.tick();
        timeline.tick();
        h.assertTrue(timeline.isEmpty(), "Completed animation must clear its render state");
        timeline.accept(List.of(snapshot(1, 2, 40)), 5, true);
        h.assertTrue(timeline.isEmpty(), "Stale snapshot must not resurrect a completed animation");
        timeline.accept(List.of(snapshot(2, 3, 40)), 7, true);
        timeline.clear();
        h.assertTrue(timeline.isEmpty() && timeline.renderStates().isEmpty(),
                "Lifecycle clear must leave no old render objects or ghost items");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void missingCompletedTaskGetsOneTerminalFrameThenDisappears(GameTestHelper h) {
        var timeline = new KineticPressAnimationTimeline();
        timeline.accept(List.of(snapshot(1, 36, 40)), 1, true);
        timeline.tick();
        double before = current(timeline);
        timeline.accept(List.of(), 2, true);
        timeline.tick();
        h.assertTrue(timeline.renderStates().size() == 1, "Completion must allow one final interpolation interval");
        var finishing = timeline.renderStates().getFirst();
        h.assertTrue(finishing.previousProgress() >= before && finishing.currentProgress() == 40,
                "Server-confirmed completion must reach the exact terminal endpoint");
        timeline.tick();
        h.assertTrue(timeline.isEmpty() && timeline.renderStates().isEmpty(),
                "Terminal interpolation must be removed next tick instead of leaving a trail");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void clientPacketsEstablishAndClearBoundInputPosition(GameTestHelper h) {
        var press = machine(h);
        var registries = h.getLevel().registryAccess();
        CompoundTag bound = press.writeClient(new CompoundTag(), registries);
        h.assertTrue(bound.contains("BoundInput") && bound.getLong("BoundInput") == h.absolutePos(BASE.above(3)).asLong(),
                "Client update must include the authoritative bound input position");
        var clientCopy = new KineticPressBlockEntity(BlockPos.ZERO, ModBlocks.KINETIC_PRESS.get().defaultBlockState());
        clientCopy.readClient(bound, registries);
        h.assertTrue(h.absolutePos(BASE.above(3)).equals(clientCopy.getBoundInputPos()),
                "Client read must establish binding without requiring a server-only scan");
        h.setBlock(BASE.above(3), Blocks.AIR);
        press.requestStructureCheck();
        CompoundTag cleared = press.writeClient(new CompoundTag(), registries);
        clientCopy.readClient(cleared, registries);
        h.assertTrue(clientCopy.getBoundInputPos() == null,
                "An unbound update must clear old client binding instead of retaining stale coordinates");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100)
    public static void processingUsesFewerSyncRefreshRequestsThanPerTickSnapshots(GameTestHelper h) {
        var press = machine(h);
        int before = press.getClientSyncCount();
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        h.runAfterDelay(40, () -> {
            int refreshRequests = press.getClientSyncCount() - before;
            h.assertTrue(!press.getProcessor().isProcessing() && outputCount(press) == 1,
                    "Synchronization measurement must include a completed twenty-tick task");
            h.assertTrue(refreshRequests >= 2 && refreshRequests <= 12,
                    "Expected at most 12 coalesced refresh requests instead of 20+ per-tick updates, got " + refreshRequests);
            CreateSandustry.LOGGER.info("KINETIC_PRESS_SYNC,task_ticks=20,observation_ticks=40,refresh_requests={}", refreshRequests);
            h.succeed();
        });
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void stalePacketCannotOverwriteBaseFieldsOrBinding(GameTestHelper h) {
        var clientCopy = new KineticPressBlockEntity(BlockPos.ZERO, ModBlocks.KINETIC_PRESS.get().defaultBlockState());
        var registries = h.getLevel().registryAccess();
        var current = new CompoundTag();
        current.putLong("AnimationRevision", 100);
        current.putInt("workingHeight", 8);
        current.putInt("energy", 2000);
        current.putBoolean("structureValid", true);
        current.putLong("BoundInput", BASE.above(8).asLong());
        current.putString("Status", "WAITING_ENERGY");
        clientCopy.readClient(current, registries);

        var stale = current.copy();
        stale.putLong("AnimationRevision", 99);
        stale.putInt("workingHeight", 3);
        stale.putInt("energy", 0);
        stale.putBoolean("structureValid", false);
        stale.remove("BoundInput");
        stale.putLong("BlockingPosition", BASE.above().asLong());
        stale.putString("Status", "INVALID_STRUCTURE");
        clientCopy.readClient(stale, registries);
        h.assertTrue(clientCopy.getWorkingHeight() == 8 && clientCopy.getEnergyStored() == 2000
                        && clientCopy.isStructureValid() && BASE.above(8).equals(clientCopy.getBoundInputPos())
                        && clientCopy.getBlockingPosition() == null
                        && clientCopy.getProcessor().getRunState() == KineticPressProcessor.RunState.WAITING_ENERGY,
                "An older revision must be rejected before changing any base or processor fields");

        current.remove("BoundInput");
        clientCopy.readClient(current, registries);
        h.assertTrue(clientCopy.getBoundInputPos() == null,
                "Same-tick authoritative changes must still be accepted at the same revision");
        h.succeed();
    }

    private static KineticPressAnimationSnapshot snapshot(int id, int progress, int total) {
        return new KineticPressAnimationSnapshot(id, new ItemStack(Items.CARROT), progress, total, 3 + id);
    }

    private static double current(KineticPressAnimationTimeline timeline) {
        return timeline.renderStates().getFirst().currentProgress();
    }

    private static KineticPressBlockEntity machine(GameTestHelper h) {
        h.setBlock(BASE.below(), Blocks.STONE);
        h.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = h.getBlockEntity(BASE);
        CompoundTag tag = press.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putInt("workingHeight", 3);
        press.loadWithComponents(tag, h.getLevel().registryAccess());
        h.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        return press;
    }

    private static int outputCount(KineticPressBlockEntity press) {
        int count = 0;
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            var stack = press.getOutputHandler().getStackInSlot(slot);
            if (stack.is(ModItems.AMETHYST_FLOWER_SEEDS.get())) count += stack.getCount();
        }
        return count;
    }
}
