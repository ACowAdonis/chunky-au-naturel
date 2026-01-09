# Chunky Performance Tuning Guide

## Overview

Chunky now supports hardware-aware concurrency tuning through system properties. These settings allow optimization for different server configurations and workloads.

## System Properties

### Chunk Generation Concurrency

**`chunky.maxWorkingCount`**
- **Description**: Maximum concurrent chunk operations
- **Default**: `max(50, availableProcessors × 8)`
- **Range**: 1-500 recommended
- **Memory Impact**: High - each concurrent chunk consumes ~5-10MB
- **Example**: `-Dchunky.maxWorkingCount=100`

**When to adjust:**
- **Increase** on high-memory servers (32GB+) with fast CPUs
- **Decrease** if experiencing OutOfMemoryError or high GC pressure
- **Decrease** on shared hosting or memory-constrained environments

### Callback Thread Pool

**`chunky.callbackThreads`**
- **Description**: Threads for async chunk completion callbacks
- **Default**: `max(4, availableProcessors / 2)`
- **Range**: 2-32 recommended
- **CPU Impact**: Moderate
- **Example**: `-Dchunky.callbackThreads=8`

**When to adjust:**
- **Increase** on high-core-count CPUs (16+ cores)
- **Decrease** if CPU usage is too high or affecting game TPS

### Task Scheduler Thread Pool

**`chunky.schedulerCoreThreads`**
- **Description**: Core threads for task scheduling
- **Default**: `max(3, availableProcessors / 2)`
- **Range**: 1-16 recommended
- **Example**: `-Dchunky.schedulerCoreThreads=6`

**`chunky.schedulerMaxThreads`**
- **Description**: Maximum threads for task scheduling
- **Default**: `max(10, availableProcessors × 2)`
- **Range**: coreThreads to 64 recommended
- **Example**: `-Dchunky.schedulerMaxThreads=20`

**`chunky.schedulerQueueSize`**
- **Description**: Task queue capacity before blocking
- **Default**: 200
- **Range**: 50-1000 recommended
- **Example**: `-Dchunky.schedulerQueueSize=500`

## Hardware-Specific Recommendations

### Low-End Server (4 cores, 8GB RAM)
```
-Dchunky.maxWorkingCount=32
-Dchunky.callbackThreads=2
-Dchunky.schedulerCoreThreads=2
-Dchunky.schedulerMaxThreads=6
```

### Medium Server (8 cores, 16GB RAM)
```
# Use defaults, or:
-Dchunky.maxWorkingCount=64
-Dchunky.callbackThreads=4
```

### High-End Server (16+ cores, 32GB+ RAM)
```
-Dchunky.maxWorkingCount=128
-Dchunky.callbackThreads=8
-Dchunky.schedulerCoreThreads=8
-Dchunky.schedulerMaxThreads=32
```

### Shared Hosting (Variable resources)
```
# Conservative settings to avoid resource abuse
-Dchunky.maxWorkingCount=25
-Dchunky.callbackThreads=2
-Dchunky.schedulerCoreThreads=2
-Dchunky.schedulerMaxThreads=4
```

## Monitoring and Tuning

### Signs You Need to Adjust

**Too High Concurrency (decrease settings):**
- OutOfMemoryError during chunk generation
- Excessive GC pauses (>100ms frequently)
- Server TPS drops below 15 during generation
- High swap usage

**Too Low Concurrency (increase settings):**
- Low CPU utilization (<50%) during generation
- Slow chunk generation rate (chunks/sec)
- Memory headroom available (>50% free)

### Monitoring Commands

**Check generation performance:**
```
/chunky status
```
Look for "chunks/sec" rate.

**Monitor JVM memory:**
```
# Add to JVM args for detailed GC logging:
-Xlog:gc*:file=gc.log:time,uptime:filecount=5,filesize=10M
```

**Profile with JFR (Java Flight Recorder):**
```
# Start recording:
jcmd <pid> JFR.start name=chunky-profile settings=profile duration=60s filename=chunky-profile.jfr

# After recording completes, analyze with:
# - JDK Mission Control (GUI)
# - jfr print chunky-profile.jfr
```

## Advanced Tuning

### C2ME Integration

When using C2ME (Concurrent Chunk Management Engine):
- C2ME and Chunky both use thread pools
- Reduce `chunky.maxWorkingCount` by 20-30% to avoid contention
- Monitor for lock contention in profiles

### World Generation Complexity

**Vanilla/Light Modpacks:**
- Can use higher `maxWorkingCount` (CPU becomes bottleneck)
- Generation is relatively fast per-chunk

**Heavy Modpacks (Terralith, etc.):**
- Use lower `maxWorkingCount` (generation is slower, memory-intensive)
- Increase `callbackThreads` to handle async operations

### SSD vs HDD

**SSD Storage:**
- Higher `maxWorkingCount` works well (fast I/O)
- Less concern about parallel chunk saves

**HDD Storage:**
- Moderate `maxWorkingCount` (I/O becomes bottleneck)
- Too many concurrent chunks cause disk thrashing

## Troubleshooting

### OutOfMemoryError
```
Solution: Decrease chunky.maxWorkingCount by 50%
Alternative: Increase heap size (-Xmx) if possible
```

### Slow Generation Despite High Resources
```
Check: Is world gen the bottleneck? (Profile with JFR)
Check: Is maxWorkingCount actually being used? (Thread dump)
Try: Increase maxWorkingCount by 25%
```

### High CPU Usage Affecting TPS
```
Solution: Decrease callbackThreads and schedulerMaxThreads
Alternative: Run generation during off-peak hours
```

### Lock Contention (Advanced)
```
Profile: Use JFR to identify lock hotspots
Expected: Some contention on RegionCache is normal
Action: If >15% time in locks, report issue with profile
```

## Default Calculation Logic

For reference, defaults are calculated as:

```java
// Chunk operations (I/O bound, high memory)
maxWorkingCount = max(50, availableProcessors × 8)

// Callback threads (CPU bound, moderate memory)
callbackThreads = max(4, availableProcessors / 2)

// Scheduler core threads (background tasks)
schedulerCoreThreads = max(3, availableProcessors / 2)

// Scheduler max threads (burst capacity)
schedulerMaxThreads = max(10, availableProcessors × 2)
```

These provide sensible defaults for most servers while allowing override for edge cases.

## Version History

- **1.3.2**: Added hardware-aware defaults and all tuning properties
- **1.3.0**: Initial `chunky.maxWorkingCount` support (fixed at 50 default)
