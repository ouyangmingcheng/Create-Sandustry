package com.minechai.create_sandustry.content.kineticpress;

import java.util.List;
import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressContentGameTests {
    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void ponderTemplateIsPackagedAndContainsTheCompactDemoColumn(GameTestHelper h) throws java.io.IOException {
        try (var stream = KineticPressContentGameTests.class.getResourceAsStream(
                "/assets/createsandustry/ponder/kinetic_press/basic.nbt")) {
            h.assertTrue(stream != null, "Registered Ponder template must be packaged");
            var template = net.minecraft.nbt.NbtIo.readCompressed(stream, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            var size = template.getList("size", net.minecraft.nbt.Tag.TAG_INT);
            h.assertTrue(size.size() == 3 && size.getInt(0) == 5 && size.getInt(1) == 8 && size.getInt(2) == 5,
                    "Compact Ponder must contain the y2 base, y5/y6 chute and item controls within eight layers");
            h.assertTrue(!template.getList("blocks", net.minecraft.nbt.Tag.TAG_COMPOUND).isEmpty(),
                    "Ponder template must include a visible floor");
        }
        h.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void survivalRecipeCraftsWithoutRequiringItsOwnMachine(GameTestHelper h) {
        var holder = h.getLevel().getRecipeManager().byKey(
                ResourceLocation.fromNamespaceAndPath(CreateSandustry.MODID, "kinetic_press")).orElseThrow();
        h.assertTrue(holder.value() instanceof CraftingRecipe, "Machine must have a crafting table recipe");
        CraftingRecipe recipe = (CraftingRecipe) holder.value();
        CraftingInput input = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.GRAVITY_MECHANISM.get()), new ItemStack(Items.IRON_INGOT),
                AllItems.ELECTRON_TUBE.asStack(), AllBlocks.BRASS_CASING.asStack(), AllItems.ELECTRON_TUBE.asStack(),
                new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.IRON_INGOT)));
        h.assertTrue(recipe.matches(input, h.getLevel()), "The gravity mechanism and brass-tier materials must craft the machine");
        ItemStack output = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(output.is(ModItems.KINETIC_PRESS.get()) && output.getCount() == 1,
                "Recipe must yield one original machine ID");
        for (var ingredient : recipe.getIngredients()) {
            for (ItemStack choice : ingredient.getItems()) {
                h.assertTrue(!choice.is(ModItems.KINETIC_PRESS.get()) && !choice.is(AllItems.PRECISION_MECHANISM.get()),
                        "Machine recipe must not depend on itself or use a precision mechanism");
            }
        }
        h.succeed();
    }
}
