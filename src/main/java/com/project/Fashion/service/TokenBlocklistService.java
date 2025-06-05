package com.project.Fashion.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenBlocklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlocklistService.class);

    // Name of the cache for blocklisted tokens
    public static final String TOKEN_BLOCKLIST_CACHE_NAME = "tokenBlocklist";

    // Using a Caffeine cache directly for simplicity as it's already a dependency.
    // Alternatively, you could use Spring Cache with @CachePut and @Cacheable if you prefer declarative caching.
    private Cache<String, String> blocklistCache;

    @Value("${jwt.expiration.ms}") // To configure cache entry expiry similarly to token
    private long tokenExpirationMs;

    @PostConstruct
    public void init() {
        // Initialize the Caffeine cache
        // The cache will store JTI as key and a placeholder string (like "blocked") as value.
        // Entries will expire. We use a slightly longer expiry for cache entries
        // than the token's own expiry to handle clock skew or processing delays,
        // or simply use the token's remaining validity.
        // For this implementation, we set a max size and an expiry based on write time.
        // A more precise approach would be to calculate remaining validity for each token.
        blocklistCache = Caffeine.newBuilder()
                .maximumSize(10000) // Max number of tokens to store in blocklist
                .expireAfterWrite(tokenExpirationMs, TimeUnit.MILLISECONDS) // Entries expire after token's original lifespan
                .build();
        logger.info("Token blocklist cache initialized with expiry of {} ms.", tokenExpirationMs);
    }

    /**
     * Adds a token's JTI to the blocklist.
     * The entry will expire from the cache based on the token's original expiration.
     *
     * @param jti The JWT ID of the token to block.
     * @param expirationDate The original expiration date of the token.
     */
    public void blockToken(String jti, Date expirationDate) {
        if (jti == null || expirationDate == null) {
            logger.warn("Attempted to block token with null JTI or expiration date.");
            return;
        }

        long remainingValidity = expirationDate.getTime() - System.currentTimeMillis();

        if (remainingValidity > 0) {
            // We use a dedicated cache instance here, so we manually put with expiry.
            // If using Spring Cache, it would be more abstract.
            // For Caffeine, we can't set per-entry TTL directly on a single Cache instance
            // in this manner after build(). The `expireAfterWrite` on build() applies to all.
            // So, the blocklistCache.put will use the globally defined expiry.
            // This is a simplification. A more robust solution might use multiple caches
            // or a cache that supports per-entry TTL like Redis.
            // For Caffeine, the global expireAfterWrite is a reasonable approximation.
            blocklistCache.put(jti, "blocked");
            logger.info("Token JTI {} added to blocklist. It will be removed from cache around its original expiry.", jti);
        } else {
            logger.info("Token JTI {} is already expired. Not adding to blocklist.", jti);
        }
    }

    /**
     * Checks if a token's JTI is in the blocklist.
     *
     * @param jti The JWT ID to check.
     * @return true if the JTI is blocklisted, false otherwise.
     */
    public boolean isTokenBlocked(String jti) {
        if (jti == null) {
            return false; // Or throw an error, but for a check, false is safer.
        }
        // getIfPresent will return null if the key is not found or if it has expired.
        boolean isBlocked = blocklistCache.getIfPresent(jti) != null;
        if (isBlocked) {
            logger.debug("Token JTI {} found in blocklist.", jti);
        }
        return isBlocked;
    }
}
