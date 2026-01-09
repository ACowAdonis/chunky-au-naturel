# Chunky Profiling Guide

## Built-in Performance Profiling

Chunky includes built-in instrumentation to measure RegionCache lock contention. This helps determine if the StampedLock optimization (P3.1) is worthwhile for your hardware and workload.

## Enable Profiling

Add this JVM argument when starting your server:
```bash
-Dchunky.metrics.enabled=true
```

**Complete server start example:**
```bash
java -Xmx8G \
     -Dchunky.metrics.enabled=true \
     -Dchunky.maxWorkingCount=100 \
     -jar forge-server.jar
```

## Running Benchmark

### 1. Start Server with Profiling

```bash
# Start server with profiling AND high concurrency to stress-test locks
java -Xmx8G \
     -Dchunky.metrics.enabled=true \
     -Dchunky.maxWorkingCount=128 \
     -Dchunky.callbackThreads=8 \
     -jar forge-server.jar
```

### 2. Run Chunk Generation

In-game or console:
```
/chunky world <world_name>
/chunky radius 5000
/chunky start
```

**Recommendation:** Run for at least 5-10 minutes to get meaningful statistics.

### 3. Monitor Output

Chunky will automatically log performance metrics with each progress update (default: every 5 seconds).

### 4. Analyze Results

Look for the metrics output in your console/logs:

```
RegionCache Metrics (15.0s elapsed):
  Reads:  45,234 ops (3,015.6/s, avg wait: 2.35µs)
  Writes: 12,456 ops (830.4/s, avg wait: 3.12µs)
  Total lock wait time: 0.125s (0.8% contention)
  Recommendation: MINIMAL contention - P3.1 not needed, current optimizations sufficient
```

## Interpreting Results

### Contention Percentage

This is the key metric. It represents how much time threads spend waiting for locks relative to wall-clock time.

**Formula:** `(total_lock_wait_time / elapsed_time) × 100`

### Thresholds

| Contention | Recommendation | Action |
|-----------|----------------|--------|
| **> 15%** | HIGH - P3.1 strongly recommended | StampedLock will provide 15-25% improvement |
| **10-15%** | MODERATE - P3.1 recommended | StampedLock will provide 10-15% improvement |
| **5-10%** | LOW - P3.1 may help | Minor benefit, evaluate risk vs reward |
| **< 5%** | MINIMAL - P3.1 not needed | Current optimizations sufficient |

### Average Wait Times

- **< 1µs**: Excellent - minimal lock contention
- **1-5µs**: Good - some contention but not severe
- **5-20µs**: Moderate - locks are contested
- **> 20µs**: High - significant lock contention

### Operations Per Second

Higher is better. Typical values:
- **Reads**: 1,000-10,000/s (depends on chunk iterator speed)
- **Writes**: 100-1,000/s (one per completed chunk)

Low values may indicate:
- Slow world generation (bottleneck is elsewhere)
- Disk I/O bottleneck
- CPU saturation

## Example Scenarios

### Scenario 1: Low-End Server
```
RegionCache Metrics (30.0s elapsed):
  Reads:  125,000 ops (4,166/s, avg wait: 0.8µs)
  Writes: 30,000 ops (1,000/s, avg wait: 1.2µs)
  Total lock wait time: 0.135s (0.5% contention)
  Recommendation: MINIMAL contention - P3.1 not needed
```

**Analysis:** Contention is minimal (0.5%). The bottleneck is likely world generation CPU, not locks. **Phase 1 optimizations are sufficient.**

### Scenario 2: High-Concurrency Server
```
RegionCache Metrics (30.0s elapsed):
  Reads:  850,000 ops (28,333/s, avg wait: 12.5µs)
  Writes: 45,000 ops (1,500/s, avg wait: 18.3µs)
  Total lock wait time: 11.2s (37.3% contention)
  Recommendation: HIGH contention - P3.1 strongly recommended
```

**Analysis:** Contention is severe (37.3%). With maxWorkingCount=150+, threads are spending more time waiting for locks than doing work. **StampedLock (P3.1) will provide significant improvement (20-40%).**

### Scenario 3: Moderate Case
```
RegionCache Metrics (30.0s elapsed):
  Reads:  480,000 ops (16,000/s, avg wait: 6.2µs)
  Writes: 38,000 ops (1,267/s, avg wait: 9.1µs)
  Total lock wait time: 3.4s (11.3% contention)
  Recommendation: MODERATE contention - P3.1 recommended
```

**Analysis:** Moderate contention (11.3%). StampedLock would help but not transformative. **Evaluate if 10-15% improvement is worth implementation risk.**

## Profiling Without Server

If you can't run a full server, you can test locally:

### 1. Single-Player World

Start Minecraft with JVM arguments:
```bash
java -Xmx4G \
     -Dchunky.metrics.enabled=true \
     -Dchunky.maxWorkingCount=64 \
     -jar forge-installer.jar
```

Generate chunks in single-player. Metrics will appear in logs:
```
%APPDATA%\.minecraft\logs\latest.log     (Windows)
~/.minecraft/logs/latest.log             (Linux/Mac)
```

### 2. Local Test Server

Run a lightweight local Forge server:
```bash
java -Xmx4G -Dchunky.metrics.enabled=true -jar forge-server.jar nogui
```

## Troubleshooting

### No Metrics Output

**Check:** Is `-Dchunky.metrics.enabled=true` actually set?

**Verify:**
```bash
# In-game or console:
/chunky help

# Look for version output, then check server startup logs
```

### Metrics Show 0% Contention Always

**Possible causes:**
1. `maxWorkingCount` too low (not enough concurrency to stress locks)
2. World generation is extremely slow (chunks/sec very low)
3. Profiling overhead disabled optimizations

**Solution:** Increase concurrency to stress-test:
```bash
-Dchunky.maxWorkingCount=200
-Dchunky.callbackThreads=12
```

### High Contention (>50%)

**This is good for testing!** It means locks ARE the bottleneck. This clearly demonstrates P3.1 would help.

However, for production use, this level may indicate:
- `maxWorkingCount` set too high for hardware
- Memory pressure causing thrashing

**For profiling purposes:** High contention is useful data. It confirms locks are the issue.

## Performance Impact of Profiling

**Overhead:** ~2-5% due to `System.nanoTime()` calls

**When to disable:**
- Production servers after profiling complete
- If seeing performance degradation

**How to disable:**
- Remove `-Dchunky.metrics.enabled=true` from JVM args
- Profiling code is compiled away when disabled (minimal runtime cost)

## Reporting Results

When reporting profile results or requesting P3.1 implementation, include:

1. **Hardware specs:**
   - CPU: cores, model
   - RAM: total, allocated to Java
   - Storage: SSD or HDD

2. **Configuration:**
   - `chunky.maxWorkingCount` value
   - `chunky.callbackThreads` value
   - Modpack (vanilla, light, heavy)

3. **Full metrics output:**
   - Copy the complete metrics log (all fields)
   - Duration of profiling run
   - Approximate chunks/second rate

4. **Conclusion:**
   - Your interpretation of contention percentage
   - Request for P3.1 if contention >10%

## Next Steps

### If Contention < 10%
You're done! Phase 1 optimizations (P1.1-P1.6) + EventBus thread-safety (P2.4) + concurrency tuning (P3.3) are sufficient. No further optimization needed.

### If Contention > 10%
Proceed with P3.1 (StampedLock implementation). Expected improvement correlates with contention:
- 10% contention → ~10-15% faster
- 20% contention → ~15-20% faster
- 30%+ contention → ~20-30% faster

P3.1 is a moderate-risk change requiring careful testing, but profiling data justifies the effort.
