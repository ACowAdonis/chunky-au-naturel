# Optimization Priority Summary

## Quick Reference

| ID | Description | Difficulty | Risk | Gain | Score | Phase |
|----|-------------|------------|------|------|-------|-------|
| P1.1 | ChunkCoordinate.hashCode() | 1 | 1 | 4 | **7** | 1 |
| P1.2 | Formatting.number() ThreadLocal | 2 | 1 | 4 | **6** | 1 |
| P1.3 | Circle.isBounding() sqrt | 1 | 1 | 3 | **6** | 1 |
| P1.4 | BukkitWorld.getDirectory() | 1 | 2 | 4 | **6** | 1 |
| P1.5 | ChunkCoordinate.toString() | 1 | 1 | 2 | **5** | 1 |
| P1.6 | Redundant containsKey | 1 | 1 | 2 | **5** | 1 |
| P2.1 | Pentagon/Star shape checks | 2 | 2 | 4 | **4** | 2 |
| P2.2 | Cache shape points() | 2 | 1 | 2 | **4** | 2 |
| P2.3 | Precompute trig constants | 2 | 1 | 2 | **4** | 2 |
| P2.4 | EventBus thread-safety | 2 | 3 | 3 | **3** | 2 |
| P2.5 | Initial capacity hints | 1 | 1 | 1 | **3** | 2 |
| P3.1 | RegionCache synchronization | 4 | 4 | 5 | **3** | 3 |
| P3.2 | CompletableFuture allocation | 4 | 4 | 4 | **2** | 3 |
| P3.3 | Tune concurrency params | 3 | 3 | 3 | **2** | 3 |
| P3.4 | Iterator allocation | 5 | 3 | 3 | **1** | 4 |

**Score = (Gain × 2) - Difficulty - Risk**

## Implementation Sequence

### Phase 1: Quick Wins (1-2 days)
Low-hanging fruit with high confidence and measurable impact.
- 6 optimizations
- Minimal risk
- Expected: 10-15% coordination overhead reduction

### Phase 2: Moderate Complexity (2-3 days)
Requires more careful implementation but still straightforward.
- 5 optimizations
- Moderate risk, good test coverage needed
- Expected: Additional 5-10% improvement

### Phase 3: Complex Changes (5-7 days)
High-impact but requires significant refactoring and testing.
- 4 optimizations
- High risk, extensive validation required
- Expected: 15-25% improvement for concurrent workloads

### Phase 4: Validation & Monitoring (2-3 days)
Benchmarking, profiling, integration testing.

## Critical Path

The highest-impact changes in order:

1. **RegionCache** (P3.1) - Major bottleneck but complex
2. **CompletableFuture** (P3.2) - Millions of allocations
3. **ChunkCoordinate** (P1.1) - Hash table hot path
4. **Shape checks** (P2.1) - Main generation loop
5. **Formatting lock** (P1.2) - Global contention

## Risk Assessment

### Low Risk (Safe to implement immediately)
- P1.1, P1.3, P1.5, P1.6, P2.2, P2.3, P2.5

### Medium Risk (Requires testing)
- P1.2, P1.4, P2.1

### High Risk (Extensive validation needed)
- P2.4, P3.1, P3.2, P3.3

### Defer
- P3.4 (await Project Valhalla or deeper architectural changes)

## Expected Overall Impact

**Conservative estimate**: 20-30% reduction in Chunky coordination overhead

**Optimistic estimate**: 30-40% reduction with good concurrency tuning

**Real-world impact**: For typical large pre-generation (10M chunks):
- Current: ~10 hours total (Minecraft: 8h, Chunky: 2h)
- Post-optimization: ~9.5 hours (Minecraft: 8h, Chunky: 1.5h)
- **Net savings: 30 minutes on 10-hour job**

Note: Bottleneck remains in Minecraft's world generation. These optimizations make Chunky's coordination layer negligible overhead rather than measurable friction.

## Measurement Plan

### Before Optimization
1. Run baseline benchmarks (10k, 1M chunks)
2. Collect JFR profiles identifying contention
3. Measure allocation rates

### After Each Phase
1. Re-run benchmarks
2. Compare profiles
3. Document improvements
4. Adjust priorities based on findings

### Final Validation
1. Extended test with C2ME integration
2. Memory pressure analysis
3. Multi-world concurrent generation test
4. Various shape patterns benchmark
