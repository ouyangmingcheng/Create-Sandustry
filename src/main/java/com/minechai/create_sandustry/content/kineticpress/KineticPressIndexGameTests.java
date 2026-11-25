package com.minechai.create_sandustry.content.kineticpress;

import java.util.EnumSet;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressIndexGameTests {
    private static final BlockPos BASE = new BlockPos(1, 0, 1);

    private KineticPressIndexGameTests() {
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void persistedRebindingUnloadAndRemovalInvalidateLookup(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        BlockPos oldInput = helper.absolutePos(BASE.above(3));
        BlockPos newInput = helper.absolutePos(BASE.above(4));
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), oldInput) == press,
                "Initial input must resolve through the loaded index");

        // Exercise the persisted binding update used on reload; real inventory
        // movement is covered separately by KineticPressBindingGameTests.
        helper.getLevel().setBlock(oldInput, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.setBlock(BASE.above(4), AllBlocks.CHUTE.getDefaultState());
        CompoundTag saved = press.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putInt("workingHeight", 4);
        saved.putLong("BoundInput", newInput.asLong());
        press.loadWithComponents(saved, helper.getLevel().registryAccess());
        press.requestStructureCheck();
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), oldInput) == null,
                "Rebinding must immediately remove the previous input key");
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), newInput) == press,
                "A read with an attached level must register the new input");

        press.onChunkUnloaded();
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), newInput) == null,
                "Chunk unload must remove the loaded input entry");
        press.onLoad();
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), newInput) == press,
                "onLoad must restore lookup before a chute can tick, without waiting for lazyTick");

        helper.setBlock(BASE, Blocks.AIR);
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), newInput) == null,
                "Destroying the base must remove its lookup entry immediately");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void inputIndexIsIsolatedByActualLevel(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        BlockPos input = helper.absolutePos(BASE.above(3));
        ServerLevel nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null && nether != helper.getLevel(), "Test needs the server's separate Nether level");
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(nether, input) == null,
                "The same coordinates in another level must not resolve an Overworld base");
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), input) == press,
                "Cross-level lookup must not remove or replace the real binding");
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void indexedNeighborNotificationImmediatelyChecksLowGap(GameTestHelper helper) {
        KineticPressBlockEntity press = machine(helper);
        BlockPos blocker = helper.absolutePos(BASE.above(1));
        helper.getLevel().setBlock(blocker, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
        notifyAt(helper, blocker);
        helper.assertTrue(!press.isStructureValid() && blocker.equals(press.getBlockingPosition()),
                "An indexed neighbor event must catch the first gap block immediately");

        helper.getLevel().setBlock(blocker, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        notifyAt(helper, blocker);
        helper.assertTrue(press.isStructureValid(), "Clearing the gap must resume validity without a lazyTick delay");
        helper.succeed();
    }

    private static void notifyAt(GameTestHelper helper, BlockPos pos) {
        KineticPressBlockEntity.onNeighborNotify(new BlockEvent.NeighborNotifyEvent(helper.getLevel(),
                pos, helper.getLevel().getBlockState(pos), EnumSet.allOf(Direction.class), false));
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID,
            timeoutTicks = 20, skyAccess = true)
    public static void sharedInputKeepsNearestOwnerAcrossRefreshAndRecovery(GameTestHelper helper) {
        helper.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity lower = helper.getBlockEntity(BASE);
        helper.setBlock(BASE.above(8), AllBlocks.CHUTE.getDefaultState());
        lower.requestStructureCheck();
        helper.assertTrue(lower.isStructureValid(), "The lower base must initially own the eight-block column");

        BlockPos upperBase = BASE.above(5);
        helper.setBlock(upperBase, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity upper = helper.getBlockEntity(upperBase);
        CompoundTag upperSaved = upper.saveWithoutMetadata(helper.getLevel().registryAccess());
        upperSaved.putInt("workingHeight", 3);
        upper.loadWithComponents(upperSaved, helper.getLevel().registryAccess());
        upper.requestStructureCheck();
        BlockPos input = helper.absolutePos(BASE.above(8));
        lower.requestStructureCheck();
        helper.assertTrue(!lower.isStructureValid() && upper.isStructureValid()
                        && lower.isBoundToInput(input) && upper.isBoundToInput(input),
                "Both bases must retain the binding while only the nearest base has a clear column");
        for (int refresh = 0; refresh < 3; refresh++) lower.requestStructureCheck();
        lower.loadWithComponents(lower.saveWithoutMetadata(helper.getLevel().registryAccess()),
                helper.getLevel().registryAccess());
        helper.assertTrue(KineticPressBlockEntity.findForBoundChute(helper.getLevel(), input) == upper,
                "Refreshing or reading the lower owner must never overwrite the nearest owner's entry");

        helper.setBlock(upperBase.above(), Blocks.STONE);
        upper.requestStructureCheck();
        helper.assertTrue(!upper.isStructureValid()
                        && KineticPressBlockEntity.findForBoundChute(helper.getLevel(), input) == upper,
                "A temporarily obstructed nearest owner must keep holding upstream items");
        helper.setBlock(upperBase.above(), Blocks.AIR);
        helper.setBlock(upperBase, Blocks.AIR);
        lower.requestStructureCheck();
        helper.assertTrue(lower.isStructureValid()
                        && KineticPressBlockEntity.findForBoundChute(helper.getLevel(), input) == lower,
                "Removing the nearest owner must expose the surviving lower binding and recover its structure");
        helper.succeed();
    }

    private static KineticPressBlockEntity machine(GameTestHelper helper) {
        helper.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(BASE);
        CompoundTag saved = press.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putInt("workingHeight", 3);
        press.loadWithComponents(saved, helper.getLevel().registryAccess());
        helper.setBlock(BASE.above(3), AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        return press;
    }
}
