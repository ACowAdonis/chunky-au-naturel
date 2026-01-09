# Chunky Performance Optimization Plan

Branch: `optimization/performance-improvements`

## Prioritization Methodology

Each optimization is scored on three dimensions:
- **Difficulty**: 1 (trivial) to 5 (complex)
- **Risk**: 1 (safe) to 5 (breaking change potential)
- **Gain**: 1 (minor) to 5 (major impact)

**Priority Score** = (Gain × 2) - Difficulty - Risk
Higher scores = implement first

---

## Phase 1: Quick Wins (Score ≥ 5)

### P1.1: Fix ChunkCoordinate.hashCode() [Score: 7]
**File**: `common/src/main/java/org/popcraft/chunky/util/ChunkCoordinate.java:24-26`
- **Difficulty**: 1 (one-line change)
- **Risk**: 1 (well-tested pattern)
- **Gain**: 4 (eliminates boxing/allocation in hot path)

**Current**:
```java
return Objects.hash(x, z);
```

**Replace with**:
```java
return (x * 31) + z;
```

**Testing**: Verify HashMap/HashSet behavior unchanged, run existing unit tests.

---

### P1.2: Fix Formatting.number() synchronization [Score: 6]
**File**: `common/src/main/java/org/popcraft/chunky/util/Formatting.java:39`
- **Difficulty**: 2 (ThreadLocal pattern)
- **Risk**: 1 (isolated change)
- **Gain**: 4 (removes global lock)

**Current**:
```java
public static synchronized String number(final double number) {
    return NUMBER_FORMAT.format(number);
}
```

**Replace with**:
```java
private static final ThreadLocal<DecimalFormat> NUMBER_FORMAT =
    ThreadLocal.withInitial(() -> {
        final DecimalFormat format = new DecimalFormat("#,###.##");
        format.setRoundingMode(RoundingMode.FLOOR);
        return format;
    });

public static String number(final double number) {
    return NUMBER_FORMAT.get().format(number);
}
```

**Testing**: Verify formatting output identical, test concurrent access.

---

### P1.3: Optimize Circle.isBounding() [Score: 6]
**File**: `common/src/main/java/org/popcraft/chunky/shape/Circle.java:13`
- **Difficulty**: 1 (eliminate sqrt)
- **Risk**: 1 (mathematically equivalent)
- **Gain**: 3 (faster for circular regions)

**Current**:
```java
return Math.hypot(centerX - x, centerZ - z) <= radiusX;
```

**Replace with**:
```java
final double dx = centerX - x;
final double dz = centerZ - z;
return (dx * dx + dz * dz) <= (radiusX * radiusX);
```

**Testing**: Verify boundary accuracy, test edge cases.

---

### P1.4: Fix BukkitWorld.getDirectory() [Score: 6]
**File**: `bukkit/src/main/java/org/popcraft/chunky/platform/BukkitWorld.java:156-167`
- **Difficulty**: 1 (direct path resolution)
- **Risk**: 2 (platform-specific, verify paths exist)
- **Gain**: 4 (eliminates tree walk on init)

**Current**:
```java
public Optional<Path> getDirectory(final String name) {
    if (name != null) {
        try (Stream<Path> paths = Files.walk(world.getWorldFolder().toPath())) {
            return paths.filter(Files::isDirectory)
                    .filter(path -> name.equals(path.getFileName().toString()))
                    .findFirst();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    return Optional.empty();
}
```

**Replace with**:
```java
public Optional<Path> getDirectory(final String name) {
    if (name == null) {
        return Optional.empty();
    }
    final Path dir = world.getWorldFolder().toPath().resolve(name);
    return Files.isDirectory(dir) ? Optional.of(dir) : Optional.empty();
}
```

**Testing**: Verify all world directory types (region, poi, entities) are found correctly.

---

### P1.5: ChunkCoordinate.toString() optimization [Score: 5]
**File**: `common/src/main/java/org/popcraft/chunky/util/ChunkCoordinate.java:30`
- **Difficulty**: 1 (simple concatenation)
- **Risk**: 1 (cosmetic change)
- **Gain**: 2 (minor, but called frequently in logging)

**Current**:
```java
return String.format("%d, %d", x, z);
```

**Replace with**:
```java
return x + ", " + z;
```

**Testing**: Verify string output identical.

---

### P1.6: Fix redundant containsKey checks [Score: 5]
**Files**:
- `common/src/main/java/org/popcraft/chunky/util/RegionCache.java:37`
- `common/src/main/java/org/popcraft/chunky/event/EventBus.java`

- **Difficulty**: 1 (pattern change)
- **Risk**: 1 (idiomatic Java)
- **Gain**: 2 (eliminates double lookup)

**Pattern**:
```java
// Instead of:
if (!map.containsKey(key)) return false;
final Value v = map.get(key);

// Use:
final Value v = map.get(key);
if (v == null) return false;
```

**Testing**: Verify null-safety, existing behavior preserved.

---

## Phase 2: Moderate Complexity (Score 3-4)

### P2.1: Optimize Pentagon/Star shape checks [Score: 4]
**Files**:
- `common/src/main/java/org/popcraft/chunky/shape/Pentagon.java:40-46`
- `common/src/main/java/org/popcraft/chunky/shape/Star.java:31-56`

- **Difficulty**: 2 (add bounding box, reorder checks)
- **Risk**: 2 (must preserve exact boundary behavior)
- **Gain**: 4 (reduces geometric calculations in main loop)

**Implementation**:
1. Add AABB (axis-aligned bounding box) pre-check
2. Early-exit on first failing line check
3. Precompute trigonometric constants

**Example for Pentagon**:
```java
// Add fields:
private final double minX, maxX, minZ, maxZ;

// Constructor computes AABB from points

public boolean isBounding(final double x, final double z) {
    // Fast rejection
    if (x < minX || x > maxX || z < minZ || z > maxZ) {
        return false;
    }

    // Ordered checks with early exit
    return insideLine(p1x, p1z, p2x, p2z, x, z)
        && insideLine(p2x, p2z, p3x, p3z, x, z)
        && insideLine(p3x, p3z, p4x, p4z, x, z)
        && insideLine(p4x, p4z, p5x, p5z, x, z)
        && insideLine(p5x, p5z, p1x, p1z, x, z);
}
```

**Testing**: Extensive boundary testing, verify chunk inclusion matches original.

---

### P2.2: Cache shape points() [Score: 4]
**Files**: All shape classes (Rectangle, Pentagon, Star, etc.)
- **Difficulty**: 2 (cache in constructor)
- **Risk**: 1 (safe, return unmodifiable)
- **Gain**: 2 (eliminates allocation per call)

**Implementation**:
```java
private final List<Vector2> cachedPoints;

// In constructor:
this.cachedPoints = Collections.unmodifiableList(Arrays.asList(
    Vector2.of(p1x, p1z),
    Vector2.of(p2x, p2z),
    // ...
));

public List<Vector2> points() {
    return cachedPoints;
}
```

**Testing**: Verify immutability, check callers don't modify list.

---

### P2.3: Precompute shape trigonometric constants [Score: 4]
**Files**: Pentagon.java, Star.java constructors
- **Difficulty**: 2 (define constants)
- **Risk**: 1 (precision verification)
- **Gain**: 2 (eliminates repeated trig calculations)

**Implementation**:
```java
// Pentagon angles: 54°, 126°, 198°, 270°, 342°
private static final double COS_54  =  0.5877852522924731;
private static final double SIN_54  =  0.8090169943749474;
private static final double COS_126 = -0.5877852522924731;
// ... etc
```

**Testing**: Verify precision matches Math.cos/sin to acceptable tolerance.

---

### P2.4: Fix EventBus thread-safety [Score: 3]
**File**: `common/src/main/java/org/popcraft/chunky/event/EventBus.java`
- **Difficulty**: 2 (replace HashMap, simplify invocation)
- **Risk**: 3 (event system changes, test all events)
- **Gain**: 3 (thread-safe, faster dispatch)

**Current issues**:
- Non-thread-safe HashMap
- MethodHandle invocation overhead
- Redundant exception wrapping

**Replace with**:
```java
private final Map<Class<?>, Set<Consumer<?>>> subscribers = new ConcurrentHashMap<>();

public void call(final Object event) {
    final Class<?> eventClass = event.getClass();
    final Set<Consumer<?>> eventSubscribers = subscribers.get(eventClass);
    if (eventSubscribers == null) {
        return;
    }
    for (Consumer<?> subscriber : eventSubscribers) {
        try {
            @SuppressWarnings("unchecked")
            final Consumer<Object> typedSubscriber = (Consumer<Object>) subscriber;
            typedSubscriber.accept(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

**Testing**: Verify all event types fire correctly, test concurrent subscription.

---

### P2.5: Add initial capacity hints [Score: 3]
**Files**: Multiple (TaskLoader.java:85, etc.)
- **Difficulty**: 1 (add capacity parameter)
- **Risk**: 1 (harmless if wrong)
- **Gain**: 1 (reduces array resizing)

**Pattern**:
```java
// Instead of:
new ArrayList<>()

// Use estimated capacity:
new ArrayList<>(expectedSize)
```

**Testing**: Verify no regressions.

---

## Phase 3: Complex Changes (Score 1-3)

### P3.1: RegionCache synchronization redesign [Score: 3]
**File**: `common/src/main/java/org/popcraft/chunky/util/RegionCache.java`
- **Difficulty**: 4 (requires concurrency expertise)
- **Risk**: 4 (critical path, thorough testing needed)
- **Gain**: 5 (removes major bottleneck)

**Options**:

**Option A: StampedLock** (Recommended)
```java
private final Map<Long, BitSetWithLock> regions = new ConcurrentHashMap<>();

private static class BitSetWithLock {
    private final BitSet bitSet = new BitSet();
    private final StampedLock lock = new StampedLock();
}

public boolean isGenerated(final int x, final int z) {
    final BitSetWithLock region = regions.get(regionKey);
    if (region == null) return false;

    long stamp = region.lock.tryOptimisticRead();
    boolean result = region.bitSet.get(chunkIndex);
    if (!region.lock.validate(stamp)) {
        stamp = region.lock.readLock();
        try {
            result = region.bitSet.get(chunkIndex);
        } finally {
            region.lock.unlockRead(stamp);
        }
    }
    return result;
}
```

**Option B: AtomicBitSet** (More complex)
- Custom implementation using AtomicLongArray
- Lock-free but requires careful implementation

**Testing**: Stress test with MAX_WORKING_COUNT concurrent operations, verify no lost updates.

---

### P3.2: Reduce CompletableFuture allocation [Score: 2]
**File**: `common/src/main/java/org/popcraft/chunky/GenerationTask.java:156-180`
- **Difficulty**: 4 (significant refactoring)
- **Risk**: 4 (async coordination logic)
- **Gain**: 4 (millions of allocations eliminated)

**Approach**:
1. Reuse completed future constants
2. Flatten composition chains
3. Consider custom FuturePool

**Implementation** (simplified):
```java
private static final CompletableFuture<Boolean> FALSE_FUTURE =
    CompletableFuture.completedFuture(false);
private static final CompletableFuture<Void> NULL_FUTURE =
    CompletableFuture.completedFuture(null);

// Flatten the chain:
final CompletableFuture<Boolean> isChunkGenerated = forceLoadExistingChunks ?
    FALSE_FUTURE : selection.world().isChunkGenerated(chunk.x(), chunk.z());
```

**Testing**: Verify async behavior identical, test timeout handling.

---

### P3.3: Tune concurrency parameters [Score: 2]
**Files**:
- `GenerationTask.java` (MAX_WORKING_COUNT, callback executor)
- `TaskScheduler.java` (thread pool sizing)

- **Difficulty**: 3 (requires benchmarking)
- **Risk**: 3 (wrong values degrade performance)
- **Gain**: 3 (better hardware utilization)

**Implementation**:
```java
// Make configurable:
private static final int MAX_WORKING_COUNT =
    Integer.getInteger("chunky.maxWorking",
        Math.max(50, Runtime.getRuntime().availableProcessors() * 10));

// Scale callback executor:
private static final int CALLBACK_THREADS =
    Integer.getInteger("chunky.callbackThreads",
        Math.max(4, Runtime.getRuntime().availableProcessors() / 2));
```

**Testing**: Benchmark with different values, monitor memory usage.

---

### P3.4: Optimize iterator allocation [Score: 1]
**Files**: All chunk iterators
- **Difficulty**: 5 (requires architectural change or Valhalla)
- **Risk**: 3 (iterator API changes)
- **Gain**: 3 (reduces GC pressure)

**Options**:
1. Primitive-specialized iterators (complex)
2. Reusable ChunkCoordinate holder (API breaking)
3. Wait for Project Valhalla value types

**Recommendation**: Defer until other optimizations complete.

---

## Phase 4: Monitoring & Validation

### M1: Add JMH Benchmarks
- **Difficulty**: 3
- **Risk**: 1
- **Files**: Create `benchmarks/` module

**Benchmarks to implement**:
1. RegionCache concurrent access
2. Shape boundary checking
3. ChunkCoordinate hash performance
4. CompletableFuture allocation patterns

### M2: Profiling Configuration
Add JFR profile configurations to track:
- Lock contention (RegionCache)
- Allocation rates (CompletableFuture, ChunkCoordinate)
- Hot methods (shape checks, formatting)

### M3: Performance Test Suite
Create repeatable performance tests:
- 10k chunk generation (small)
- 1M chunk generation (medium)
- 10M chunk generation (large)
- Various shapes (circle, rectangle, pentagon, star)

---

## Implementation Strategy

### Week 1: Phase 1 (Quick Wins)
All P1.x items - low risk, measurable gains, build momentum.

**Target**: 10-15% coordination overhead reduction

### Week 2: Phase 2 (Moderate)
P2.1 (shapes) and P2.4 (EventBus) - higher impact items first.

**Target**: Additional 5-10% improvement

### Week 3: Phase 3 (Complex)
P3.1 (RegionCache) - requires dedicated focus and extensive testing.

**Target**: 15-25% improvement for concurrent workloads

### Week 4: Validation
Benchmarking, profiling, integration testing with C2ME.

**Target**: Document overall gains, identify remaining bottlenecks

---

## Risk Mitigation

1. **Feature flag risky changes**: Add system properties to toggle optimizations
2. **Extensive testing**: Run existing test suite after each change
3. **Benchmarking**: Measure before/after for each optimization
4. **Gradual rollout**: Merge in phases, not as single massive PR
5. **Rollback plan**: Each optimization in separate commit for easy revert

---

## Success Metrics

### Primary:
- **Chunks/second throughput** (measure with large pre-generation task)
- **CPU utilization** (should remain high, not blocked on locks)
- **Memory pressure** (GC frequency and pause times)

### Secondary:
- Lock contention metrics (JFR)
- Object allocation rates (JFR)
- Thread pool saturation

### Target:
20-40% reduction in Chunky coordination overhead (not total generation time, as that's dominated by Minecraft). For a 10M chunk pre-generation that takes 10 hours, target 30-60 minutes improvement.

---

## Notes

- Most gains come from reducing lock contention (RegionCache) and allocation overhead (CompletableFuture, ChunkCoordinate)
- Shape optimizations matter most for Pentagon/Star patterns
- Conservative estimates assume Chunky overhead is ~20% of total time; actual gains depend on workload
- C2ME interaction: These optimizations are complementary and shouldn't conflict
