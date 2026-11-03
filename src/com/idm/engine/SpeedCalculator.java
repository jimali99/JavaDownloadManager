package com.idm.engine;

import java.util.LinkedList;

public class SpeedCalculator {
    private static class Sample {
        final long timestamp;
        final long bytes;

        Sample(long timestamp, long bytes) {
            this.timestamp = timestamp;
            this.bytes = bytes;
        }
    }

    private final LinkedList<Sample> samples = new LinkedList<>();
    private final int windowMs;
    private long lastReportedSpeed = 0;

    public SpeedCalculator() {
        this(2000); // 2 second rolling window
    }

    public SpeedCalculator(int windowMs) {
        this.windowMs = windowMs;
    }

    public synchronized void recordSample(long currentTotalBytes) {
        long now = System.currentTimeMillis();
        samples.addLast(new Sample(now, currentTotalBytes));

        // Purge samples older than windowMs
        while (!samples.isEmpty() && (now - samples.peekFirst().timestamp) > windowMs) {
            samples.removeFirst();
        }
    }

    public synchronized double getCurrentSpeed() {
        if (samples.size() < 2) {
            return lastReportedSpeed;
        }
        Sample oldest = samples.peekFirst();
        Sample newest = samples.peekLast();
        long timeDiff = newest.timestamp - oldest.timestamp;
        if (timeDiff <= 50) {
            return lastReportedSpeed;
        }
        long bytesDiff = newest.bytes - oldest.bytes;
        if (bytesDiff < 0) {
            bytesDiff = 0;
        }
        double speed = (bytesDiff * 1000.0) / timeDiff;
        // Smooth exponential moving average
        lastReportedSpeed = (long) (0.7 * speed + 0.3 * lastReportedSpeed);
        return lastReportedSpeed;
    }

    public synchronized void reset() {
        samples.clear();
        lastReportedSpeed = 0;
    }
}
