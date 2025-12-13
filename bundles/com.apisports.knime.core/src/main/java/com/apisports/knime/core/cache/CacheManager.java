package com.apisports.knime.core.cache;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Two-level cache manager with L1 (in-memory) and L2 (disk) caching.
 * Uses LRU eviction for memory cache and TTL-based expiration.
 */
public class CacheManager {
    
    private static final int DEFAULT_L1_SIZE = 1000;
    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final String CACHE_DIR = System.getProperty("user.home") + "/.apisports/cache";
    
    private final Map<String, CacheEntry> l1Cache;
    private final Duration ttl;
    private final Path cacheDirectory;
    private final int maxL1Size;

    public CacheManager() {
        this(DEFAULT_L1_SIZE, DEFAULT_TTL);
    }

    public CacheManager(int maxL1Size, Duration ttl) {
        this.maxL1Size = maxL1Size;
        this.ttl = ttl;
        this.l1Cache = new ConcurrentHashMap<>();
        this.cacheDirectory = Paths.get(CACHE_DIR);
        
        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            // Log warning but continue - disk cache will be unavailable
            System.err.println("Warning: Could not create cache directory: " + e.getMessage());
        }
    }

    /**
     * Get a cached value by key.
     * Checks L1 (memory) first, then L2 (disk).
     * 
     * @param key The cache key
     * @return The cached value, or null if not found or expired
     */
    public String get(String key) {
        // Check L1 cache
        CacheEntry entry = l1Cache.get(key);
        if (entry != null) {
            if (entry.isExpired()) {
                l1Cache.remove(key);
                return null;
            }
            return entry.value();
        }

        // Check L2 cache (disk)
        return readFromDisk(key);
    }

    /**
     * Put a value into the cache.
     * Stores in both L1 (memory) and L2 (disk).
     * 
     * @param key The cache key
     * @param value The value to cache
     */
    public void put(String key, String value) {
        Instant expiresAt = Instant.now().plus(ttl);
        CacheEntry entry = new CacheEntry(key, value, expiresAt);
        
        // Add to L1 cache with size limit
        if (l1Cache.size() >= maxL1Size) {
            // Simple eviction: remove oldest entry
            String oldestKey = l1Cache.keySet().stream().findFirst().orElse(null);
            if (oldestKey != null) {
                l1Cache.remove(oldestKey);
            }
        }
        l1Cache.put(key, entry);
        
        // Write to L2 cache
        writeToDisk(key, entry);
    }

    /**
     * Clear all cached data from memory and disk.
     */
    public void clear() {
        l1Cache.clear();
        try {
            Files.walk(cacheDirectory)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        // Ignore individual file deletion errors
                    }
                });
        } catch (IOException e) {
            // Ignore errors during cache clearing
        }
    }

    private String readFromDisk(String key) {
        Path filePath = getCacheFilePath(key);
        if (!Files.exists(filePath)) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(filePath))) {
            CacheEntry entry = (CacheEntry) ois.readObject();
            if (entry.isExpired()) {
                Files.delete(filePath);
                return null;
            }
            
            // Promote to L1 cache
            l1Cache.put(key, entry);
            return entry.value();
        } catch (IOException | ClassNotFoundException e) {
            // Cache read failed, return null
            return null;
        }
    }

    private void writeToDisk(String key, CacheEntry entry) {
        Path filePath = getCacheFilePath(key);
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(filePath))) {
            oos.writeObject(entry);
        } catch (IOException e) {
            // Disk cache write failed, but L1 cache still works
            System.err.println("Warning: Could not write to disk cache: " + e.getMessage());
        }
    }

    private Path getCacheFilePath(String key) {
        // Hash the key to create a valid filename
        String filename = String.valueOf(Math.abs(key.hashCode())) + ".cache";
        return cacheDirectory.resolve(filename);
    }
}
