package com.minechai.create_sandustry.content.compat.jade;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.block.machine.kinetic_press.KineticPressBlockEntity;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import snownee.jade.api.ITooltip;
import snownee.jade.impl.BlockAccessorImpl;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressJadeGameTests {
    private KineticPressJadeGameTests() {}

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void jadeUsesServerSnapshotOnEveryFaceAndUpdatesPauseState(GameTestHelper h) {
        BlockPos base = new BlockPos(1, 1, 1);
        h.setBlock(base.below(), Blocks.STONE);
        h.setBlock(base, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = h.getBlockEntity(base);
        CompoundTag saved = press.saveWithoutMetadata(h.getLevel().registryAccess());
        saved.putInt("workingHeight", 3);
        press.loadWithComponents(saved, h.getLevel().registryAccess());
        h.setBlock(base.above(3), AllBlocks.CHUTE.getDefaultState());
        press.requestStructureCheck();
        press.getInputHandler().insertItem(0, new ItemStack(ModItems.BURNT_RESIDUE.get()), false);
        for (int tick = 0; tick < 10; tick++) press.getProcessor().tick();

        CompoundTag serverData = new CompoundTag();
        BlockPos pos = h.absolutePos(base);
        var accessor = new BlockAccessorImpl.Builder().level(h.getLevel())
                .player(h.makeMockPlayer(GameType.SURVIVAL)).serverConnected(true)
                .blockState(press.getBlockState()).blockEntity(press).serverData(serverData)
                .hit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)).build();
        KineticPressJadeProvider.INSTANCE.appendServerData(serverData, accessor);
        CompoundTag status = serverData.getCompound(KineticPressJadeProvider.DATA_KEY);
        h.assertTrue(status.getInt("Energy") == 0 && status.getInt("Capacity") == 3000
                        && status.getInt("Height") == 3 && status.getString("State").equals("waiting_energy")
                        && status.getInt("BatchTotal") == 1 && status.getInt("BatchCompleted") == 0,
                "Jade must obtain authoritative wait, energy and batch data even when looking at the top face");
        h.assertTrue(!status.contains("InputItems") && !status.contains("ActiveTasks"),
                "Jade status must not expose full inventories or rolled task results");

        press.receiveEnergy(1200, false);
        List<Component> lines = new ArrayList<>();
        ITooltip tooltip = (ITooltip) Proxy.newProxyInstance(ITooltip.class.getClassLoader(),
                new Class<?>[] {ITooltip.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "add" -> { lines.add((Component) args[0]); yield null; }
                    case "remove" -> false;
                    default -> throw new AssertionError("Unexpected tooltip operation: " + method.getName());
                });
        // Jade copies serverData when building an accessor, as a client does on receipt.
        for (Direction side : Direction.values()) {
            var clientAccessor = new BlockAccessorImpl.Builder().from(accessor).serverData(serverData)
                    .hit(new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false)).build();
            lines.clear();
            KineticPressJadeProvider.INSTANCE.appendTooltip(tooltip, clientAccessor, null);
            h.assertTrue(lines.size() == 4 && hasLine(lines, "energy", 0, 3000)
                            && hasLine(lines, "height", 3) && hasLine(lines, "batch", 0, 1),
                    "Client tooltip must use the received snapshot on " + side
                            + ", without recomputing from a newer block entity");
        }

        h.setBlock(base.east(), Blocks.REDSTONE_BLOCK);
        KineticPressJadeProvider.INSTANCE.appendServerData(serverData, accessor);
        h.assertTrue(serverData.getCompound(KineticPressJadeProvider.DATA_KEY).getString("State").equals("paused")
                        && serverData.getCompound(KineticPressJadeProvider.DATA_KEY).getInt("Energy") == 1200,
                "A refreshed Jade snapshot must show redstone pause and charging without starting queued work");
        h.succeed();
    }

    private static boolean hasLine(List<Component> lines, String key, Object... values) {
        return lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                && contents.getKey().equals("gui.createsandustry.kinetic_press.goggles." + key)
                && Arrays.equals(contents.getArgs(), values));
    }
}
