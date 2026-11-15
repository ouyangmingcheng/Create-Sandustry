package com.minechai.create_sandustry.content.gravity;

import java.util.Objects;
import java.util.function.DoubleSupplier;

import com.minechai.create_sandustry.registry.ModDataComponents;
import com.minechai.create_sandustry.registry.ModItems;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Probability rules are pure; only activate mutates a server-side item entity. */
public final class GravityActivationLogic {
    public enum Outcome {
        SUCCESS,
        DAMAGE,
        CONTINUE
    }

    public record ActivationResult(Outcome outcome, ItemStack stack) {
    }

    private GravityActivationLogic() {
    }

    public static double successChance(int consecutiveBounces) {
        if (consecutiveBounces <= 0)
            return 0;
        return consecutiveBounces >= 15 ? 1 : 0.05 + 0.95 * (consecutiveBounces - 1) / 14.0;
    }

    public static double damageChance(int interruptionCount) {
        // The probability naturally reaches 100% at five interruptions; avoid overflow thereafter.
        return interruptionCount >= 5 ? 1.0 : 0.05 * (1 << Math.max(0, interruptionCount));
    }

    public static Outcome evaluate(int consecutiveBounces, int interruptionCount, DoubleSupplier random) {
        if (consecutiveBounces <= 0)
            return Outcome.CONTINUE;
        if (consecutiveBounces >= 15 || random.getAsDouble() < successChance(consecutiveBounces))
            return Outcome.SUCCESS;
        if (random.getAsDouble() < damageChance(interruptionCount))
            return Outcome.DAMAGE;
        return Outcome.CONTINUE;
    }

    /** Called once by a motion source at its apex, after the bounce counter was persisted. */
    public static Outcome activate(ItemEntity entity, int consecutiveBounces, GravityActivationSource source) {
        if (entity.isRemoved())
            return Outcome.CONTINUE;
        ActivationResult result = resolve(entity.level(), entity.getItem(), consecutiveBounces, source, entity.getRandom());
        if (result.outcome() != Outcome.CONTINUE) {
            if (result.outcome() == Outcome.DAMAGE && entity.level() instanceof net.minecraft.server.level.ServerLevel server) {
                ItemStack original = entity.getItem().copy();
                server.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        net.minecraft.sounds.SoundEvents.ITEM_BREAK, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8F, 1.0F);
                for (int i = 0; i < 15; i++)
                    server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, original),
                            entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 0,
                            (server.random.nextDouble() - .5) * .15, .05 + server.random.nextDouble() * .08,
                            (server.random.nextDouble() - .5) * .15, 1);
            }
            entity.setItem(result.stack());
            entity.removeData(com.minechai.create_sandustry.registry.ModAttachments.GRAVITY_BOUNCE);
            entity.hasImpulse = true;
        }
        return result.outcome();
    }

    /** Stack-based entry point for future launchers that keep items in an internal flight queue. */
    public static ActivationResult resolve(Level level, ItemStack input, int consecutiveBounces,
            GravityActivationSource source, RandomSource random) {
        Objects.requireNonNull(source, "source");
        if (level.isClientSide || !GravityMechanismAssembly.isAwaitingActivation(level, input))
            return new ActivationResult(Outcome.CONTINUE, input);

        Outcome outcome = evaluate(consecutiveBounces, GravityMechanismData.interruptions(input), random::nextDouble);
        ItemStack result;
        if (outcome == Outcome.SUCCESS) {
            result = GravityMechanismAssembly.complete(level, input);
            if (result.isEmpty())
                return new ActivationResult(Outcome.CONTINUE, input);
        } else if (outcome == Outcome.DAMAGE) {
            result = switch (random.nextInt(3)) {
                case 0 -> AllBlocks.COGWHEEL.asStack();
                case 1 -> new ItemStack(ModItems.MOONHOP_BOARD.get());
                default -> AllItems.GOLDEN_SHEET.asStack();
            };
        } else {
            return new ActivationResult(outcome, input);
        }

        result.remove(AllDataComponents.SEQUENCED_ASSEMBLY);
        result.remove(ModDataComponents.GRAVITY_TOTAL_BOUNCES.get());
        result.remove(ModDataComponents.GRAVITY_INTERRUPTION_COUNT.get());
        return new ActivationResult(outcome, result);
    }
}
