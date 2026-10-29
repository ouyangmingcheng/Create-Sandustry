package com.minechai.create_sandustry.content.timebearing;

/**
 * 时钟轴承时间锁的纯判定逻辑。
 */
public final class ClockworkBearingGate {
    public enum Mode {
        LOCKED,
        FIXED_RATE,
        RPM_DRIVEN
    }

    public record State(Mode mode, double dayTimePerTick) {
        public boolean isUnlocked() {
            return this.mode != Mode.LOCKED;
        }
    }

    private static final State LOCKED = new State(Mode.LOCKED, 0.0D);

    private ClockworkBearingGate() {
    }

    public static State evaluate(
            boolean assembled,
            boolean stalled,
            float inputSpeed,
            float networkCapacity,
            int minimumNetworkCapacity,
            int rpmDrivenMinimumNetworkCapacity,
            double fixedDayTimePerTick,
            double rpmDayTimeMultiplier) {
        if (!assembled
                || stalled
                || !Float.isFinite(inputSpeed)
                || inputSpeed == 0.0F
                || !Float.isFinite(networkCapacity)
                || networkCapacity < Math.max(0, minimumNetworkCapacity)) {
            return LOCKED;
        }

        if (networkCapacity > Math.max(0, rpmDrivenMinimumNetworkCapacity)) {
            double rate = Math.abs((double) inputSpeed) * Math.max(0.0D, rpmDayTimeMultiplier);
            return Double.isFinite(rate) ? new State(Mode.RPM_DRIVEN, rate) : LOCKED;
        }

        double fixedRate = Math.max(0.0D, fixedDayTimePerTick);
        return Double.isFinite(fixedRate) ? new State(Mode.FIXED_RATE, fixedRate) : LOCKED;
    }

    public static boolean isUnlocked(
            boolean assembled,
            boolean stalled,
            float inputSpeed,
            float networkCapacity,
            int minimumNetworkCapacity) {
        return evaluate(
                assembled,
                stalled,
                inputSpeed,
                networkCapacity,
                minimumNetworkCapacity,
                Integer.MAX_VALUE,
                1.0D,
                1.0D).isUnlocked();
    }
}
