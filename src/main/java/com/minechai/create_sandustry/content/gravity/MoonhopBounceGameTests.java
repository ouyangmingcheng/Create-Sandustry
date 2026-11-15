package com.minechai.create_sandustry.content.gravity;

import java.util.concurrent.atomic.AtomicInteger;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.moonhop.MoonhopBlock;
import com.minechai.create_sandustry.registry.ModAttachments;
import com.minechai.create_sandustry.registry.ModBlocks;
import com.minechai.create_sandustry.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class MoonhopBounceGameTests {
    private MoonhopBounceGameTests() {}

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 60)
    public static void moonhopSableRealTopImpacts(GameTestHelper helper) {
        if (ModList.get().isLoaded("sable")) SableMoonhopGameTests.impacts(helper); else helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 60)
    public static void moonhopSableRepeatedFlightsAndMechanism(GameTestHelper helper) {
        if (ModList.get().isLoaded("sable")) SableMoonhopGameTests.flights(helper); else helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 60)
    public static void moonhopSableContactExclusions(GameTestHelper helper) {
        if (ModList.get().isLoaded("sable")) SableMoonhopGameTests.exclusions(helper); else helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 60)
    public static void moonhopSableMovingAndRotatingFlights(GameTestHelper helper) {
        if (ModList.get().isLoaded("sable")) SableMoonhopGameTests.movingFlights(helper); else helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID)
    public static void moonhopPropertiesCodecAndVanillaCallbacks(GameTestHelper helper) {
        BlockPos pos = surface(helper, ModBlocks.MOONHOP_BLOCK.get());
        MoonhopBlock block = ModBlocks.MOONHOP_BLOCK.get();
        BlockState state = block.defaultBlockState();
        helper.assertTrue(state.getDestroySpeed(helper.getLevel(), pos) == 0 && block.getExplosionResistance() == 0,
                "Moonhop retains slime's zero hardness and blast resistance");
        helper.assertTrue(block.getFriction() == 0.8F && state.getSoundType() == SoundType.SLIME_BLOCK
                        && state.getPistonPushReaction() == PushReaction.NORMAL
                        && state.getCollisionShape(helper.getLevel(), pos).bounds().equals(
                                Blocks.SLIME_BLOCK.defaultBlockState().getCollisionShape(helper.getLevel(), pos).bounds()),
                "Slime friction, all sound events, collision and normal piston movement are retained");
        helper.assertTrue(block.codec() == MoonhopBlock.CODEC,
                "The concrete codec is MoonhopBlock's codec, rather than SlimeBlock's codec");
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5);
        player.setDeltaMovement(0.3, -1, 0.2);
        player.move(MoverType.SELF, player.getDeltaMovement());
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - 1.0) < 1e-12,
                "Strong impacts retain vanilla reflection without the previous additive bonus");
        block.updateEntityAfterFallOn(helper.getLevel(), player);
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - 1.0) < 1e-12,
                "An already positive velocity cannot receive another rebound");
        player.setPos(pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() + 0.5);
        player.setDeltaMovement(0, -0.02, 0);
        player.move(MoverType.SELF, player.getDeltaMovement());
        helper.assertTrue(player.getDeltaMovement().y == MoonhopBlock.MINIMUM_BOUNCE_VELOCITY,
                "Even a very slow living landing receives the full minimum velocity");
        float health = player.getHealth();
        block.fallOn(helper.getLevel(), state, pos, player, 10);
        helper.assertTrue(player.getHealth() == health, "Normal landing has slime's zero fall damage");
        player.setShiftKeyDown(true);
        player.setDeltaMovement(0.3, -1, 0.2);
        block.updateEntityAfterFallOn(helper.getLevel(), player);
        helper.assertTrue(player.getDeltaMovement().y == 0, "Sneaking suppresses the entire rebound");
        block.fallOn(helper.getLevel(), state, pos, player, 10);
        helper.assertTrue(player.getHealth() < health, "Sneaking retains vanilla fall damage");
        player.setShiftKeyDown(false);
        Vec3 walking = new Vec3(0.3, 0.03, 0.2);
        player.setDeltaMovement(walking);
        block.stepOn(helper.getLevel(), pos, state, player);
        Vec3 moonhopWalking = player.getDeltaMovement();
        player.setDeltaMovement(walking);
        Blocks.SLIME_BLOCK.stepOn(helper.getLevel(), pos, Blocks.SLIME_BLOCK.defaultBlockState(), player);
        helper.assertTrue(player.getDeltaMovement().equals(moonhopWalking), "stepOn matches the 1.21.1 slime slowdown");
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID)
    public static void moonhopPistonResolverRejectsBothAdhesionDirections(GameTestHelper helper) {
        BlockPos piston = helper.absolutePos(new BlockPos(5, 5, 5));
        BlockState moon = ModBlocks.MOONHOP_BLOCK.get().defaultBlockState();
        for (Direction direction : Direction.values()) {
            for (boolean extending : new boolean[]{true, false}) {
                BlockPos root = piston.relative(direction, extending ? 1 : 2);
                for (Block neighbour : new Block[]{Blocks.DIRT, Blocks.SLIME_BLOCK, Blocks.HONEY_BLOCK, moon.getBlock()}) {
                    for (Direction side : Direction.values()) {
                        if (side.getAxis() == direction.getAxis()) continue;
                        clear(helper, piston, 4);
                        helper.getLevel().setBlock(root, moon, 2);
                        helper.getLevel().setBlock(root.relative(side), neighbour.defaultBlockState(), 2);
                        var resolver = new PistonStructureResolver(helper.getLevel(), piston, direction, extending);
                        helper.assertTrue(resolver.resolve() && resolver.getToPush().size() == 1
                                        && resolver.getToPush().contains(root),
                                "Moving moonhop must not drag any side neighbour: " + neighbour + direction + extending);
                        if (neighbour == Blocks.SLIME_BLOCK || neighbour == Blocks.HONEY_BLOCK) {
                            helper.getLevel().setBlock(root, neighbour.defaultBlockState(), 2);
                            helper.getLevel().setBlock(root.relative(side), moon, 2);
                            resolver = new PistonStructureResolver(helper.getLevel(), piston, direction, extending);
                            helper.assertTrue(resolver.resolve() && !resolver.getToPush().contains(root.relative(side)),
                                    "Moving sticky neighbours cannot drag moonhop in the reverse check");
                        }
                    }
                }
            }
        }
        clear(helper, piston, 4);
        for (int i = 1; i <= 3; i++) helper.getLevel().setBlock(piston.east(i), i == 3 ? Blocks.DIRT.defaultBlockState() : moon, 2);
        var line = new PistonStructureResolver(helper.getLevel(), piston, Direction.EAST, true);
        helper.assertTrue(line.resolve() && line.getToPush().size() == 3,
                "Non-adhesive blocks still push a contiguous line normally");
        helper.assertTrue(!moon.isStickyBlock() && !moon.isSlimeBlock() && !moon.canStickTo(moon),
                "No self adhesion or moving-slime classification");
        for (Block block : BuiltInRegistries.BLOCK) {
            BlockState other = block.defaultBlockState();
            if (other.isStickyBlock()) helper.assertTrue(!moon.canStickTo(other),
                    "Moonhop vetoes adhesion to every installed sticky block: " + block);
        }
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void moonhopRealPistonsPushAndRetractWithoutDraggingNeighbours(GameTestHelper helper) {
        BlockPos normal = helper.absolutePos(new BlockPos(2, 3, 3));
        BlockPos sticky = helper.absolutePos(new BlockPos(2, 3, 7));
        BlockState moon = ModBlocks.MOONHOP_BLOCK.get().defaultBlockState();
        for (BlockPos piston : new BlockPos[]{normal, sticky}) {
            helper.getLevel().setBlockAndUpdate(piston, (piston.equals(normal) ? Blocks.PISTON : Blocks.STICKY_PISTON)
                    .defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.getLevel().setBlockAndUpdate(piston.east(), moon);
            helper.getLevel().setBlockAndUpdate(piston.east(2), moon);
            helper.getLevel().setBlockAndUpdate(piston.east(3), Blocks.DIRT.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(piston.east().north(), Blocks.SLIME_BLOCK.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(piston.east().south(), Blocks.HONEY_BLOCK.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(piston.west(), Blocks.REDSTONE_BLOCK.defaultBlockState());
        }
        helper.runAfterDelay(5, () -> {
            for (BlockPos piston : new BlockPos[]{normal, sticky}) {
                helper.assertTrue(helper.getLevel().getBlockState(piston.east(2)).is(moon.getBlock())
                                && helper.getLevel().getBlockState(piston.east(3)).is(moon.getBlock())
                                && helper.getLevel().getBlockState(piston.east(4)).is(Blocks.DIRT),
                        "Both piston types push the complete straight line");
                helper.getLevel().setBlockAndUpdate(piston.west(), Blocks.AIR.defaultBlockState());
            }
            helper.runAfterDelay(5, () -> {
                helper.assertTrue(helper.getLevel().getBlockState(normal.east(2)).is(moon.getBlock())
                                && helper.getLevel().getBlockState(sticky.east()).is(moon.getBlock())
                                && helper.getLevel().getBlockState(sticky.east(2)).isAir()
                                && helper.getLevel().getBlockState(sticky.east(3)).is(moon.getBlock()),
                        "Normal piston leaves blocks; sticky piston pulls only the first moonhop back");
                for (BlockPos piston : new BlockPos[]{normal, sticky})
                    helper.assertTrue(helper.getLevel().getBlockState(piston.east().north()).is(Blocks.SLIME_BLOCK)
                                    && helper.getLevel().getBlockState(piston.east().south()).is(Blocks.HONEY_BLOCK),
                            "Adjacent slime and honey remain at their original positions");
                helper.succeed();
            });
        });
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void moonhopPlayersAndMobsMaintainStableMinimumFlights(GameTestHelper helper) {
        for (double height : new double[]{0.02, 2, 8, 20}) {
            Cow cow = new Cow(EntityType.COW, helper.getLevel()) {
                {
                    goalSelector.removeAllGoals(goal -> true);
                    targetSelector.removeAllGoals(goal -> true);
                }
            };
            livingTrace(helper, cow, height, "cow");
            livingTrace(helper, helper.makeMockPlayer(GameType.SURVIVAL), height, "player");
        }
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID)
    public static void moonhopItemSuppressionAndNoGravityKeepVanillaLanding(GameTestHelper helper) {
        BlockPos pos = surface(helper, ModBlocks.MOONHOP_BLOCK.get());
        for (boolean noGravity : new boolean[]{false, true}) {
            ItemEntity item = item(helper, pos, 0.1, new ItemStack(Items.COBBLESTONE));
            item.setNoGravity(noGravity);
            item.setShiftKeyDown(!noGravity);
            item.setDeltaMovement(0, -1, 0);
            item.tickCount++;
            item.tick();
            helper.assertTrue(item.onGround() && item.getDeltaMovement().y == 0,
                    "Suppressed/no-gravity item lands without a new impulse");
        }
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID)
    public static void moonhopSidesCeilingsLiquidsAndDepartureCannotLaunch(GameTestHelper helper) {
        BlockPos pos = surface(helper, ModBlocks.MOONHOP_BLOCK.get());
        ItemEntity side = item(helper, pos, 0, new ItemStack(Items.COBBLESTONE));
        side.setPos(pos.getX() - 0.3, pos.getY() + 0.3, pos.getZ() + 0.5);
        side.setDeltaMovement(1, 0, 0);
        side.move(MoverType.SELF, side.getDeltaMovement());
        helper.assertTrue(side.horizontalCollision && side.getDeltaMovement().y == 0,
                "A side collision supplies no vertical impulse");

        ItemEntity below = item(helper, pos, 0, new ItemStack(Items.COBBLESTONE));
        below.setPos(pos.getX() + 0.5, pos.getY() - below.getBbHeight() - 0.1, pos.getZ() + 0.5);
        below.setDeltaMovement(0, 1, 0);
        below.move(MoverType.SELF, below.getDeltaMovement());
        helper.assertTrue(below.verticalCollision && !below.verticalCollisionBelow && below.getDeltaMovement().y == 0,
                "The underside acts as a ceiling, with no rebound");

        helper.getLevel().setBlock(pos.above(), Blocks.WATER.defaultBlockState(), 2);
        ItemEntity wet = item(helper, pos, 0.1, new ItemStack(Items.COBBLESTONE));
        wet.setDeltaMovement(0, -1, 0);
        wet.tickCount++;
        wet.tick();
        helper.assertTrue(wet.getDeltaMovement().y <= 0, "A water-covered top supplies no launch");
        helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);

        // A half-block-high orb exactly fills the gap below this top slab.
        helper.getLevel().setBlock(pos.above(), Blocks.SMOOTH_STONE_SLAB.defaultBlockState()
                .setValue(SlabBlock.TYPE, SlabType.TOP), 2);
        var blocked = EntityType.EXPERIENCE_ORB.create(helper.getLevel());
        blocked.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        for (int tick = 0; tick < 10; tick++) {
            blocked.setDeltaMovement(0, -0.04, 0);
            blocked.move(MoverType.SELF, blocked.getDeltaMovement());
            helper.assertTrue(blocked.onGround() && blocked.getDeltaMovement().y == 0,
                    "No upward clearance must not produce repeated floor/ceiling impulses");
        }
        helper.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 2);

        helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
        ItemEntity departed = item(helper, pos, 0.1, new ItemStack(Items.COBBLESTONE));
        departed.setDeltaMovement(0, -1, 0);
        departed.tickCount++;
        departed.tick();
        helper.assertTrue(departed.onGround() && departed.getDeltaMovement().y == 0,
                "Landing on another block receives no leftover moonhop impulse");
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void moonhopOrdinaryItemsBounceContinuouslyWithoutExtendingLifetime(GameTestHelper helper) {
        BlockPos pos = surface(helper, ModBlocks.MOONHOP_BLOCK.get());
        for (double height : new double[]{0, 0.02, 2, 8}) {
            ItemEntity item = item(helper, pos, height, new ItemStack(Items.COBBLESTONE));
            int bounces = 0;
            int minimumBounces = 0;
            double previousRebound = Double.POSITIVE_INFINITY;
            for (int tick = 0; tick < 1400; tick++) {
                double incoming = item.getDeltaMovement().y - item.getGravity();
                item.tickCount++;
                item.tick();
                if (item.verticalCollisionBelow && incoming < 0) {
                    double expected = Math.max(-incoming * 0.8, MoonhopBlock.MINIMUM_BOUNCE_VELOCITY) * 0.98;
                    helper.assertTrue(Math.abs(item.getDeltaMovement().y - expected) < 1e-10,
                            "Each landing applies the minimum once, then vanilla drag once");
                    helper.assertTrue(expected <= previousRebound + 1e-10, "Rebound speeds decline to a stable floor");
                    previousRebound = expected;
                    if (Math.abs(expected - MoonhopBlock.MINIMUM_BOUNCE_VELOCITY * 0.98) < 1e-10) minimumBounces++;
                    bounces++;
                    Vec3 after = item.getDeltaMovement();
                    ModBlocks.MOONHOP_BLOCK.get().updateEntityAfterFallOn(helper.getLevel(), item);
                    helper.assertTrue(item.getDeltaMovement().equals(after), "Duplicate callback cannot add impulse");
                }
                helper.assertTrue(item.isAlive() && !item.onGround(), "Items never settle during the trace");
            }
            helper.assertTrue(bounces > 50 && minimumBounces > 40, "Items repeatedly return to the minimum flight");
            helper.assertTrue(item.getAge() == 1400 && item.lifespan == 6000, "Bounce never resets item age or lifespan");
            helper.assertTrue(!item.hasData(ModAttachments.GRAVITY_BOUNCE), "Ordinary items have no processing tracker");
            CreateSandustry.LOGGER.info("MOONHOP_INFINITE_ITEM,height={},bounces={},minimum_bounces={},age={}",
                    height, bounces, minimumBounces, item.getAge());
        }
        ItemEntity expiring = item(helper, pos, 0, new ItemStack(Items.COBBLESTONE));
        CompoundTag saved = expiring.saveWithoutId(new CompoundTag());
        saved.putShort("Age", (short)5990);
        expiring.load(saved);
        for (int tick = 0; tick < 10; tick++) { expiring.tickCount++; expiring.tick(); }
        helper.assertTrue(expiring.isRemoved() && expiring.getAge() == 6000, "Bouncing item still expires at its natural lifetime");
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID, timeoutTicks = 40)
    public static void moonhopGravityChainCountsOneFlightAndUsesExistingAssembly(GameTestHelper helper) {
        BlockPos pos = surface(helper, ModBlocks.MOONHOP_BLOCK.get());
        for (double height : new double[]{0, 0.02, 8}) {
            ItemStack incomplete = GravityMechanismAssembly.createIncomplete(helper.getLevel());
            helper.assertTrue(!incomplete.isEmpty(), "Use the actual Create assembly intermediate");
            ItemEntity item = item(helper, pos, height, incomplete);
            AtomicInteger apices = new AtomicInteger();
            int collisions = 0;
            // Suppress random outcomes only in this local test callback to observe forty flights.
            // Production still rolls success/damage normally at every effective apex.
            for (int tick = 0; tick < 1600 && apices.get() < 40; tick++) {
                item.tickCount++;
                GravityBouncePhysics.beforeTick(item, (entity, count) -> {
                    helper.assertTrue(count == Math.min(15, apices.incrementAndGet()),
                            "Each apex increments once, retaining the consecutive cap of fifteen");
                    helper.assertTrue(GravityMechanismData.interruptions(entity.getItem()) == 0,
                            "Minimum-speed flights must not disconnect a chain");
                });
                item.tick();
                Vec3 nativeMotion = item.getDeltaMovement();
                boolean bounced = item.verticalCollisionBelow && nativeMotion.y > item.getGravity();
                GravityBouncePhysics.afterTick(item, GravityBounceSurfaces::getBounceCoefficient,
                        GravityBouncePhysics.MINIMUM_IMPACT_SPEED);
                if (bounced) {
                    collisions++;
                    helper.assertTrue(item.getDeltaMovement().equals(nativeMotion), "Tracker cannot bounce twice");
                }
                Vec3 postMotion = item.getDeltaMovement();
                GravityBouncePhysics.afterTick(item, GravityBounceSurfaces::getBounceCoefficient,
                        GravityBouncePhysics.MINIMUM_IMPACT_SPEED);
                helper.assertTrue(item.getDeltaMovement().equals(postMotion), "Duplicate Post cannot bounce twice");
            }
            helper.assertTrue(apices.get() == 40 && collisions == apices.get()
                            && GravityMechanismData.totalBounces(item.getItem()) == apices.get()
                            && GravityMechanismData.interruptions(item.getItem()) == 0,
                    "Forty complete flights count exactly once without false interruption");
            ItemStack completed = GravityMechanismAssembly.complete(helper.getLevel(), item.getItem());
            helper.assertTrue(completed.is(ModItems.GRAVITY_MECHANISM.get()), "Existing processing step completes unchanged");
            CreateSandustry.LOGGER.info("MOONHOP_INFINITE_GRAVITY,height={},collisions={},apices={},interruptions={}",
                    height, collisions, apices.get(), GravityMechanismData.interruptions(item.getItem()));
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 2);
            item.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            item.setDeltaMovement(Vec3.ZERO);
            for (int tick = 0; tick < 20; tick++) {
                item.tickCount++;
                GravityBouncePhysics.beforeTick(item, (entity, count) -> helper.fail("No apex after departure"));
                item.tick();
                GravityBouncePhysics.afterTick(item, GravityBounceSurfaces::getBounceCoefficient,
                        GravityBouncePhysics.MINIMUM_IMPACT_SPEED);
            }
            helper.assertTrue(GravityMechanismData.interruptions(item.getItem()) == 1
                            && item.getData(ModAttachments.GRAVITY_BOUNCE).consecutiveBounces() == 0,
                    "A genuine departure disconnects once and retains existing wear rules");
            helper.getLevel().setBlock(pos, ModBlocks.MOONHOP_BLOCK.get().defaultBlockState(), 2);
        }
        helper.succeed();
    }

    @GameTest(template = "moonhop_physics", templateNamespace = CreateSandustry.MODID)
    public static void moonhopSableLoadedRestitutionWhenInstalled(GameTestHelper helper) throws ReflectiveOperationException {
        if (ModList.get().isLoaded("sable")) {
            // Optional assertion of the resolved datapack property, including tag preset priority.
            helper.assertTrue(ModBlocks.MOONHOP_BLOCK.get().defaultBlockState().is(net.minecraft.tags.TagKey.create(
                            net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.ResourceLocation.parse("sable:bouncy"))),
                    "The optional Sable bouncy tag is merged without replacing its vanilla members");
            var method = Class.forName("dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper")
                    .getMethod("getRestitution", BlockState.class);
            double moonhop = (double) method.invoke(null, ModBlocks.MOONHOP_BLOCK.get().defaultBlockState());
            double slime = (double) method.invoke(null, Blocks.SLIME_BLOCK.defaultBlockState());
            helper.assertTrue(Math.abs(moonhop - 0.8) < 1e-12 && Math.abs(slime - 0.5) < 1e-12,
                    "Sable actually loaded moonhop=0.8 and vanilla slime=0.5 from datapacks");
            CreateSandustry.LOGGER.info("MOONHOP_SABLE_PROPERTIES,moonhop={},slime={}", moonhop, slime);
        } else {
            helper.assertTrue(ModBlocks.MOONHOP_BLOCK.get() instanceof MoonhopBlock,
                    "Moonhop registers and runs without Sable");
            CreateSandustry.LOGGER.info("MOONHOP_WITHOUT_SABLE,registered=true");
        }
        helper.succeed();
    }

    private static void livingTrace(GameTestHelper helper, LivingEntity entity, double height, String kind) {
        BlockPos pos = surface(helper, ModBlocks.MOONHOP_BLOCK.get());
        entity.setPos(pos.getX() + 0.5, pos.getY() + 1 + height, pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        int collisions = 0;
        int minimumBounces = 0;
        double previousRebound = Double.POSITIVE_INFINITY;
        double previousApex = Double.POSITIVE_INFINITY;
        double apex = 0;
        for (int tick = 0; tick < 1800; tick++) {
            double incoming = entity.getDeltaMovement().y;
            entity.tickCount++;
            entity.tick();
            if (entity.verticalCollisionBelow && incoming < 0) {
                double expectedRebound = Math.max(-incoming, MoonhopBlock.MINIMUM_BOUNCE_VELOCITY);
                helper.assertTrue(expectedRebound <= previousRebound + 1e-9, kind + " rebound cannot accumulate energy");
                if (collisions > 1) helper.assertTrue(apex <= previousApex + 1e-6, kind + " apex heights cannot grow");
                previousApex = apex;
                apex = 0;
                previousRebound = expectedRebound;
                if (Math.abs(expectedRebound - MoonhopBlock.MINIMUM_BOUNCE_VELOCITY) < 1e-9) minimumBounces++;
                helper.assertTrue(entity.getDeltaMovement().y > 0, kind + " must take off on each top collision");
                collisions++;
            }
            apex = Math.max(apex, entity.getY() - pos.getY() - 1);
        }
        helper.assertTrue(collisions > 50 && minimumBounces > 40, kind + " must maintain repeated minimum flights");
        CreateSandustry.LOGGER.info("MOONHOP_INFINITE_LIVING,kind={},height={},collisions={},minimum_bounces={}",
                kind, height, collisions, minimumBounces);
    }

    private static ItemEntity item(GameTestHelper helper, BlockPos pos, double height, ItemStack stack) {
        ItemEntity entity = new ItemEntity(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 1 + height,
                pos.getZ() + 0.5, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        return entity;
    }

    private static BlockPos surface(GameTestHelper helper, Block block) {
        BlockPos pos = helper.absolutePos(new BlockPos(5, 2, 5));
        for (int y = 1; y < 25; y++) helper.getLevel().setBlock(pos.above(y), Blocks.AIR.defaultBlockState(), 2);
        helper.getLevel().setBlock(pos, block.defaultBlockState(), 2);
        return pos;
    }

    private static void clear(GameTestHelper helper, BlockPos pos, int radius) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius)))
            helper.getLevel().setBlock(p, Blocks.AIR.defaultBlockState(), 2);
    }
}
