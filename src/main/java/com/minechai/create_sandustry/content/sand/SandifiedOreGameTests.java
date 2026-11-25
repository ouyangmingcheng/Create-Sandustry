package com.minechai.create_sandustry.content.sand;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import io.netty.channel.embedded.EmbeddedChannel;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModBlocks;

import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime checks for tool gating, actual server-side drops and ore experience.
 */
@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class SandifiedOreGameTests {
    private static final BlockPos TEST_POS = BlockPos.ZERO;

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void sandifiedOresRuntime(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos absolutePos = helper.absolutePos(TEST_POS);
        ServerPlayer player = makeUnlistedServerPlayer(level);
        player.moveTo(absolutePos.getX() + 0.5, absolutePos.getY() + 1.0, absolutePos.getZ() + 0.5, 0.0F, 0.0F);

        try {
            for (SandifiedOreDefinitions.Entry entry : SandifiedOreDefinitions.ENTRIES) {
                verifyEntry(helper, level, player, absolutePos, entry);
            }
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    private static ServerPlayer makeUnlistedServerPlayer(ServerLevel level) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "sandified-ore-test");
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(profile, false);
        ServerPlayer player = new ServerPlayer(
                level.getServer(), level, profile, cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        new ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void verifyEntry(
            GameTestHelper helper,
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            SandifiedOreDefinitions.Entry entry) {
        Block target = ModBlocks.SANDIFIED_ORES.get(entry.id()).get();
        Block base = entry.variant().usesDeepslateSource()
                ? ModBlocks.PETRIFIED_SAND.get()
                : ModBlocks.HARDENED_SAND.get();
        BlockState state = target.defaultBlockState();
        BlockState baseState = base.defaultBlockState();
        String id = entry.targetBlock();

        require(helper, state.is(BlockTags.MINEABLE_WITH_SHOVEL), id + " is not mineable/shovel");
        require(helper, !state.is(BlockTags.MINEABLE_WITH_PICKAXE), id + " is still mineable/pickaxe");
        require(helper, state.requiresCorrectToolForDrops(), id + " does not require the correct tool");
        require(helper, state.getDestroySpeed(level, pos) == baseState.getDestroySpeed(level, pos),
                id + " hardness differs from its base");
        require(helper,
                state.getExplosionResistance(level, pos, null)
                        == baseState.getExplosionResistance(level, pos, null),
                id + " explosion resistance differs from its base");
        require(helper, state.getSoundType() == baseState.getSoundType(),
                id + " sound differs from its base");
        require(helper, state.getMapColor(level, pos) == baseState.getMapColor(level, pos),
                id + " map color differs from its base");
        require(helper, state.instrument() == baseState.instrument(),
                id + " note block instrument differs from its base");
        if (entry.ore().redstoneBehavior()) {
            require(helper, state.getLightEmission() == 0, id + " should start unlit");
            require(helper, state.setValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT, true)
                            .getLightEmission() == 9,
                    id + " lit state does not emit light level 9");
        }
        verifyTierTag(helper, state, entry.ore().toolTier(), id);

        Item correctShovel = shovelFor(entry.ore().toolTier());
        Item underTierShovel = underTierShovelFor(entry.ore().toolTier());
        ItemStack correctTool = new ItemStack(correctShovel);
        ItemStack wrongTypeTool = new ItemStack(Items.IRON_PICKAXE);
        ItemStack underTierTool = underTierShovel == null ? ItemStack.EMPTY : new ItemStack(underTierShovel);

        player.setItemInHand(InteractionHand.MAIN_HAND, wrongTypeTool);
        verifyCannotHarvestAndDropsNothing(helper, level, player, pos, target, state, wrongTypeTool, id, "pickaxe");
        if (!underTierTool.isEmpty()) {
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, underTierTool);
            verifyCannotHarvestAndDropsNothing(
                    helper, level, player, pos, target, state, underTierTool, id, "under-tier shovel");
        }

        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, correctTool);
        require(helper, player.hasCorrectToolForDrops(state), id + " rejects the required shovel tier");
        require(helper, state.canHarvestBlock(level, pos, player), id + " server harvest check rejects required shovel");
        Set<Item> normalDropTypes = expectedDropTypes(level, player, pos, state, correctTool);
        require(helper, !normalDropTypes.isEmpty(), id + " loot table produced no normal drops");
        require(helper, rollDrops(level, player, pos, state, correctTool, 0.5F).isEmpty(),
                id + " should produce no minerals at or above the 50% chance boundary");
        verifyExperienceProvider(level, player, pos, state, correctTool, entry, helper, id);
        helper.killAllEntitiesOfClass(ItemEntity.class);
        helper.killAllEntitiesOfClass(ExperienceOrb.class);
        helper.setBlock(TEST_POS, target);
        require(helper, player.gameMode.destroyBlock(pos), id + " was not destroyed by a correct shovel");
        require(helper, level.getBlockState(pos).isAir(), id + " remained after a correct shovel break");
        Set<Item> actualDrops = actualItemTypes(level, pos);
        require(helper, normalDropTypes.containsAll(actualDrops),
                id + " produced an item outside its source loot table: " + actualDrops);
        verifyExperienceOrb(level, pos, entry, helper, id);

        ItemStack silkTool = new ItemStack(correctShovel);
        silkTool.enchant(level.registryAccess().holderOrThrow(Enchantments.SILK_TOUCH), 1);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, silkTool);
        Set<Item> silkDropTypes = expectedDropTypes(level, player, pos, state, silkTool);
        require(helper, silkDropTypes.contains(target.asItem()), id + " silk touch does not drop its own block");
        Block source = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entry.sourceBlock())).orElseThrow();
        require(helper, !silkDropTypes.contains(source.asItem()), id + " silk touch still drops the source ore");
        List<ItemStack> silkDrops = rollDrops(level, player, pos, state, silkTool, 0.5F);
        require(helper, silkDrops.size() == 1 && silkDrops.getFirst().is(target.asItem())
                        && silkDrops.getFirst().getCount() == 1,
                id + " silk touch should always produce exactly one sandified ore block");

        ItemStack fortuneTool = new ItemStack(correctShovel);
        fortuneTool.enchant(level.registryAccess().holderOrThrow(Enchantments.FORTUNE), 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, fortuneTool);
        require(helper, player.hasCorrectToolForDrops(state), id + " rejects a fortune shovel");
        require(helper, !expectedDropTypes(level, player, pos, state, fortuneTool).isEmpty(),
                id + " fortune shovel produced no drops");
        require(helper, rollDrops(level, player, pos, state, fortuneTool, 0.5F).isEmpty(),
                id + " fortune should not bypass the 50% mineral drop chance");

        helper.killAllEntities();
    }

    private static void verifyTierTag(
            GameTestHelper helper,
            BlockState state,
            SandifiedOreDefinitions.ToolTier tier,
            String id) {
        switch (tier) {
            case WOOD -> {
                require(helper, !state.is(BlockTags.NEEDS_STONE_TOOL), id + " incorrectly needs a stone tool");
                require(helper, !state.is(BlockTags.NEEDS_IRON_TOOL), id + " incorrectly needs an iron tool");
            }
            case STONE -> {
                require(helper, state.is(BlockTags.NEEDS_STONE_TOOL), id + " missing needs_stone_tool");
                require(helper, !state.is(BlockTags.NEEDS_IRON_TOOL), id + " incorrectly needs an iron tool");
            }
            case IRON -> {
                require(helper, state.is(BlockTags.NEEDS_IRON_TOOL), id + " missing needs_iron_tool");
                require(helper, !state.is(BlockTags.NEEDS_STONE_TOOL), id + " has conflicting needs_stone_tool");
            }
        }
    }

    private static Item shovelFor(SandifiedOreDefinitions.ToolTier tier) {
        return switch (tier) {
            case WOOD -> Items.WOODEN_SHOVEL;
            case STONE -> Items.STONE_SHOVEL;
            case IRON -> Items.IRON_SHOVEL;
        };
    }

    private static Item underTierShovelFor(SandifiedOreDefinitions.ToolTier tier) {
        return switch (tier) {
            case WOOD -> null;
            case STONE -> Items.WOODEN_SHOVEL;
            case IRON -> Items.STONE_SHOVEL;
        };
    }

    private static void verifyCannotHarvestAndDropsNothing(
            GameTestHelper helper,
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            Block target,
            BlockState state,
            ItemStack tool,
            String id,
            String toolName) {
        require(helper, !player.hasCorrectToolForDrops(state), id + " incorrectly accepts " + toolName);
        require(helper, !state.canHarvestBlock(level, pos, player), id + " server harvest check accepts " + toolName);
        helper.killAllEntities();
        helper.setBlock(TEST_POS, target);
        require(helper, player.gameMode.destroyBlock(pos), id + " could not be broken with " + toolName);
        require(helper, level.getBlockState(pos).isAir(), id + " remained after " + toolName + " break");
        require(helper, actualItemTypes(level, pos).isEmpty(),
                id + " dropped items with " + toolName + " (tool=" + tool.getItem() + ")");
        require(helper, actualExperienceOrbs(level, pos).isEmpty(),
                id + " dropped experience with " + toolName);
    }

    private static Set<Item> expectedDropTypes(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            ItemStack tool) {
        Set<Item> items = new HashSet<>();
        for (ItemStack stack : rollDrops(level, player, pos, state, tool, Math.nextDown(0.5F))) {
            items.add(stack.getItem());
        }
        return items;
    }

    /** Fix the chance roll while retaining the loot table's real count and fortune calculations. */
    private static List<ItemStack> rollDrops(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            float chanceRoll) {
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, tool)
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.BLOCK);
        LootTable table = level.getServer().reloadableRegistries().getLootTable(state.getBlock().getLootTable());
        return table.getRandomItems(params, new LegacyRandomSource(0x5A4D1F1EDL) {
            @Override
            public float nextFloat() {
                return chanceRoll;
            }
        });
    }

    private static Set<Item> actualItemTypes(ServerLevel level, BlockPos pos) {
        Set<Item> items = new HashSet<>();
        for (ItemEntity entity : level.getEntitiesOfClass(
                ItemEntity.class, new AABB(pos).inflate(2.0), ItemEntity::isAlive)) {
            items.add(entity.getItem().getItem());
        }
        return items;
    }

    private static List<ExperienceOrb> actualExperienceOrbs(ServerLevel level, BlockPos pos) {
        return level.getEntitiesOfClass(
                ExperienceOrb.class, new AABB(pos).inflate(2.0), ExperienceOrb::isAlive);
    }

    private static void verifyExperienceProvider(
            ServerLevel level,
            ServerPlayer player,
            BlockPos pos,
            BlockState state,
            ItemStack tool,
            SandifiedOreDefinitions.Entry entry,
            GameTestHelper helper,
            String id) {
        for (long seed = 0; seed < 16; seed++) {
            level.getRandom().setSeed(seed);
            int xp = state.getExpDrop(level, pos, null, player, tool);
            require(helper, xp >= entry.ore().xpMin() && xp <= entry.ore().xpMax(),
                    id + " xp " + xp + " outside [" + entry.ore().xpMin() + ", " + entry.ore().xpMax() + "]");
        }
    }

    private static void verifyExperienceOrb(
            ServerLevel level,
            BlockPos pos,
            SandifiedOreDefinitions.Entry entry,
            GameTestHelper helper,
            String id) {
        List<ExperienceOrb> orbs = actualExperienceOrbs(level, pos);
        if (entry.ore().xpMin() > 0) {
            require(helper, !orbs.isEmpty(), id + " dropped no experience orb");
        } else if (entry.ore().xpMax() == 0) {
            require(helper, orbs.isEmpty(), id + " dropped unexpected experience");
        }
    }

    private static void require(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    private SandifiedOreGameTests() {
    }
}
