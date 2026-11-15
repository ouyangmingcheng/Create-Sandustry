package com.minechai.create_sandustry.content.gravity;

import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;

import net.minecraft.world.level.Level;

/** Display statistics only. Never changes a recipe, its result pool, or an activation roll. */
public final class GravityAssemblyJeiChance {
    public record Totals(double success, double scrap) {}

    private static final Totals TOTALS = calculateTotals();

    private GravityAssemblyJeiChance() {}

    /** Fresh stack, starting at bounce one, uninterrupted until success or damage. */
    private static Totals calculateTotals() {
        double remaining = 1;
        double success = 0;
        double scrap = 0;
        for (int bounce = 1; remaining > 0; bounce++) {
            double successChance = GravityActivationLogic.successChance(bounce);
            double damageChance = GravityActivationLogic.damageChance(0);
            success += remaining * successChance;
            scrap += remaining * (1 - successChance) * damageChance;
            remaining *= (1 - successChance) * (1 - damageChance);
        }
        return new Totals(success, scrap);
    }

    public static Totals totals() { return TOTALS; }

    public static boolean isGravityRecipe(Level level, SequencedAssemblyRecipe recipe) {
        return level != null && level.getRecipeManager().byKey(GravityMechanismAssembly.RECIPE_ID)
                .filter(holder -> holder.value() == recipe).isPresent()
                && recipe.getTransitionalItem().is(ModItems.INCOMPLETE_GRAVITY_MECHANISM.get())
                && recipe.getLoops() == 1 && recipe.getSequence().size() == 2
                && recipe.getSequence().get(1).getRecipe() instanceof BounceActivationRecipe
                && recipe.resultPool.size() == 1
                && recipe.resultPool.getFirst().getStack().is(ModItems.GRAVITY_MECHANISM.get())
                && recipe.getOutputChance() == 1;
    }

    /** Called only at the native JEI category's presentation call sites. */
    public static float outputChance(Level level, SequencedAssemblyRecipe recipe) {
        return isGravityRecipe(level, recipe) ? (float) TOTALS.success() : recipe.getOutputChance();
    }
}
