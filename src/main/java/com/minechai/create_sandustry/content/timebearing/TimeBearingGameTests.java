package com.minechai.create_sandustry.content.timebearing;

import com.minechai.create_sandustry.CreateSandustry;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateSandustry.MODID)
@PrefixGameTestTemplate(false)
public final class TimeBearingGameTests {
    private TimeBearingGameTests() {
    }

    @GameTest(template = "time_bearing_empty", templateNamespace = CreateSandustry.MODID, timeoutTicks = 20)
    public static void clockworkBearingGate(GameTestHelper helper) {
        ClockworkBearingGate.State belowMinimum = state(32.0F, 499999.0F, 1.0D, 1.0D);
        helper.assertTrue(belowMinimum.mode() == ClockworkBearingGate.Mode.LOCKED,
                "容量低于 500000 SU 时必须保持时间锁定");

        ClockworkBearingGate.State fixed = state(32.0F, 500000.0F, 2.5D, 1.0D);
        helper.assertTrue(fixed.mode() == ClockworkBearingGate.Mode.FIXED_RATE,
                "500000 至 4000000 SU 应使用固定速率");
        assertClose(helper, 2.5D, fixed.dayTimePerTick(), "固定模式应使用配置速率");

        ClockworkBearingGate.State atHighThreshold = state(32.0F, 4_000_000.0F, 1.0D, 1.0D);
        helper.assertTrue(atHighThreshold.mode() == ClockworkBearingGate.Mode.FIXED_RATE,
                "容量恰好 4000000 SU 时仍应使用固定速率");

        ClockworkBearingGate.State rpmDriven = state(32.0F, 4_000_001.0F, 1.0D, 1.0D);
        helper.assertTrue(rpmDriven.mode() == ClockworkBearingGate.Mode.RPM_DRIVEN,
                "容量严格超过 4000000 SU 时应使用 RPM 驱动");
        assertClose(helper, 32.0D, rpmDriven.dayTimePerTick(), "32 RPM、倍率 1.0 应产生 32 tick/tick");

        ClockworkBearingGate.State reverse = state(-32.0F, 4_000_001.0F, 1.0D, 0.5D);
        assertClose(helper, 16.0D, reverse.dayTimePerTick(), "反向转速应采用绝对值并应用倍率");

        ClockworkBearingGate.State stopped = state(0.0F, 4_000_001.0F, 1.0D, 1.0D);
        helper.assertTrue(stopped.mode() == ClockworkBearingGate.Mode.LOCKED,
                "输入转速为零时必须保持时间锁定");

        ClockworkBearingGate.State stalled = ClockworkBearingGate.evaluate(
                true, true, 32.0F, 4_000_001.0F, 500000, 4_000_000, 1.0D, 1.0D);
        helper.assertTrue(stalled.mode() == ClockworkBearingGate.Mode.LOCKED,
                "结构停滞时必须保持时间锁定");

        helper.assertTrue(WorldTimeController.initialDayTimeTarget(12345L, 6000) == 6000L,
                "世界首次初始化应把当前日时间设为中午 6000");
        helper.assertTrue(WorldTimeController.initialDayTimeTarget(30000L, 6000) == 30000L,
                "初始正午应保留当前游戏日编号");

        helper.succeed();
    }

    private static ClockworkBearingGate.State state(
            float speed, float capacity, double fixedRate, double rpmMultiplier) {
        return ClockworkBearingGate.evaluate(
                true, false, speed, capacity, 500000, 4_000_000, fixedRate, rpmMultiplier);
    }

    private static void assertClose(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) <= 1.0E-9D,
                message + "：期望 " + expected + "，实际 " + actual);
    }
}
