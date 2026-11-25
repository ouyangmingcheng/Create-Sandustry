package com.minechai.create_sandustry.content.kineticpress;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressInputGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void rejectsInvalidInputAndSimulationHasNoSideEffects(GameTestHelper h) {
        var press = machine(h);
        var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, h.absolutePos(BASE), Direction.UP);
        h.assertTrue(handler != null, "Top input capability missing");
        ItemStack invalid = new ItemStack(Items.DIRT, 16);
        h.assertTrue(handler.insertItem(0, invalid, false).getCount() == 16, "Invalid input must remain upstream");
        ItemStack valid = new ItemStack(ModItems.BURNT_RESIDUE.get(), 4);
        h.assertTrue(handler.insertItem(0, valid, true).getCount() == 3, "Simulation must report one accepted item");
        h.assertTrue(press.getProcessor().getInputCount() == 0 && press.getProcessor().getInputIdleTicks() == 0
                && press.getProcessor().getActiveTaskCount() == 0 && press.getEnergyStored() == 0,
                "Simulation must not change machine state");
        h.assertTrue(handler.insertItem(0, valid, false).getCount() == 3, "Valid recipe input must be accepted without FE");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 45)
    public static void boundChuteKeepsRejectedStack(GameTestHelper h) {
        var press = machine(h);
        ChuteBlockEntity chute = h.getBlockEntity(BASE.above(3));
        chute.setItem(new ItemStack(Items.DIRT, 7));
        h.runAfterDelay(20, () -> {
            h.assertTrue(chute.getItem().is(Items.DIRT) && chute.getItem().getCount() == 7,
                    "Rejected input must remain in bound chute");
            h.assertTrue(press.getProcessor().getInputCount() == 0, "Chute must share the input filter");
            h.succeed();
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void returnsSixteenQueuedInputsOnceWithComponents(GameTestHelper h) {
        var press = machine(h);
        ItemStack original = new ItemStack(ModItems.BURNT_RESIDUE.get());
        original.set(DataComponents.CUSTOM_NAME, Component.literal("return sentinel"));
        for (int i = 0; i < 16; i++) press.getInputHandler().insertItem(i, original.copy(), false);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(press.returnQueuedInputs(player) == 16, "All unstarted inputs must return");
        h.assertTrue(press.returnQueuedInputs(player) == 0, "Repeated return must be idempotent");
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItemSameComponents(original, stack)) count += stack.getCount();
        }
        h.assertTrue(count == 16, "Return must preserve components and count");
        h.assertTrue(!press.getProcessor().isBatchLocked() && press.canAcceptInput(), "Empty batch must reopen input");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void returnsLegacyInvalidInputsAndKeepsPaidTasks(GameTestHelper h) {
        var press = machine(h);
        var registries = h.getLevel().registryAccess();
        CompoundTag saved = press.saveWithoutMetadata(registries);
        ListTag inputs = new ListTag();
        CompoundTag slot = new CompoundTag();
        slot.putInt("Slot", 0);
        slot.put("Item", new ItemStack(Items.DIRT, 1).saveOptional(registries));
        inputs.add(slot);
        CompoundTag input = new CompoundTag(); input.put("Items", inputs); saved.put("InputItems", input);
        ListTag tasks = new ListTag(); CompoundTag task = new CompoundTag();
        task.putInt("Id", 1); task.putInt("Total", 40); task.putInt("Progress", 10);
        task.putInt("WorkingHeight", 3);
        task.put("Item", new ItemStack(ModItems.BURNT_RESIDUE.get()).saveOptional(registries));
        tasks.add(task); saved.put("ActiveTasks", tasks); saved.putBoolean("BatchLocked", true);
        press.loadWithComponents(saved, registries);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(press.returnQueuedInputs(player) == 1, "Invalid legacy/reloaded input must remain returnable");
        h.assertTrue(press.getProcessor().getActiveTaskCount() == 1 && press.getProcessor().getProcessingProgress() == 10,
                "Return must not cancel paid active work");
        h.assertTrue(player.getInventory().countItem(Items.DIRT) == 1, "Invalid item was lost");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void fullPlayerInventoryDropsReturnedRemainder(GameTestHelper h) {
        var press = machine(h);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(h.absolutePos(BASE).getCenter());
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            player.getInventory().items.set(i, new ItemStack(Items.STONE, 64));
        }
        h.assertTrue(press.returnQueuedInputs(player) == 1, "Full inventory must still return input");
        int dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(h.absolutePos(BASE)).inflate(2), e -> e.getItem().is(ModItems.BURNT_RESIDUE.get()))
                .stream().mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(dropped == 1 && press.getProcessor().getInputCount() == 0, "Remainder must have exactly one owner");
        h.succeed();
    }

    private static KineticPressBlockEntity machine(GameTestHelper h) {
        h.setBlock(BASE.below(), Blocks.STONE); h.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = h.getBlockEntity(BASE);
        CompoundTag tag = press.saveWithoutMetadata(h.getLevel().registryAccess());
        tag.putInt("workingHeight", 3); press.loadWithComponents(tag, h.getLevel().registryAccess());
        h.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState()); press.requestStructureCheck();
        return press;
    }
}
