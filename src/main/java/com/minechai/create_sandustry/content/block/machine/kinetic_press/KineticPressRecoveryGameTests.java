package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Item;
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
public final class KineticPressRecoveryGameTests {
    private static final BlockPos BASE = new BlockPos(1, 1, 1);

    private KineticPressRecoveryGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100)
    public static void waitingEnergyResumesAfterRefillAndChargesOnce(GameTestHelper h) {
        var press = machine(h);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        h.runAfterDelay(13, () -> {
            state(h, press, "WAITING_ENERGY");
            h.assertTrue(press.getProcessor().getInputCount() == 1 && press.getEnergyStored() == 0,
                    "Energy wait must preserve unpaid input");
            press.receiveEnergy(1200, false);
            h.runAfterDelay(2, () -> {
                state(h, press, "RUNNING");
                h.assertTrue(press.getEnergyStored() == 0 && press.getProcessor().getActiveTaskCount() == 1,
                        "Refilled energy must start exactly one paid task");
                h.runAfterDelay(45, () -> {
                    h.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                            "Recovered task must complete normally");
                    h.assertTrue(press.getEnergyStored() == 0, "Running task must not charge additional FE");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 120)
    public static void brokenStructureFreezesPaidTaskAcrossReloadAndResumes(GameTestHelper h) {
        var press = machine(h);
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        h.runAfterDelay(14, () -> {
            h.assertTrue(press.getProcessor().getActiveTaskCount() == 1, "Fixture task did not start");
            int progress = press.getProcessor().getProcessingProgress();
            int energy = press.getEnergyStored();
            CompoundTag before = press.saveWithoutMetadata(h.getLevel().registryAccess());
            ListTag results = before.getList("ActiveTasks", Tag.TAG_COMPOUND).getCompound(0)
                    .getList("RolledResults", Tag.TAG_COMPOUND).copy();
            h.setBlock(BASE.above(), Blocks.STONE);
            press.requestStructureCheck();
            state(h, press, "INVALID_STRUCTURE");
            h.runAfterDelay(12, () -> {
                h.assertTrue(press.getProcessor().getProcessingProgress() == progress,
                        "Invalid structure must freeze paid task progress");
                CompoundTag paused = press.saveWithoutMetadata(h.getLevel().registryAccess());
                h.assertTrue(results.equals(paused.getList("ActiveTasks", Tag.TAG_COMPOUND).getCompound(0)
                        .getList("RolledResults", Tag.TAG_COMPOUND)), "Pause must not reroll results");
                press.loadWithComponents(paused, h.getLevel().registryAccess());
                press.requestStructureCheck();
                state(h, press, "INVALID_STRUCTURE");
                h.assertTrue(press.getProcessor().getProcessingProgress() == progress
                        && press.getEnergyStored() == energy, "Paused reload must preserve progress and FE");
                h.setBlock(BASE.above(), Blocks.AIR);
                press.requestStructureCheck();
                h.runAfterDelay(2, () -> {
                    h.assertTrue(press.getProcessor().getProcessingProgress() > progress,
                            "Repaired structure must resume original task");
                    h.runAfterDelay(45, () -> {
                        h.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == productCount(h, results, ModItems.DORMANT_SEEDS.get())
                                        && outputCount(press, Items.GOLD_NUGGET) == productCount(h, results, Items.GOLD_NUGGET),
                                "Resumed task must settle only its originally rolled results");
                        h.assertTrue(press.getEnergyStored() == energy, "Repair/resume must not charge again");
                        h.succeed();
                    });
                });
            });
        });
    }

    // The five-block template is roofed by GameTest unless skyAccess is requested;
    // an eight-block input move needs a genuinely open vertical column.
    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100, skyAccess = true)
    public static void waitingBatchAllowsHeightChangeWithSafeFailureAndRepricesInput(GameTestHelper h) {
        var press = machine(h);
        ServerPlayer player = serverPlayer(h);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        h.runAfterDelay(13, () -> {
            state(h, press, "WAITING_ENERGY");
            h.assertTrue(press.getProcessor().isBatchLocked() && !press.getProcessor().isHeightChangeLocked(),
                    "Unstarted locked batch must permit height repair");
            for (int offset = 1; offset <= 8; offset++) {
                if (offset != 3) {
                    h.assertTrue(h.getBlockState(BASE.above(offset)).isAir(),
                            "Height-move fixture column is blocked at offset " + offset + ": "
                                    + h.getBlockState(BASE.above(offset)));
                }
            }
            h.setBlock(BASE.above(8), Blocks.STONE);
            h.assertTrue(!press.trySetWorkingHeight(player, 8), "Occupied destination must reject height move");
            h.assertTrue(press.getWorkingHeight() == 3 && press.isBoundToInput(h.absolutePos(BASE.above(3)))
                            && h.getBlockState(BASE.above(3)).is(AllBlocks.CHUTE.get()),
                    "Failed height move must preserve height, binding and input block");
            h.setBlock(BASE.above(8), Blocks.AIR);
            h.assertTrue(press.trySetWorkingHeight(player, 8), "Waiting batch should move bound chute to height eight");
            h.assertTrue(press.getWorkingHeight() == 8 && press.isBoundToInput(h.absolutePos(BASE.above(8)))
                            && h.getBlockState(BASE.above(3)).isAir()
                            && h.getBlockState(BASE.above(8)).is(AllBlocks.CHUTE.get()),
                    "Successful height move must update input block and binding");
            press.receiveEnergy(200, false);
            h.runAfterDelay(2, () -> {
                h.assertTrue(press.getProcessor().getActiveTaskCount() == 1 && press.getEnergyStored() == 0,
                        "Waiting input must use the new 200 FE cost rather than old 1200 FE cost");
                h.assertTrue(press.getProcessor().isHeightChangeLocked(), "Started tasks must lock height");
                h.runAfterDelay(45, () -> {
                    h.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1,
                            "Repriced task must finish normally");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 100)
    public static void fullOutputPreservesInputAndEnergyThenRecovers(GameTestHelper h) {
        var press = machine(h);
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            press.getOutputHandler().setStackInSlot(slot, new ItemStack(Items.STONE, 64));
        }
        press.receiveEnergy(3000, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        h.runAfterDelay(13, () -> {
            state(h, press, "WAITING_OUTPUT");
            h.assertTrue(press.getProcessor().getInputCount() == 1 && press.getEnergyStored() == 3000
                            && press.getProcessor().getActiveTaskCount() == 0,
                    "Output wait must not consume input or FE");
            press.getOutputHandler().extractItem(0, 64, false);
            press.getOutputHandler().extractItem(1, 64, false);
            h.runAfterDelay(2, () -> {
                h.assertTrue(press.getProcessor().getActiveTaskCount() == 1 && press.getEnergyStored() == 1800,
                        "Freeing output capacity must resume and charge exactly once");
                h.runAfterDelay(45, () -> {
                    h.assertTrue(outputCount(press, ModItems.DORMANT_SEEDS.get()) == 1
                            && press.getEnergyStored() == 1800, "Recovered task must settle once without extra FE");
                    h.succeed();
                });
            });
        });
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void redstoneLocksExternalExtractionAndUnlocksWithoutLoss(GameTestHelper h) {
        var press = machine(h);
        press.getOutputHandler().insertInternal(new ItemStack(Items.GOLD_NUGGET, 7));
        h.setBlock(BASE.east(), Blocks.REDSTONE_BLOCK);
        state(h, press, "PAUSED");
        h.assertTrue(press.getOutputHandler().extractItem(0, 7, true).isEmpty()
                        && press.getOutputHandler().extractItem(0, 7, false).isEmpty()
                        && press.removeItem(0, 7).isEmpty(),
                "Redstone pause must block simulated, actual and Container extraction");
        h.assertTrue(outputCount(press, Items.GOLD_NUGGET) == 7, "Paused extraction must retain output");
        CompoundTag paused = press.saveWithoutMetadata(h.getLevel().registryAccess());
        press.loadWithComponents(paused, h.getLevel().registryAccess());
        h.assertTrue(press.getOutputHandler().extractItem(0, 7, false).isEmpty(),
                "Reloading paused machine must not bypass redstone extraction gate");
        h.setBlock(BASE.east(), Blocks.AIR);
        h.assertTrue(press.getOutputHandler().extractItem(0, 7, false).getCount() == 7,
                "Removing redstone must restore extraction without losing output");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void energyAboveCapacityNeverConsumesInputOrCharges(GameTestHelper h) {
        var press = machine(h);
        var manager = h.getLevel().getRecipeManager();
        List<RecipeHolder<?>> original = new ArrayList<>(manager.getRecipes());
        try {
            var recipe = new KineticPressRecipe(Ingredient.of(Items.CARROT),
                    List.of(new KineticPressOutput(Items.GOLD_NUGGET, 1, 1.0F)), 40, 23, 200);
            manager.replaceRecipes(List.of(new RecipeHolder<>(
                    ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "energy_limit_fixture"), recipe)));
            press.receiveEnergy(3000, false);
            for (int slot = 0; slot < 16; slot++) {
                h.assertTrue(press.getInputHandler().insertItem(slot, new ItemStack(Items.CARROT), false).isEmpty(),
                        "Valid recipe input must remain eligible even when current height costs too much");
            }
            press.getProcessor().tick();
            state(h, press, "ENERGY_LIMIT");
            h.assertTrue(press.getProcessor().getInputCount() == 16 && press.getProcessor().getActiveTaskCount() == 0
                            && press.getEnergyStored() == 3000,
                    "4200 FE task must not consume input or partially charge a 3000 FE battery");
            h.assertTrue(!press.getProcessor().isHeightChangeLocked(),
                    "Energy-limit wait must allow raising height to make the recipe executable");
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            h.assertTrue(press.returnQueuedInputs(player) == 16
                            && player.getInventory().countItem(Items.CARROT) == 16,
                    "Over-capacity input must remain returnable");
        } finally {
            manager.replaceRecipes(original);
        }
        h.succeed();
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
        h.assertTrue(press.isStructureValid(), "Recovery fixture must have a valid structure");
        return press;
    }

    private static ServerPlayer serverPlayer(GameTestHelper h) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "press-recovery-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        player.setPos(h.absolutePos(BASE).getCenter());
        return player;
    }

    private static void state(GameTestHelper h, KineticPressBlockEntity press, String expected) {
        h.assertTrue(press.getProcessor().getRunState().name().equals(expected),
                "Expected state " + expected + " but was " + press.getProcessor().getRunState());
    }

    private static int productCount(GameTestHelper h, ListTag products, Item item) {
        int count = 0;
        for (int i = 0; i < products.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(h.getLevel().registryAccess(), products.getCompound(i));
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    private static int outputCount(KineticPressBlockEntity press, Item item) {
        int count = 0;
        for (int slot = 0; slot < press.getOutputHandler().getSlots(); slot++) {
            ItemStack stack = press.getOutputHandler().getStackInSlot(slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
