package org.elasticsearch.metric;

import java.util.concurrent.atomic.LongAdder;

public class Counter {
    private final LongAdder count;

    public Counter() {
        this.count = new LongAdder();
    }

    /**
     * Increment the counter by one.
     */
    public void inc() {
        inc(1);
    }

    /**
     * Increment the counter by {@code n}.
     *
     */
    public void inc(long n) {
        count.add(n);
    }

    /**
     * Decrement the counter by one.
     */
    public void dec() {
        dec(1);
    }

    /**
     * Decrement the counter by {@code n}.
     *
     */
    public void dec(long n) {
        count.add(-n);
    }

    /**
     * Returns the counter's current value.
     *
     */
    public long getCount() {
        return count.sum();
    }
}
