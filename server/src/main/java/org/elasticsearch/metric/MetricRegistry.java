package org.elasticsearch.metric;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author bairen
 **/
public class MetricRegistry {

    private static Map<String, Measure> metricMap = new ConcurrentHashMap<>();

    private static ScheduledThreadPoolExecutor schedule;

    static {
        schedule = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("stat-rolling"));
    }

    private static long startTime = System.currentTimeMillis();

    public static long startTime() {
        return startTime;
    }

    public static void reset() {
        metricMap.clear();
        startTime = System.currentTimeMillis();
    }

    public static Measure measure(String name) {
        //默认周期是60
        return metricMap.computeIfAbsent(name, (k) -> new Measure(name,60));
    }

    private static void addStatRollingTask(Measure measure) {
        scheduleNextRollingTask(measure);
        schedule.setMaximumPoolSize(Math.max(1, metricMap.size()));
    }

    private static void scheduleNextRollingTask(Measure measure) {
        StatLogRollingTask rollingTask = new StatLogRollingTask(measure);
        long now = System.currentTimeMillis();
        long rollingTs = measure.getRollingData().getRollingTs();
        long delayMillis = (rollingTs - now);
        if (delayMillis < 0) {
            schedule.submit(rollingTask);
        } else {
            schedule.schedule(rollingTask, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    private static class StatLogRollingTask implements Runnable {

        private Measure measure;

        public StatLogRollingTask(Measure measure) {
            super();
            this.measure = measure;
        }

        @Override
        public void run() {
            Tsdb.writeRollingData(measure.rolling());
            scheduleNextRollingTask(measure);
        }
    }

    /**
     * 表示不同的维度
     */
    public static class Measure {

        private AtomicReference<StatRollingData> ref = new AtomicReference<StatRollingData>();

        private String name;

        //限制MetricEntry的大小
        private int maxSize = -1;

        private int intervalSec;

        public Measure(String name,int intervalSec) {
            this.name = name;
            this.intervalSec =intervalSec;
            rolling();
            addStatRollingTask(this);
        }

        public String getName() {
            return name;
        }
        /**
         * 返回一个监控项
         */
        public MetricEntry entry(String entry) {
            return ref.get().entry(entry);
        }

        /**
         * 返回所有监控项
         */
        public List<MetricEntry> entries() {
            return entries(null);
        }

        /**
         * 返回所有监控项并且排序
         */
        public List<MetricEntry> entries(Comparator<MetricEntry> comparator) {
            return ref.get().entries(comparator);
        }

        /**
         * 清空所有监控项
         */
        public void clear() {
            ref.get().clear();
        }

        /**
         * 限制group最大监控项
         */
        public Measure maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        StatRollingData getRollingData() {
            return ref.get();
        }

        public StatRollingData rolling() {
            long intervalMillis = TimeUnit.SECONDS.toMillis(intervalSec);
            do {

                long now = System.currentTimeMillis();
                //获取当前时间的起点
                long timeSlot = now - now % intervalMillis;
                StatRollingData pre = ref.get();
                //获取下一次滚动时间
                long rollingTs = timeSlot + intervalMillis;

                StatRollingData next = new StatRollingData(this, timeSlot, rollingTs);
                if (ref.compareAndSet(pre, next)) {
                    return pre;
                }
            } while (true);
        }

    }

    /**
     * 表示可滚动的stat数据
     */
    static class StatRollingData {
        private Measure measure;
        private long timeSlot;
        private long rollingTs;//下次滚动时间
        private Map<String, MetricEntry> entryStatsMap = new ConcurrentHashMap<>();

        public StatRollingData(Measure measure, long timeSlot, long rollingTs) {
            this.measure = measure;
            this.timeSlot = timeSlot;
            this.rollingTs = rollingTs;
        }

        public MetricEntry entry(String entry) {
            if (measure.maxSize > 0) {
                synchronized (this) {
                    if (entryStatsMap.size() >= measure.maxSize) {
                        entryStatsMap.clear();
                    }
                }
            }
            return entryStatsMap.computeIfAbsent(entry, (k) -> new MetricEntry(entry));
        }

        public Measure getMeasure() {
            return measure;
        }

        public List<MetricEntry> entries(Comparator<MetricEntry> comparator) {
            List<MetricEntry> list = new ArrayList<>(entryStatsMap.values());
            if (comparator != null)
                Collections.sort(list, comparator);
            return list;
        }

        public void clear() {
            entryStatsMap.clear();
        }

        public long getTimeSlot() {
            return timeSlot;
        }

        public long getRollingTs() {
            return rollingTs;
        }
    }

    /**
     * 表示监控项
     */
    public static class MetricEntry {
        private String name;
        private Map<String, Object> fieldMap = new ConcurrentHashMap<>();
        private Map<String, Counter> counterMap = new ConcurrentHashMap<>();
        private Map<String, Meter> meterMap = new ConcurrentHashMap<>();

        public MetricEntry(String name) {
            this.name = name;
        }

        public Counter counter(String counter) {
            return counterMap.computeIfAbsent(counter, (k) -> new Counter());
        }

        public Meter meter(String meter) {
            return meterMap.computeIfAbsent(meter, (k) -> new Meter());
        }

        public List<String> getAllMeterName() {
            return meterMap.keySet().stream().sorted().collect(Collectors.toList());
        }

        public List<String> getAllCountName() {
            return counterMap.keySet().stream().sorted().collect(Collectors.toList());
        }

        public String getName() {
            return name;
        }

        /**
         * 直接覆盖旧值
         */
        public MetricEntry field(String key, Object value) {
            fieldMap.put(key, value);
            return this;
        }
    }

}
