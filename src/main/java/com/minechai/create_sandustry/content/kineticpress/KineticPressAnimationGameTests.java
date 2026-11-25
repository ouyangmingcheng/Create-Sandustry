package com.minechai.create_sandustry.content.kineticpress;

import com.minechai.create_sandustry.CreateSandustry;
import com.minechai.create_sandustry.content.kineticpress.client.KineticPressAnimation;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class KineticPressAnimationGameTests {
    private static final double K = 7.0D;
    private static final double TARGET_Y = 1.0D;

    private KineticPressAnimationGameTests() {
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void exponentialMotionMatchesTimeAndHeight(GameTestHelper helper) {
        verifyHeightAndTime(helper, 3, 40);
        verifyHeightAndTime(helper, 8, 40);
        verifyHeightAndTime(helper, 16, 40);
        verifyHeightAndTime(helper, 8, 20);
        verifyHeightAndTime(helper, 8, 100);

        // Increasing k from the previous default 5 to 7 concentrates more travel late.
        double previousCurve = KineticPressAnimation.getProgress(20, 40, 5.0D);
        double newCurve = KineticPressAnimation.getProgress(20, 40, K);
        helper.assertTrue(newCurve < previousCurve,
                "Increasing k from 5 to 7 must make the midpoint slower");
        helper.assertTrue(newCurve < 0.5D, "Exponential curve must not be halfway at u=0.5");

        // The same normalized time must yield the same progress for any duration.
        help(helper, KineticPressAnimation.getProgress(10, 20, K),
                KineticPressAnimation.getProgress(50, 100, K), "Duration scaling must be normalized");

        // Never reach 1 early, but force exact terminal values at and after T.
        helper.assertTrue(KineticPressAnimation.getProgress(39.999D, 40, K) < 1.0D,
                "Animation reached the base before processingTime");
        helper.assertTrue(KineticPressAnimation.getProgress(40, 40, K) == 1.0D,
                "Animation must be exactly complete at processingTime");
        helper.assertTrue(KineticPressAnimation.getProgress(80, 40, K) == 1.0D,
                "Animation must stay exactly complete after processingTime");
        helper.succeed();
    }

    @GameTest(template = "empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void synchronizedInterpolationNeverMovesBackwards(GameTestHelper helper) {
        double lastRendered = Double.NEGATIVE_INFINITY;
        for (int serverProgress = 0; serverProgress < 10; serverProgress++) {
            KineticPressAnimationRenderState advancing = new KineticPressAnimationRenderState(
                    1, ItemStack.EMPTY, 40, 8, serverProgress, serverProgress + 1);
            for (int step = 0; step <= 20; step++) {
                double rendered = advancing.progressAt(step / 20.0F);
                helper.assertTrue(rendered >= lastRendered - 1.0E-9D,
                        "A synchronized interpolation tick must never move backwards");
                lastRendered = rendered;
            }

            KineticPressAnimationRenderState held = new KineticPressAnimationRenderState(
                    1, ItemStack.EMPTY, 40, 8, serverProgress + 1, serverProgress + 1);
            double nextTickStart = held.progressAt(0.0F);
            helper.assertTrue(nextTickStart >= lastRendered - 1.0E-9D,
                    "A repeated server snapshot must hold at the current endpoint");
            lastRendered = nextTickStart;
        }
        helper.succeed();
    }

    private static void verifyHeightAndTime(GameTestHelper helper, int height, int time) {
        double startY = TARGET_Y + height;
        helper.assertTrue(KineticPressAnimation.getProgress(0, time, K) == 0.0D,
                "Animation must start at progress 0");
        helper.assertTrue(KineticPressAnimation.getDistance(0, time, height, K) == 0.0D,
                "Animation must start with zero distance");

        for (int elapsed : new int[] {1, time / 4, time / 2, time * 3 / 4, time - 1}) {
            if (elapsed <= 0 || elapsed >= time) {
                continue;
            }
            double previous = KineticPressAnimation.getPosition(
                    startY, elapsed - 1, time, height, K);
            double current = KineticPressAnimation.getPosition(
                    startY, elapsed, time, height, K);
            helper.assertTrue(current < previous, "Animation must move downward monotonically");
            helper.assertTrue(current >= TARGET_Y, "Animation must not pass through the base early");
        }

        double terminalDistance = KineticPressAnimation.getDistance(time, time, height, K);
        double terminalPosition = KineticPressAnimation.getPosition(startY, time, time, height, K);
        helper.assertTrue(terminalDistance == height,
                "Terminal distance must equal workingHeight for height " + height);
        helper.assertTrue(terminalPosition == TARGET_Y,
                "Terminal position must equal exact target Y for height " + height);
    }

    private static void help(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 1.0E-9D, message);
    }
}