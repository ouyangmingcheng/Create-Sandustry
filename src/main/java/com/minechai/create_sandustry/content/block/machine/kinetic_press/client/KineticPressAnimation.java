package com.minechai.create_sandustry.content.block.machine.kinetic_press.client;

import com.minechai.create_sandustry.config.ModConfigs;

/**
 * Pure exponential fall motion shared by rendering and deterministic tests.
 * The terminal conditions are explicit so exp() rounding can never prevent the
 * item from reaching the base.
 */
public final class KineticPressAnimation {
    private KineticPressAnimation() {
    }

    public static double getProgress(double elapsedTicks, double processingTime) {
        return getProgress(elapsedTicks, processingTime,
                ModConfigs.KINETIC_PRESS_ANIMATION_EXPONENT.get());
    }

    public static double getProgress(double elapsedTicks, double processingTime, double exponent) {
        if (processingTime <= 0.0D || elapsedTicks >= processingTime) {
            return 1.0D;
        }
        if (elapsedTicks <= 0.0D) {
            return 0.0D;
        }

        double u = Math.clamp(elapsedTicks / processingTime, 0.0D, 1.0D);
        if (u >= 1.0D) {
            return 1.0D;
        }
        if (exponent <= 1.0E-9D) {
            return u;
        }

        double denominator = Math.expm1(exponent);
        if (denominator <= 0.0D) {
            return u;
        }
        return Math.clamp(Math.expm1(exponent * u) / denominator, 0.0D, 1.0D);
    }

    public static double getDistance(double elapsedTicks, double processingTime, double workingHeight) {
        return getDistance(elapsedTicks, processingTime, workingHeight,
                ModConfigs.KINETIC_PRESS_ANIMATION_EXPONENT.get());
    }

    public static double getDistance(double elapsedTicks, double processingTime, double workingHeight,
                                    double exponent) {
        return workingHeight * getProgress(elapsedTicks, processingTime, exponent);
    }

    public static double getPosition(double startY, double elapsedTicks, double processingTime,
                                     double workingHeight) {
        return getPosition(startY, elapsedTicks, processingTime, workingHeight,
                ModConfigs.KINETIC_PRESS_ANIMATION_EXPONENT.get());
    }

    public static double getPosition(double startY, double elapsedTicks, double processingTime,
                                     double workingHeight, double exponent) {
        if (processingTime <= 0.0D || elapsedTicks >= processingTime) {
            return startY - workingHeight;
        }
        return startY - getDistance(elapsedTicks, processingTime, workingHeight, exponent);
    }
}