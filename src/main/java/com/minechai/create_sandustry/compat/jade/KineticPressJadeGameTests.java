package com.minechai.create_sandustry.compat.jade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.kineticpress.KineticPressBlockEntity;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import snownee.jade.addon.universal.EnergyStorageProvider;
import snownee.jade.addon.universal.ItemStorageProvider;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.JadeIds;
import snownee.jade.api.view.ViewGroup;
import snownee.jade.impl.BlockAccessorImpl;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressJadeGameTests {
    private KineticPressJadeGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void jadeUsesNativeStoragePacketsOnEveryFaceWithoutMachineDiagnostics(GameTestHelper h) {
        KineticPressBlockEntity press = machine(h);
        for (int slot = 0; slot < 3; slot++) {
            press.getInputHandler().insertItem(slot, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        }
        press.getOutputHandler().setStackInSlot(0, new ItemStack(Items.GOLD_NUGGET, 5));
        for (int tick = 0; tick < 10; tick++) press.getProcessor().tick();

        List<BlockAccessor> snapshots = new ArrayList<>();
        for (Direction side : Direction.values()) {
            BlockAccessor accessor = nativeSnapshot(h, press, side);
            snapshots.add(accessor);
            assertOnlyStorage(h, accessor);
            CompoundTag energy = energy(accessor);
            h.assertTrue(energy.getLong("Cur") == 0 && energy.getLong("Capacity") == 3000,
                    "Native Jade energy bar data must be available from " + side);
            List<ItemStack> items = items(h, accessor);
            h.assertTrue(items.size() == 2 && count(items, new ItemStack(ModItems.BURNT_RESIDUE.get())) == 3
                            && count(items, new ItemStack(Items.GOLD_NUGGET)) == 5,
                    "Every face must show queued inputs and output buffers once, with merged counts");
        }

        // Change the real machine after the packet is captured.
        press.receiveEnergy(1200, false);
        press.getInputHandler().removeOne(0);
        press.getOutputHandler().setStackInSlot(0, new ItemStack(Items.GOLD_NUGGET, 9));
        for (BlockAccessor snapshot : snapshots) {
            h.assertTrue(energy(snapshot).getLong("Cur") == 0
                            && count(items(h, snapshot), new ItemStack(ModItems.BURNT_RESIDUE.get())) == 3
                            && count(items(h, snapshot), new ItemStack(Items.GOLD_NUGGET)) == 5,
                    "Native client storage views must use the received snapshot, not newer block entity data");
        }

        h.setBlock(new BlockPos(2, 1, 1), Blocks.REDSTONE_BLOCK);
        BlockAccessor paused = nativeSnapshot(h, press, Direction.UP);
        assertOnlyStorage(h, paused);
        h.assertTrue(energy(paused).getLong("Cur") == 1200
                        && count(items(h, paused), new ItemStack(ModItems.BURNT_RESIDUE.get())) == 2,
                "Paused machines must still show authoritative charging and buffered items without a status line");
        h.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void jadeOmitsEmptyAndActiveTaskItemsAndPreservesLargeCountsAndComponents(GameTestHelper h) {
        KineticPressBlockEntity press = machine(h);
        BlockAccessor empty = nativeSnapshot(h, press, Direction.DOWN);
        assertOnlyStorage(h, empty);
        h.assertTrue(items(h, empty).isEmpty() && energy(empty).getLong("Capacity") == 3000,
                "An empty machine must have an empty native item display and a zero energy bar");

        press.receiveEnergy(1200, false);
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        for (int tick = 0; tick < 10; tick++) press.getProcessor().tick();
        h.assertTrue(press.getProcessor().getActiveTaskCount() == 1,
                "The fixture must own a running task with already determined products");
        BlockAccessor running = nativeSnapshot(h, press, Direction.NORTH);
        h.assertTrue(items(h, running).isEmpty() && energy(running).getLong("Cur") == 0,
                "Running ingredients and future rolled products must not appear as buffered inventory");

        press.getOutputHandler().setStackInSlot(0, new ItemStack(Items.GOLD_NUGGET, 64));
        press.getOutputHandler().setStackInSlot(1, new ItemStack(Items.GOLD_NUGGET, 64));
        ItemStack named = new ItemStack(Items.GOLD_NUGGET, 2);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Jade component test"));
        press.getOutputHandler().setStackInSlot(2, named);
        BlockAccessor buffered = nativeSnapshot(h, press, Direction.SOUTH);
        List<ItemStack> displayed = items(h, buffered);
        h.assertTrue(displayed.size() == 2 && count(displayed, new ItemStack(Items.GOLD_NUGGET)) == 128
                        && count(displayed, named) == 2,
                "Native item packets must preserve counts above one stack and keep component variants separate");
        displayed.getFirst().setCount(1);
        h.assertTrue(press.getOutputHandler().getStackInSlot(0).getCount() == 64
                        && press.getOutputHandler().getStackInSlot(1).getCount() == 64,
                "Jade aggregation and snapshot mutation must never alter actual buffer ownership");
        h.succeed();
    }

    private static KineticPressBlockEntity machine(GameTestHelper h) {
        BlockPos base = new BlockPos(1, 1, 1);
        h.setBlock(base.below(), Blocks.STONE);
        h.setBlock(base, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = h.getBlockEntity(base);
        CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putInt("workingHeight", 3);
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.setBlock(base.above(3), AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        return press;
    }

    private static BlockAccessor nativeSnapshot(GameTestHelper h, KineticPressBlockEntity press, Direction side) {
        BlockPos pos = press.getBlockPos();
        BlockAccessor accessor = new BlockAccessorImpl.Builder().level(h.getLevel())
                .player(h.makeMockPlayer(GameType.SURVIVAL)).serverConnected(true)
                .blockState(press.getBlockState()).blockEntity(press)
                .hit(new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false)).build();
        // Client request gates read GUI-only preferences, unavailable on a
        // dedicated server. Test extension eligibility and the real server packet
        // path; the packet UIDs below also verify native provider selection.
        h.assertTrue(KineticPressJadeProvider.Energy.INSTANCE.shouldRequestData(accessor)
                        && KineticPressJadeProvider.Items.INSTANCE.shouldRequestData(accessor),
                "Storage extensions must serve both buffers on " + side);
        EnergyStorageProvider.getBlock().appendServerData(accessor.getServerData(), accessor);
        ItemStorageProvider.getBlock().appendServerData(accessor.getServerData(), accessor);
        return accessor;
    }

    private static void assertOnlyStorage(GameTestHelper h, BlockAccessor accessor) {
        CompoundTag data = accessor.getServerData();
        h.assertTrue(data.getAllKeys().equals(Set.of("JadeEnergyStorage", "JadeEnergyStorageUid",
                        JadeIds.UNIVERSAL_ITEM_STORAGE.toString())),
                "Jade must receive only native energy and item storage, without height, state or batch data");
        h.assertTrue(data.getString("JadeEnergyStorageUid").equals(KineticPressJadeProvider.Energy.INSTANCE.getUid().toString()),
                "Native energy data must use the registered press extension instead of a duplicate generic provider");
    }

    private static CompoundTag energy(BlockAccessor accessor) {
        return ViewGroup.readList(accessor.getServerData(), "JadeEnergyStorage", Function.identity())
                .getFirst().views.getFirst();
    }

    private static List<ItemStack> items(GameTestHelper h, BlockAccessor accessor) {
        var packet = accessor.decodeFromNbt(ViewGroup.listCodec(ItemStack.OPTIONAL_STREAM_CODEC),
                accessor.getServerData().get(JadeIds.UNIVERSAL_ITEM_STORAGE.toString())).orElseThrow();
        h.assertTrue(packet.getKey().equals(KineticPressJadeProvider.Items.INSTANCE.getUid()),
                "Native item data must use the combined buffer extension, got " + packet.getKey());
        return packet.getValue().stream().flatMap(group -> group.views.stream()).toList();
    }

    private static int count(List<ItemStack> items, ItemStack sample) {
        return items.stream().filter(item -> ItemStack.isSameItemSameComponents(item, sample))
                .mapToInt(ItemStack::getCount).sum();
    }
}
