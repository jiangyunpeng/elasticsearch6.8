package org.elasticsearch.metric;

public interface MovingAverages {

    /**
     * Tick the internal clock of the MovingAverages implementation if needed
     * (according to the internal ticking interval)
     */
    void tickIfNecessary();

    /**
     * Update all three moving averages with n events having occurred since the last update.
     *
     */
    void update(long n);

    /**
     * Returns the one-minute moving average rate
     *
     * @return the one-minute moving average rate
     */
    double getM1Rate();

    /**
     * Returns the five-minute moving average rate
     *
     * @return the five-minute moving average rate
     */
    double getM5Rate();

    /**
     * Returns the fifteen-minute moving average rate
     *
     * @return the fifteen-minute moving average rate
     */
    double getM15Rate();
}
