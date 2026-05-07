package com.urlshortener.service;

import com.urlshortener.config.RedisConfig;
import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.exception.AliasAlreadyExistsException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlInactiveException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.repository.UrlMappingRepository;
import com.urlshortener.util.Base62Encoder;
import com.urlshortener.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UrlService {

    // ── Dependencies ──────────────────────────────────────────────
    // Spring injects these via constructor
    // @RequiredArgsConstructor generates the constructor
    // for all final fields automatically
    private final UrlMappingRepository repository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Base62Encoder base62Encoder;

    // ── Config from application.yml ───────────────────────────────
    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.default-expiry-days:30}")
    private int defaultExpiryDays;


    // ════════════════════════════════════════════════════════════════
    //  CREATE — Shorten a URL
    // ════════════════════════════════════════════════════════════════

    @Transactional
    public ShortenResponse shortenUrl(ShortenRequest request) {
        log.info("Shortening URL: {} alias={}",
            request.originalUrl(), request.customAlias());

        // ── Step 1: Handle custom alias or generate short code ────
        String shortCode;
        if (request.customAlias() != null
                && !request.customAlias().isBlank()) {

            // User wants a vanity alias
            // Check if it is already taken
            if (repository.existsByShortCode(
                    request.customAlias())) {
                throw new AliasAlreadyExistsException(
                    request.customAlias());
            }
            shortCode = request.customAlias();

        } else {
            // Auto generate from Snowflake ID
            // Snowflake → 64-bit long → Base62 → 7 chars
            long snowflakeId = snowflakeIdGenerator.nextId();
            shortCode = base62Encoder.encode(snowflakeId);
            log.debug("Generated shortCode={} from id={}",
                shortCode, snowflakeId);
        }

        // ── Step 2: Calculate expiry date ─────────────────────────
        LocalDateTime expiresAt = null;
        int expiryDays = (request.expiryDays() != null)
            ? request.expiryDays()
            : defaultExpiryDays;

        if (expiryDays > 0) {
            expiresAt = LocalDateTime.now()
                .plusDays(expiryDays);
        }
        // expiryDays == 0 means never expires
        // expiresAt stays null

        // ── Step 3: Build and save entity ─────────────────────────
        long entityId = snowflakeIdGenerator.nextId();

        UrlMapping mapping = UrlMapping.builder()
            .id(entityId)
            .shortCode(shortCode)
            .originalUrl(request.originalUrl())
            .customAlias(request.customAlias())
            .expiresAt(expiresAt)
            .createdBy(request.createdBy())
            .isActive(true)
            .clickCount(0L)
            .build();

        UrlMapping saved = repository.save(mapping);
        log.info("Saved shortCode={} → {}",
            shortCode, request.originalUrl());

        // ── Step 4: Return response DTO ───────────────────────────
        return ShortenResponse.from(saved, baseUrl);
    }


    // ════════════════════════════════════════════════════════════════
    //  RESOLVE — Find original URL from short code
    // ════════════════════════════════════════════════════════════════

    // @Cacheable checks Redis before running method body
    // Cache HIT  → returns from Redis immediately
    // Cache MISS → runs method, stores result in Redis
    // key = "urls::aB3xZ9k"
@Cacheable(
    value = RedisConfig.CACHE_URLS,
    key = "#shortCode",
    condition = "!#shortCode.isEmpty()",
    cacheManager = "cacheManager"
)
@Transactional(readOnly = true)
public UrlMapping resolveUrl(String shortCode) {
    log.debug("Cache MISS — querying DB for {}",
        shortCode);

    UrlMapping mapping = repository
        .findByShortCode(shortCode)
        .orElseThrow(() ->
            new UrlNotFoundException(shortCode));

    if (!mapping.getIsActive()) {
        throw new UrlInactiveException(shortCode);
    }
    if (mapping.isExpired()) {
        throw new UrlExpiredException(shortCode);
    }

    return mapping;
}


    // ════════════════════════════════════════════════════════════════
    //  RECORD CLICK — Increment counter after redirect
    // ════════════════════════════════════════════════════════════════

    // @CacheEvict removes stale cached entry after update
    // Next resolveUrl() call re-fetches fresh data from DB
    @Transactional
    public void recordClick(String shortCode) {
        try {
            repository.incrementClickCount(shortCode);
        } catch (Exception ex) {
            // Click counting is non-critical
            // Never let it break the redirect flow
            log.error("Failed to increment click for {}: {}",
                shortCode, ex.getMessage());
        }
    }


    // ════════════════════════════════════════════════════════════════
    //  GET INFO — Get metadata without redirecting
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public ShortenResponse getUrlInfo(String shortCode) {
        UrlMapping mapping = resolveUrl(shortCode);
        return ShortenResponse.from(mapping, baseUrl);
    }


    // ════════════════════════════════════════════════════════════════
    //  DEACTIVATE — Soft delete a short URL
    // ════════════════════════════════════════════════════════════════

    @Transactional
    @CacheEvict(
        value = RedisConfig.CACHE_URLS,
        key = "#shortCode"
    )
    public void deactivateUrl(String shortCode) {
        UrlMapping mapping = repository
            .findByShortCode(shortCode)
            .orElseThrow(() ->
                new UrlNotFoundException(shortCode));

        mapping.setIsActive(false);
        repository.save(mapping);

        log.info("Deactivated shortCode={}", shortCode);
    }


    // ════════════════════════════════════════════════════════════════
    //  LIST — Get all URLs by user
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<ShortenResponse> getUrlsByUser(
            String createdBy) {
        return repository
            .findByCreatedByOrderByCreatedAtDesc(createdBy)
            .stream()
            .map(m -> ShortenResponse.from(m, baseUrl))
            .toList();
    }


    // ════════════════════════════════════════════════════════════════
    //  SCHEDULED CLEANUP — Runs every hour automatically
    // ════════════════════════════════════════════════════════════════

    // cron = "0 0 * * * *" means:
    // second=0, minute=0, every hour, every day
    // Runs at 1:00, 2:00, 3:00... automatically
    // No HTTP call needed — Spring triggers it internally
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredUrls() {
        log.info("Running expired URL cleanup...");

        // ONE bulk UPDATE — not a loop
        // Handles thousands of expired URLs in one statement
        int count = repository.deactivateExpiredUrls(
            LocalDateTime.now());

        if (count > 0) {
            log.info("Deactivated {} expired URL(s)", count);
        }
    }


    // ════════════════════════════════════════════════════════════════
    //  STATS — Platform statistics
    // ════════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Map<String, Object> getStats() {
        long totalUrls = repository.count();
        long urlsToday = repository
            .countActiveUrlsCreatedAfter(
                LocalDateTime.now().minusDays(1));
        List<UrlMapping> topUrls = repository
            .findTopByClickCount(5);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUrls", totalUrls);
        stats.put("urlsLast24h", urlsToday);
        stats.put("topUrls", topUrls.stream()
            .map(m -> Map.of(
                "shortCode", m.getShortCode(),
                "clickCount", m.getClickCount()
            )).toList());

        return stats;
    }
    @Cacheable(
    value = "urls",
    key = "#shortCode",
    unless = "#result == null"
)
@Transactional(readOnly = true)
public String findLongUrlByShortCode(String shortCode) {
    return repository.findByShortCode(shortCode)
        .map(UrlMapping::getOriginalUrl)
        .orElse(null);
}
}