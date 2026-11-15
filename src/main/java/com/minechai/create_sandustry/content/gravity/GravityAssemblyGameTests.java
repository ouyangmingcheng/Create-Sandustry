package com.minechai.create_sandustry.content.gravity;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;
import com.minechai.create_sandustry.registry.ModRecipeTypes;
import com.mojang.serialization.JsonOps;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;

import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class GravityAssemblyGameTests {
    private GravityAssemblyGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void nativeDeployerCreatesIntermediateAndCannotSkipBounce(GameTestHelper helper) {
        var level = helper.getLevel();
        ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, ModItems.MOONHOP_BOARD.get().getDefaultInstance());
        inventory.setStackInSlot(1, AllItems.PRECISION_MECHANISM.asStack());
        RecipeWrapper wrapper = new RecipeWrapper(inventory);
        var deploying = SequencedAssemblyRecipe.getRecipe(level, wrapper,
                AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
        helper.assertTrue(deploying.isPresent(), "Moonhop board and precision mechanism must match native deploying");
        helper.assertTrue(deploying.get().id().equals(GravityMechanismAssembly.RECIPE_ID),
                "Deployer must select the real gravity sequenced-assembly recipe");
        var outputs = deploying.get().value().rollResults(level.random);
        helper.assertTrue(outputs.size() == 1, "Deployer must produce one intermediate");
        ItemStack intermediate = outputs.getFirst();
        helper.assertTrue(GravityMechanismAssembly.isAwaitingActivation(level, intermediate),
                "Deployer output must carry the native waiting step and progress");
        SequencedAssemblyItem item = ModItems.INCOMPLETE_GRAVITY_MECHANISM.get();
        helper.assertTrue(intermediate.getMaxStackSize() == 1 && item.isBarVisible(intermediate)
                        && item.getProgress(intermediate) == 0.5F && item.getBarWidth(intermediate) == 7,
                "Intermediate must use Create's native one-item stack and half-complete progress bar");

        inventory.setStackInSlot(0, intermediate);
        helper.assertTrue(SequencedAssemblyRecipe.getRecipe(level, wrapper,
                        AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isEmpty(),
                "Repeating the deployer must not bypass the bounce step");
        var bounce = SequencedAssemblyRecipe.getRecipe(level, intermediate,
                ModRecipeTypes.BOUNCE_ACTIVATION.get(), BounceActivationRecipe.class);
        helper.assertTrue(bounce.isPresent()
                        && !bounce.get().value().matches(new SingleRecipeInput(intermediate), level),
                "Bounce step must be selectable by explicit activation and unavailable to generic machine searches");
        ItemStack completed = GravityMechanismAssembly.complete(level, intermediate);
        helper.assertTrue(completed.is(ModItems.GRAVITY_MECHANISM.get()) && completed.getCount() == 1
                        && !completed.has(AllDataComponents.SEQUENCED_ASSEMBLY),
                "Server completion must advance Create's final step to one clean gravity mechanism");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void invalidAssemblyStatesAreRejected(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(!GravityMechanismAssembly.isAwaitingActivation(level,
                        ModItems.INCOMPLETE_GRAVITY_MECHANISM.get().getDefaultInstance()),
                "An unprocessed intermediate must not activate");
        ItemStack valid = GravityMechanismAssembly.createIncomplete(level);
        helper.assertTrue(GravityMechanismAssembly.isAwaitingActivation(level, valid),
                "Native first-step result must activate");
        for (SequencedAssembly invalid : new SequencedAssembly[] {
                new SequencedAssembly(ResourceLocation.withDefaultNamespace("missing"), 1, 0.5F),
                new SequencedAssembly(GravityMechanismAssembly.RECIPE_ID, 0, 0.0F),
                new SequencedAssembly(GravityMechanismAssembly.RECIPE_ID, 3, 0.5F),
                new SequencedAssembly(GravityMechanismAssembly.RECIPE_ID, -1, 0.5F),
                new SequencedAssembly(GravityMechanismAssembly.RECIPE_ID, 1, Float.NaN),
                new SequencedAssembly(GravityMechanismAssembly.RECIPE_ID, 1, 1.0F) }) {
            ItemStack corrupted = valid.copy();
            corrupted.set(AllDataComponents.SEQUENCED_ASSEMBLY, invalid);
            helper.assertTrue(!GravityMechanismAssembly.isAwaitingActivation(level, corrupted)
                            && GravityMechanismAssembly.complete(level, corrupted).isEmpty(),
                    "Invalid assembly id, step, or progress must not activate or complete: " + invalid);
        }
        helper.assertTrue(!GravityMechanismAssembly.isAwaitingActivation(level, valid.copyWithCount(2)),
                "Invalid oversized intermediate stacks must not process into a single output");
        ItemStack wrongItem = new ItemStack(Items.STONE);
        wrongItem.set(AllDataComponents.SEQUENCED_ASSEMBLY, valid.get(AllDataComponents.SEQUENCED_ASSEMBLY));
        helper.assertTrue(!GravityMechanismAssembly.isAwaitingActivation(level, wrongItem),
                "Attaching assembly data to an unrelated item must not activate it");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void customAssemblyStepSurvivesJsonAndNetworkCodecs(GameTestHelper helper) {
        var level = helper.getLevel();
        var original = level.getRecipeManager().byKey(GravityMechanismAssembly.RECIPE_ID).orElseThrow().value();
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        var json = Recipe.CODEC.encodeStart(ops, original).getOrThrow();
        Recipe<?> fromJson = Recipe.CODEC.parse(ops, json).getOrThrow();
        assertSteps(helper, fromJson);

        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
        try {
            Recipe.STREAM_CODEC.encode(buffer, original);
            Recipe<?> fromNetwork = Recipe.STREAM_CODEC.decode(buffer);
            assertSteps(helper, fromNetwork);
            helper.assertTrue(buffer.readableBytes() == 0, "Recipe network codec must consume the complete packet");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void tooltipShowsNextAttemptAndPreservesForeignLines(GameTestHelper helper) {
        ItemStack stack = GravityMechanismAssembly.createIncomplete(helper.getLevel());
        var next = net.minecraft.network.chat.Component.translatable("create.recipe.assembly.next",
                net.minecraft.network.chat.Component.translatable("recipe.createsandustry.assembly.bounce_activation"));
        var foreign = net.minecraft.network.chat.Component.literal("Another mod's information");
        var lines = new java.util.ArrayList<net.minecraft.network.chat.Component>(java.util.List.of(next, foreign));
        GravityTooltipEvents.adapt(helper.getLevel(), stack, lines);
        helper.assertTrue(lines.size() == 4 && lines.get(0) == next && lines.get(3) == foreign, "Preserve native next and other mods' lines");
        var success = (net.minecraft.network.chat.contents.TranslatableContents) lines.get(1).getContents();
        helper.assertTrue(success.getArgs()[0].equals("5.00") && lines.get(1).getStyle().getColor().getValue() == 0x55FF55, "First success is green 5%");
        stack.set(com.minechai.create_sandustry.registry.ModDataComponents.GRAVITY_TOTAL_BOUNCES.get(), 3);
        lines = new java.util.ArrayList<>(java.util.List.of(next, foreign));
        GravityTooltipEvents.adapt(helper.getLevel(), stack, lines);
        var damage = (net.minecraft.network.chat.contents.TranslatableContents) lines.get(2).getContents();
        helper.assertTrue(damage.getArgs()[0].equals("5.00") && lines.get(2).getStyle().getColor().getValue() == 0xFFAA00,
                "Legacy total bounce statistics do not increase the gold damage probability");
        stack.set(com.minechai.create_sandustry.registry.ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get(), 2);
        lines = new java.util.ArrayList<>(java.util.List.of(next, foreign));
        GravityTooltipEvents.adapt(helper.getLevel(), stack, lines);
        damage = (net.minecraft.network.chat.contents.TranslatableContents) lines.get(2).getContents();
        helper.assertTrue(damage.getArgs()[0].equals("20.00"), "Two interruptions display the shared 20% damage rule");
        GravityTooltipEvents.adapt(helper.getLevel(), stack, lines);
        helper.assertTrue(lines.size() == 4, "No duplicated probability lines");
        helper.assertTrue(GravityTooltipEvents.percentage(GravityActivationLogic.successChance(15)).equals("100.00"), "Exact guaranteed percentage");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void jeiDisplayTotalsNeverChangeTheRealResultPool(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = (SequencedAssemblyRecipe) level.getRecipeManager()
                .byKey(GravityMechanismAssembly.RECIPE_ID).orElseThrow().value();
        var totals = GravityAssemblyJeiChance.totals();
        helper.assertTrue(Math.abs(totals.success() - 0.8306623798759742) < 1e-12
                        && Math.abs(totals.scrap() - 0.1693376201240256) < 1e-12
                        && Math.abs(totals.success() + totals.scrap() - 1) < 1e-12,
                "Full uninterrupted-run probabilities must include success before conditional damage");
        helper.assertTrue(Math.abs(GravityAssemblyJeiChance.outputChance(level, recipe) - totals.success()) < 1e-7,
                "Only JEI's presentation chance exposes the theoretical overall success rate");
        helper.assertTrue(recipe.getOutputChance() == 1 && recipe.resultPool.size() == 1
                        && recipe.resultPool.getFirst().getChance() == 1,
                "Reading the display chance must not mutate the actual weighted result pool");
        var unrelated = new SequencedAssemblyRecipe(
                (com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeSerializer) recipe.getSerializer());
        unrelated.resultPool.addAll(recipe.resultPool);
        helper.assertTrue(GravityAssemblyJeiChance.outputChance(level, unrelated) == 1
                        && !GravityAssemblyJeiChance.isGravityRecipe(level, unrelated),
                "Another recipe with the same output cannot acquire gravity's display probability");
        ItemStack completed = GravityMechanismAssembly.complete(level, GravityMechanismAssembly.createIncomplete(level));
        helper.assertTrue(completed.is(ModItems.GRAVITY_MECHANISM.get()),
                "After the activation success roll, native final advancement still yields only the mechanism");
        helper.succeed();
    }

    private static void assertSteps(GameTestHelper helper, Recipe<?> recipe) {
        helper.assertTrue(recipe instanceof SequencedAssemblyRecipe, "Decoded recipe must be native sequenced assembly");
        SequencedAssemblyRecipe sequence = (SequencedAssemblyRecipe) recipe;
        helper.assertTrue(sequence.getSequence().size() == 2 && sequence.getLoops() == 1
                        && sequence.getSequence().getFirst().getRecipe() instanceof DeployerApplicationRecipe
                        && sequence.getSequence().get(1).getRecipe() instanceof BounceActivationRecipe,
                "Both JSON and network codecs must preserve deploying plus the registered custom bounce step");
    }
}
