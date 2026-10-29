package com.minechai.create_sandustry.content.timebearing;

/**
 * 保存昼夜时间的小数余量，避免每 tick 截断导致累计误差。
 */
final class DayTimeAccumulator {
    private double pendingTicks;

    void add(double ticks) {
        if (!Double.isFinite(ticks) || ticks < 0.0D) {
            return;
        }
        this.pendingTicks += ticks;
        if (!Double.isFinite(this.pendingTicks) || this.pendingTicks < 0.0D) {
            this.pendingTicks = 0.0D;
        }
    }

    long consumeWholeTicks() {
        long wholeTicks = (long) Math.floor(this.pendingTicks);
        if (wholeTicks <= 0L) {
            if (!Double.isFinite(this.pendingTicks) || this.pendingTicks < 0.0D) {
                this.pendingTicks = 0.0D;
            }
            return 0L;
        }

        this.pendingTicks -= wholeTicks;
        if (!Double.isFinite(this.pendingTicks) || this.pendingTicks < 0.0D) {
            this.pendingTicks = 0.0D;
        }
        return wholeTicks;
    }

    double pendingTicks() {
        return this.pendingTicks;
    }

    void clear() {
        this.pendingTicks = 0.0D;
    }
}
