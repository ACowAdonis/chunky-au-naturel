package org.popcraft.chunky.util;

import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegionCache {
    private final Map<String, WorldState> cache = new ConcurrentHashMap<>();

    public WorldState getWorld(final String world) {
        return cache.computeIfAbsent(world, x -> new WorldState());
    }

    public void clear(final String world) {
        cache.remove(world);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static final class WorldState {
        private final Map<Long, BitSet> regions = new ConcurrentHashMap<>();
        private final RegionCacheMetrics metrics = new RegionCacheMetrics();

        public void setGenerated(final int x, final int z) {
            final int regionX = x >> 5;
            final int regionZ = z >> 5;
            final long regionKey = ChunkMath.pack(regionX, regionZ);
            final BitSet region = regions.computeIfAbsent(regionKey, v -> new BitSet());
            final int chunkIndex = ChunkMath.regionIndex(x, z);

            if (RegionCacheMetrics.isEnabled()) {
                final long startTime = System.nanoTime();
                synchronized (region) {
                    region.set(chunkIndex);
                }
                metrics.recordWrite(System.nanoTime() - startTime);
            } else {
                synchronized (region) {
                    region.set(chunkIndex);
                }
            }
        }

        public boolean isGenerated(final int x, final int z) {
            final int regionX = x >> 5;
            final int regionZ = z >> 5;
            final long regionKey = ChunkMath.pack(regionX, regionZ);
            final BitSet region = regions.get(regionKey);
            if (region == null) {
                return false;
            }
            final int chunkIndex = ChunkMath.regionIndex(x, z);

            if (RegionCacheMetrics.isEnabled()) {
                final long startTime = System.nanoTime();
                final boolean result;
                synchronized (region) {
                    result = region.get(chunkIndex);
                }
                metrics.recordRead(System.nanoTime() - startTime);
                return result;
            } else {
                synchronized (region) {
                    return region.get(chunkIndex);
                }
            }
        }

        public RegionCacheMetrics getMetrics() {
            return metrics;
        }
    }
}
