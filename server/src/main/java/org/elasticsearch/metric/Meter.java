package org.elasticsearch.metric;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class Meter {

    private final MovingAverages movingAverages;
    private final LongAdder count = new LongAdder();
    private final long startTime;
    private final Clock clock;

    public Meter() {
        this(new ExponentialMovingAverages(), Clock.defaultClock());
    }

    public Meter(MovingAverages movingAverages, Clock clock) {
        this.movingAverages = movingAverages;
        this.clock = clock;
        this.startTime = this.clock.getTick();
    }

    public void mark() {
        mark(1);
    }

    public void mark(long n) {
        movingAverages.tickIfNecessary();
        count.add(n);
        movingAverages.update(n);
    }

    public long getCount() {
        return count.sum();
    }

    public double getMeanRate() {
        if (getCount() == 0) {
            return 0.0;
        } else {
            final double elapsed = clock.getTick() - startTime;
            return getCount() / elapsed * TimeUnit.SECONDS.toNanos(1);
        }
    }

    public double getOneMinuteRate() {
        movingAverages.tickIfNecessary();
        return movingAverages.getM1Rate();
    }

    public double getOneMinuteRateRoundx1() {
        return roundx1(getOneMinuteRate());
    }

    private static double roundx1(double value) {
        return Math.round(value * 10) / 10.0;
    }

}
