package com.minechai.create_sandustry.content.kineticpress;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.infrastructure.config.ModConfigs;
import com.minechai.create_sandustry.registry.ModItems;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressRecipeValidationGameTests {
    private KineticPressRecipeValidationGameTests() {}

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void originalRecipeFieldsAndNetworkRoundTripRemainCompatible(GameTestHelper h) {
        var recipe = parse(h, validJson()).getOrThrow();
        h.assertTrue(recipe.processingTime() == 40 && recipe.requiredHeight() == 8 && recipe.baseEnergy() == 200,
                "Recipe timing and height must remain unchanged while energy uses the reduced scale");
        h.assertTrue(recipe.calculateEnergyCost(3, 200) == 1200
                        && recipe.calculateEnergyCost(8, 200) == 200,
                "Existing energy formula must remain unchanged");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            KineticPressRecipeSerializer.STREAM_CODEC.encode(buffer, recipe);
            var decoded = KineticPressRecipeSerializer.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.results().equals(recipe.results()) && decoded.processingTime() == 40
                            && decoded.requiredHeight() == 8 && decoded.baseEnergy() == 200,
                    "Validated network codec must retain existing recipe data");
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void rejectsEmptyAndMoreThanEightResultsWithCodecErrors(GameTestHelper h) {
        JsonObject empty = validJson(); empty.add("results", new JsonArray());
        rejects(h, empty, "Empty results must not decode");
        JsonObject tooMany = validJson(); JsonArray results = new JsonArray();
        for (int i = 0; i < 9; i++) results.add(validJson().getAsJsonArray("results").get(0).deepCopy());
        tooMany.add("results", results);
        rejects(h, tooMany, "Nine result entries must not decode");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void rejectsInvalidTimeEnergyHeightCountAndChance(GameTestHelper h) {
        for (String key : List.of("processing_time", "base_energy", "required_height")) {
            JsonObject invalid = validJson(); invalid.addProperty(key, -1);
            rejects(h, invalid, "Negative " + key + " must not decode");
        }
        JsonObject zeroTime = validJson(); zeroTime.addProperty("processing_time", 0);
        rejects(h, zeroTime, "Zero processing time must not decode");
        JsonObject zeroHeight = validJson(); zeroHeight.addProperty("required_height", 0);
        rejects(h, zeroHeight, "Zero height must not decode");
        JsonObject excessiveHeight = validJson(); excessiveHeight.addProperty("required_height", 257);
        rejects(h, excessiveHeight, "Height beyond schema limit must not decode");
        for (int count : new int[] {0, 100}) {
            JsonObject invalid = validJson(); firstResult(invalid).addProperty("count", count);
            rejects(h, invalid, "Illegal output count must not decode: " + count);
        }
        for (float chance : new float[] {-0.01F, 1.01F, Float.NaN}) {
            JsonObject invalid = validJson(); firstResult(invalid).addProperty("chance", chance);
            rejects(h, invalid, "Illegal output chance must not decode: " + chance);
        }
        JsonObject air = validJson(); firstResult(air).addProperty("item", "minecraft:air");
        rejects(h, air, "Air output must not silently consume an input");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void rejectsRecipesUnreachableWithinHeightAndEnergyCapacity(GameTestHelper h) {
        JsonObject excessiveBase = validJson(); excessiveBase.addProperty("base_energy", KineticPressBlockEntity.MAX_ENERGY + 1);
        rejects(h, excessiveBase, "Base cost above battery capacity must not decode");
        int surcharge = ModConfigs.KINETIC_PRESS_EXTRA_ENERGY_PER_HEIGHT.get();
        JsonObject tall = validJson(); tall.addProperty("required_height", 256);
        long minimum = 200L + 233L * surcharge;
        if (minimum > KineticPressBlockEntity.MAX_ENERGY) {
            var result = parse(h, tall);
            h.assertTrue(result.error().isPresent()
                            && result.error().orElseThrow().message().contains("cannot run at any working height"),
                    "Unreachable recipe must report the minimum-energy failure clearly");
        } else {
            h.assertTrue(parse(h, tall).result().isPresent(),
                    "A tall recipe made reachable by configured surcharge must remain valid");
        }
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void rejectsMaximumOutputsThatCannotFitEmptyCache(GameTestHelper h) {
        JsonObject invalid = validJson();
        firstResult(invalid).addProperty("item", "minecraft:diamond_sword");
        firstResult(invalid).addProperty("count", 10);
        var result = parse(h, invalid);
        h.assertTrue(result.error().isPresent() && result.error().orElseThrow().message().contains("9-slot"),
                "Ten unstackable maximum outputs must be rejected before machine input");
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void zeroChanceAndIndependentPerUnitRollsRetainTheirMeaning(GameTestHelper h) {
        JsonObject zero = validJson(); firstResult(zero).addProperty("chance", 0.0F);
        var zeroRecipe = parse(h, zero).getOrThrow();
        h.assertTrue(zeroRecipe.getMaximumOutputStacks().isEmpty()
                        && zeroRecipe.rollResults(RandomSource.create(17)).isEmpty(),
                "Zero chance must remain zero, without reserving output or generating items");
        var output = new KineticPressOutput(Items.GOLD_NUGGET, 4, 0.25F);
        var actualRandom = RandomSource.create(823741L);
        var expectedRandom = RandomSource.create(823741L);
        for (int trial = 0; trial < 100; trial++) {
            int expected = 0;
            for (int unit = 0; unit < 4; unit++) if (expectedRandom.nextFloat() < 0.25F) expected++;
            h.assertTrue(output.roll(actualRandom).getCount() == expected,
                    "count=4 must mean four independent probability draws, trial=" + trial);
        }
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void networkCodecRejectsInvalidTimeInsteadOfCreatingUnsafeRecipe(GameTestHelper h) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, Ingredient.of(ModItems.BURNT_RESIDUE.get()));
            KineticPressOutput.STREAM_CODEC.apply(ByteBufCodecs.list(KineticPressRecipe.MAX_RESULTS))
                    .encode(buffer, List.of(new KineticPressOutput(Items.GOLD_NUGGET, 1, 1.0F)));
            buffer.writeVarInt(0); buffer.writeVarInt(8); buffer.writeVarInt(200);
            boolean rejected = false;
            try {
                KineticPressRecipeSerializer.STREAM_CODEC.decode(buffer);
            } catch (IllegalArgumentException | DecoderException expected) {
                rejected = true;
            }
            h.assertTrue(rejected, "Network data must reuse structural validation instead of accepting zero time");
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    private static JsonObject validJson() {
        return JsonParser.parseString("""
                {"ingredient":{"item":"createsandustry:burnt_residue"},
                 "results":[{"item":"createsandustry:amethyst_flower_seeds","count":1,"chance":1.0}],
                 "processing_time":40,"required_height":8,"base_energy":200}
                """).getAsJsonObject();
    }

    private static JsonObject firstResult(JsonObject json) {
        return json.getAsJsonArray("results").get(0).getAsJsonObject();
    }

    private static DataResult<KineticPressRecipe> parse(GameTestHelper h, JsonElement json) {
        return KineticPressRecipeSerializer.CODEC.codec().parse(
                RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess()), json);
    }

    private static void rejects(GameTestHelper h, JsonObject json, String message) {
        var result = parse(h, json);
        h.assertTrue(result.error().isPresent() && result.result().isEmpty(), message);
        h.assertTrue(!result.error().orElseThrow().message().isBlank(), "Invalid recipe needs a diagnostic message");
    }
}
