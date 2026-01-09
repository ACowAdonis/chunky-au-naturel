package org.popcraft.chunky.util;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Tracks RegionCache performance metrics to measure lock contention.
 * Used to determine if StampedLock optimization (P3.1) is worthwhile.
 *
 * Currently ENABLED BY DEFAULT for testing. Will be disabled by default in production.
 */
public class RegionCacheMetrics {
    private static final boolean ENABLED = !Boolean.getBoolean("chunky.metrics.disabled");

    static {
        // Log status at startup so users know if profiling is active
        final String status = ENABLED ? "ENABLED" : "DISABLED";
        System.out.println("[Chunky] Performance profiling is " + status);
        if (ENABLED) {
            System.out.println("[Chunky] Lock contention metrics will be logged with each progress update");
        }
    }

    // Read operations
    private final LongAdder readOps = new LongAdder();
    private final LongAdder readLockWaitNanos = new LongAdder();

    // Write operations
    private final LongAdder writeOps = new LongAdder();
    private final LongAdder writeLockWaitNanos = new LongAdder();

    // Last reset time
    private final AtomicLong lastResetTime = new AtomicLong(System.currentTimeMillis());

    public void recordRead(long lockWaitNanos) {
        if (!ENABLED) return;
        readOps.increment();
        readLockWaitNanos.add(lockWaitNanos);
    }

    public void recordWrite(long lockWaitNanos) {
        if (!ENABLED) return;
        writeOps.increment();
        writeLockWaitNanos.add(lockWaitNanos);
    }

    public MetricsSnapshot getSnapshot() {
        final long reads = readOps.sum();
        final long writes = writeOps.sum();
        final long readWaitNanos = readLockWaitNanos.sum();
        final long writeWaitNanos = writeLockWaitNanos.sum();
        final long elapsedMs = System.currentTimeMillis() - lastResetTime.get();

        return new MetricsSnapshot(reads, writes, readWaitNanos, writeWaitNanos, elapsedMs);
    }

    public void reset() {
        readOps.reset();
        writeOps.reset();
        readLockWaitNanos.reset();
        writeLockWaitNanos.reset();
        lastResetTime.set(System.currentTimeMillis());
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static class MetricsSnapshot {
        public final long readOperations;
        public final long writeOperations;
        public final long readWaitNanos;
        public final long writeWaitNanos;
        public final long elapsedMs;

        MetricsSnapshot(long readOps, long writeOps, long readWaitNanos, long writeWaitNanos, long elapsedMs) {
            this.readOperations = readOps;
            this.writeOperations = writeOps;
            this.readWaitNanos = readWaitNanos;
            this.writeWaitNanos = writeWaitNanos;
            this.elapsedMs = elapsedMs;
        }

        public double getReadOpsPerSecond() {
            return elapsedMs > 0 ? (readOperations * 1000.0) / elapsedMs : 0;
        }

        public double getWriteOpsPerSecond() {
            return elapsedMs > 0 ? (writeOperations * 1000.0) / elapsedMs : 0;
        }

        public double getAvgReadWaitMicros() {
            return readOperations > 0 ? (readWaitNanos / 1000.0) / readOperations : 0;
        }

        public double getAvgWriteWaitMicros() {
            return writeOperations > 0 ? (writeWaitNanos / 1000.0) / writeOperations : 0;
        }

        public double getTotalWaitTimeSeconds() {
            return (readWaitNanos + writeWaitNanos) / 1_000_000_000.0;
        }

        public double getContentionPercentage() {
            if (elapsedMs == 0) return 0;
            final double totalWaitSeconds = getTotalWaitTimeSeconds();
            final double elapsedSeconds = elapsedMs / 1000.0;
            return (totalWaitSeconds / elapsedSeconds) * 100.0;
        }

        @Override
        public String toString() {
            return String.format(
                "\n=== CHUNKY PERFORMANCE METRICS (%.1fs elapsed) ===\n" +
                "  Reads:  %,d ops (%.1f/s, avg wait: %.2fµs)\n" +
                "  Writes: %,d ops (%.1f/s, avg wait: %.2fµs)\n" +
                "  Total lock wait time: %.3fs\n" +
                "  LOCK CONTENTION: %.1f%%\n" +
                "  Recommendation: %s\n" +
                "==========================================",
                elapsedMs / 1000.0,
                readOperations, getReadOpsPerSecond(), getAvgReadWaitMicros(),
                writeOperations, getWriteOpsPerSecond(), getAvgWriteWaitMicros(),
                getTotalWaitTimeSeconds(),
                getContentionPercentage(),
                getRecommendation()
            );
        }

        public String getRecommendation() {
            final double contention = getContentionPercentage();
            if (contention > 15) {
                return "HIGH contention - P3.1 (StampedLock) strongly recommended";
            } else if (contention > 10) {
                return "MODERATE contention - P3.1 (StampedLock) recommended";
            } else if (contention > 5) {
                return "LOW contention - P3.1 may provide minor benefit";
            } else {
                return "MINIMAL contention - P3.1 not needed, current optimizations sufficient";
            }
        }
    }
}
