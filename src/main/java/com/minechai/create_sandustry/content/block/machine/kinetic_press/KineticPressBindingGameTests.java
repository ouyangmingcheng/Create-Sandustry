package com.minechai.create_sandustry.content.block.machine.kinetic_press;

import java.util.UUID;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.logistics.chute.ChuteBlockEntity;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressBindingGameTests {
    private static final BlockPos BASE = new BlockPos(1, 0, 1);
    private static final int OLD_HEIGHT = 3;
    private static final int NEW_HEIGHT = 4;

    private KineticPressBindingGameTests() {
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void movingFullChutePreservesOneInventoryAndComponents(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, true);
        ServerPlayer player = makePlayer(helper);
        ItemStack payload = payload("moving-input");
        ChuteBlockEntity original = helper.getBlockEntity(BASE.above(OLD_HEIGHT));
        original.setItem(payload.copy());

        try {
            helper.assertTrue(press.trySetWorkingHeight(player, NEW_HEIGHT), "Moving the bound input up failed");
            assertMovedInventory(helper, press, NEW_HEIGHT, payload);
            helper.assertTrue(helper.getBlockState(BASE.above(OLD_HEIGHT)).isAir(),
                    "Original input position must be empty after moving");

            helper.assertTrue(press.trySetWorkingHeight(player, OLD_HEIGHT), "Moving the bound input back failed");
            assertMovedInventory(helper, press, OLD_HEIGHT, payload);
            helper.assertTrue(helper.getBlockState(BASE.above(NEW_HEIGHT)).isAir(),
                    "Temporary input position must be empty after moving back");
            helper.assertTrue(press.getInputHandler().countItems() == 0,
                    "Moving the chute must not also copy its contents into the input queue");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void occupiedMoveTargetKeepsOriginalChuteInventory(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, true);
        ServerPlayer player = makePlayer(helper);
        ItemStack payload = payload("blocked-input");
        ChuteBlockEntity original = helper.getBlockEntity(BASE.above(OLD_HEIGHT));
        original.setItem(payload.copy());
        helper.setBlock(BASE.above(NEW_HEIGHT), Blocks.STONE);

        try {
            helper.assertTrue(!press.trySetWorkingHeight(player, NEW_HEIGHT), "Occupied target must reject a move");
            helper.assertTrue(press.getWorkingHeight() == OLD_HEIGHT, "Failed move must retain the old height");
            assertMovedInventory(helper, press, OLD_HEIGHT, payload);
            helper.assertTrue(helper.getBlockState(BASE.above(NEW_HEIGHT)).is(Blocks.STONE),
                    "Failed move must not overwrite the occupied target");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "steam_whistle_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void playerQuickPlacementConsumesOneChuteAndBindsInput(GameTestHelper helper) {
        KineticPressBlockEntity press = setupMachine(helper, false);
        ServerPlayer player = makePlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(AllBlocks.CHUTE.get(), 2));
        BlockPos basePos = press.getBlockPos();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(basePos), Direction.UP, basePos, false);

        try {
            helper.assertTrue(KineticPressPlacementHandler.tryPlace(helper.getLevel(), press, player,
                    InteractionHand.MAIN_HAND, hit), "Player quick placement should create the input");
            helper.assertTrue(player.getMainHandItem().getCount() == 1,
                    "Survival quick placement must consume exactly one chute");
            helper.assertTrue(press.isStructureValid(), "Quick placement must complete the structure");
            helper.assertTrue(press.isBoundToInput(helper.absolutePos(BASE.above(OLD_HEIGHT))),
                    "Quick placement must bind the chute at the configured height");
            helper.assertTrue(helper.getBlockState(BASE.above(OLD_HEIGHT)).is(AllBlocks.CHUTE.get()),
                    "Quick placement created the input at the wrong location");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    private static void assertMovedInventory(GameTestHelper helper, KineticPressBlockEntity press,
                                            int height, ItemStack expected) {
        ChuteBlockEntity moved = helper.getBlockEntity(BASE.above(height));
        helper.assertTrue(ItemStack.matches(moved.getItem(), expected),
                "Chute move must preserve the entire stack and all data components");
        helper.assertTrue(press.getWorkingHeight() == height && press.isStructureValid(),
                "Moved input must produce a valid structure at the new height");
        helper.assertTrue(press.isBoundToInput(helper.absolutePos(BASE.above(height))),
                "The bound input must follow the moved chute");
        AABB bounds = new AABB(press.getBlockPos()).expandTowards(0.0D, NEW_HEIGHT + 1.0D, 0.0D)
                .inflate(1.0D);
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, bounds,
                entity -> ItemStack.isSameItemSameComponents(entity.getItem(), expected)).isEmpty(),
                "Moving or rejecting a move must never also drop the held chute item");
    }

    private static ItemStack payload(String name) {
        ItemStack stack = new ItemStack(ModItems.BURNT_RESIDUE.get(), 64);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static KineticPressBlockEntity setupMachine(GameTestHelper helper, boolean withInput) {
        helper.setBlock(BASE, ModBlocks.KINETIC_PRESS.get());
        KineticPressBlockEntity press = helper.getBlockEntity(BASE);
        CompoundTag saved = press.saveWithoutMetadata(helper.getLevel().registryAccess());
        saved.putInt("workingHeight", OLD_HEIGHT);
        press.loadWithComponents(saved, helper.getLevel().registryAccess());
        if (withInput) {
            helper.setBlock(BASE.above(OLD_HEIGHT), AllBlocks.CHUTE.getDefaultState());
        }
        press.requestStructureCheck();
        return press;
    }

    private static ServerPlayer makePlayer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        GameProfile profile = new GameProfile(UUID.randomUUID(), "press-input-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        BlockPos base = helper.absolutePos(BASE);
        player.moveTo(base.getX() + 2.5D, base.getY() + 1.0D, base.getZ() + 0.5D, 0.0F, 0.0F);
        return player;
    }
}
