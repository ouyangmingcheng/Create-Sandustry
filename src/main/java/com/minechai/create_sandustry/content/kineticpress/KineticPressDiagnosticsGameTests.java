package com.minechai.create_sandustry.content.kineticpress;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressDiagnosticsGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressDiagnosticsGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void sixteenTaskBatchCountsPersistAndNextBatchResetsHistory(GameTestHelper h) {
        withRecipe(h, press -> {
            press.receiveEnergy(1600, false);
            for (int slot = 0; slot < 16; slot++) press.getInputHandler().insertItem(slot, new ItemStack(Items.CARROT), false);
            h.assertTrue(press.getProcessor().getBatchTotal() == 16 && press.getProcessor().getBatchCompleted() == 0,
                    "Collected batch must count all sixteen inputs before starting");
            for (int tick = 0; tick < 3; tick++) press.getProcessor().tick();
            h.assertTrue(press.getProcessor().getBatchTotal() == 16 && press.getProcessor().getBatchCompleted() == 16,
                    "Each completed independent task must count exactly once");
            CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
            press.loadWithComponents(saved, h.getLevel().registryAccess());
            h.assertTrue(press.getProcessor().getBatchTotal() == 16 && press.getProcessor().getBatchCompleted() == 16,
                    "Completed batch history must survive save/reload");
            press.getInputHandler().insertItem(0, new ItemStack(Items.CARROT), false);
            h.assertTrue(press.getProcessor().getBatchTotal() == 1 && press.getProcessor().getBatchCompleted() == 0,
                    "A genuinely new batch must reset completed history");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void returningUnstartedInputsAdjustsTotalAndPreservesCompletedCount(GameTestHelper h) {
        withRecipe(h, press -> {
            press.receiveEnergy(400, false);
            for (int slot = 0; slot < 16; slot++) press.getInputHandler().insertItem(slot, new ItemStack(Items.CARROT), false);
            for (int tick = 0; tick < 3; tick++) press.getProcessor().tick();
            h.assertTrue(press.getProcessor().getBatchTotal() == 16 && press.getProcessor().getBatchCompleted() == 4
                            && press.getProcessor().getInputCount() == 12,
                    "Four affordable tasks must complete while twelve remain queued");
            h.assertTrue(press.returnQueuedInputs(h.makeMockPlayer(GameType.SURVIVAL)) == 12,
                    "Only twelve unstarted tasks may be returned");
            h.assertTrue(press.getProcessor().getBatchTotal() == 4 && press.getProcessor().getBatchCompleted() == 4,
                    "Return must subtract only cancelled unstarted work and preserve four completed tasks");
            h.assertTrue(!press.getProcessor().isBatchLocked(), "Returned waiting batch must release its lock");
        });
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void remainingTimeUsesMaximumIndividualRemainderAndWaitsForQueuedWork(GameTestHelper h) {
        var press = machine(h);
        CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
        ListTag tasks = new ListTag();
        tasks.add(task(h, 1, 20, 5));
        tasks.add(task(h, 2, 100, 99));
        saved.put("ActiveTasks", tasks);
        saved.putBoolean("BatchLocked", true);
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(press.getProcessor().getRemainingTicks() == 15,
                "ETA must be max(20-5,100-99)=15, not max(total)-max(progress)=1");
        saved.put("InputItems", inputItems(h, 1));
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(press.getProcessor().getRemainingTicks() == -1,
                "Queued work with unknown resource wait must suppress a fabricated completion ETA");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void clientDiagnosticsSnapshotCarriesAuthoritativeWaitCountsAndEnergy(GameTestHelper h) {
        var press = machine(h);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        for (int tick = 0; tick < 10; tick++) press.getProcessor().tick();
        var registries = h.getLevel().registryAccess();
        CompoundTag packet = press.writeClient(new CompoundTag(), registries);
        h.assertTrue(packet.getString("Status").equals("WAITING_ENERGY") && packet.getInt("InputCount") == 1
                        && packet.getInt("BatchTotal") == 1 && packet.getInt("BatchCompleted") == 0
                        && packet.getInt("RemainingTicks") == -1 && packet.getInt("EnergyDeficit") == 1200,
                "Server packet must describe the real waiting batch rather than client guesses");
        var clientCopy = new KineticPressBlockEntity(BlockPos.ZERO, ModBlocks.KINETIC_PRESS.get().defaultBlockState());
        clientCopy.readClient(packet, registries);
        h.assertTrue(clientCopy.getProcessor().getRunState().name().equals("WAITING_ENERGY")
                        && clientCopy.getProcessor().getInputCount() == 1
                        && clientCopy.getProcessor().getActiveTaskCount() == 0
                        && clientCopy.getProcessor().getBatchTotal() == 1
                        && clientCopy.getProcessor().getRemainingTicks() == -1,
                "Client display must read server diagnostic fields without querying recipes or real inventory");
        var energy = clientCopy.getProcessor().getEnergyDiagnostics();
        h.assertTrue(energy.requiredEnergy() == 1200 && energy.extraEnergy() == 1000 && energy.deficit() == 1200,
                "Client energy hint must preserve the 3-block recipe's 1000 FE surcharge and 1200 FE deficit");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void olderParallelAndSingleTaskSavesInferOnlyStillOwnedBatchWork(GameTestHelper h) {
        var press = machine(h);
        CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.remove("BatchTotal");
        saved.remove("BatchCompleted");
        saved.put("InputItems", inputItems(h, 2));
        ListTag tasks = new ListTag();
        tasks.add(task(h, 1, 40, 10));
        tasks.add(task(h, 2, 80, 20));
        saved.put("ActiveTasks", tasks);
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(press.getProcessor().getBatchTotal() == 4 && press.getProcessor().getBatchCompleted() == 0,
                "Old parallel save must infer two queued plus two active tasks without inventing completed history");
        saved.remove("ActiveTasks");
        saved.putBoolean("Processing", true);
        saved.putInt("ProcessingProgress", 10);
        saved.putInt("ProcessingTotalTime", 40);
        saved.put("CurrentItem", new ItemStack(ModItems.BURNT_RESIDUE.get()).saveOptional(h.getLevel().registryAccess()));
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.assertTrue(press.getProcessor().getBatchTotal() == 3 && press.getProcessor().getBatchCompleted() == 0
                        && press.getProcessor().getActiveTaskCount() == 1,
                "Legacy single-task migration must infer two queued plus one preserved active task");
        h.succeed();
    }

    private static CompoundTag inputItems(GameTestHelper h, int count) {
        ListTag slots = new ListTag();
        for (int slot = 0; slot < count; slot++) {
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", slot);
            entry.put("Item", new ItemStack(ModItems.BURNT_RESIDUE.get()).saveOptional(h.getLevel().registryAccess()));
            slots.add(entry);
        }
        CompoundTag items = new CompoundTag();
        items.put("Items", slots);
        return items;
    }

    private static CompoundTag task(GameTestHelper h, int id, int total, int progress) {
        CompoundTag task = new CompoundTag();
        task.putInt("Id", id);
        task.putInt("Total", total);
        task.putInt("Progress", progress);
        task.putInt("WorkingHeight", 3);
        task.put("Item", new ItemStack(ModItems.BURNT_RESIDUE.get()).saveOptional(h.getLevel().registryAccess()));
        return task;
    }

    private static void withRecipe(GameTestHelper h, Consumer<KineticPressBlockEntity> scenario) {
        var manager = h.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = new ArrayList<>(manager.getRecipes());
        try {
            manager.replaceRecipes(List.of(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "diagnostic_fixture"),
                    new KineticPressRecipe(Ingredient.of(Items.CARROT),
                            List.of(new KineticPressOutput(Items.GOLD_NUGGET, 1, 1)), 2, 3, 100))));
            scenario.accept(machine(h));
        } finally {
            manager.replaceRecipes(original);
        }
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
}
