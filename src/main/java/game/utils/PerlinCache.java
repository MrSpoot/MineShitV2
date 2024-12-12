package game.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PerlinCache {
    private final ConcurrentHashMap<Long, Float> cache;
    private final ConcurrentLinkedQueue<Long> order;
    private final FastNoiseLite noise;
    private final int maxCacheSize;

    public PerlinCache(long seed, int maxCacheSize) {
        this.noise = new FastNoiseLite();
        this.noise.SetSeed((int) seed);
        this.noise.SetNoiseType(FastNoiseLite.NoiseType.OpenSimplex2);
        this.noise.SetFrequency(0.006f);

        this.cache = new ConcurrentHashMap<>();
        this.order = new ConcurrentLinkedQueue<>();
        this.maxCacheSize = maxCacheSize;
    }

    public float getNoise(int x, int z) {
        long key = (((long) x) << 32) | (z & 0xFFFFFFFFL);

        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        float value = noise.GetNoise(x, z);
        cache.put(key, value);
        order.add(key);

        if (cache.size() > maxCacheSize) {
            Long oldestKey = order.poll();
            if (oldestKey != null) {
                cache.remove(oldestKey);
            }
        }

        return value;
    }

    public void clear() {
        cache.clear();
        order.clear();
    }
}



