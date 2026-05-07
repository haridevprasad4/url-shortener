package com.urlshortener.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.urlshortener.entity.UrlMapping;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * What the server sends back after creating a short URL
 * or when fetching URL info
 *
 * Example JSON response:
 * {
 *   "shortCode": "aB3xZ9k",
 *   "shortUrl":  "http://localhost:8080/r/aB3xZ9k",
 *   "originalUrl": "https://example.com/long/path",
 *   "clickCount": 42,
 *   "isActive": true,
 *   "createdAt": "2025-04-01T12:30:00"
 * }
 *
 * @JsonInclude(NON_NULL)
 * Fields that are null are completely removed from JSON
 * If expiresAt is null it will not appear in the response at all
 * Cleaner JSON for API consumers
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response with shortened URL details")
public record ShortenResponse(

        @Schema(description = "The 7-character short code",
                example = "aB3xZ9k")
        String shortCode,

        @Schema(description = "Full short URL ready to share",
                example = "http://localhost:8080/r/aB3xZ9k")
        String shortUrl,

        @Schema(description = "The original long URL")
        String originalUrl,

        @Schema(description = "Number of times accessed")
        Long clickCount,

        @Schema(description = "Whether this URL is active")
        Boolean isActive,

        // @JsonFormat controls how LocalDateTime appears in JSON
        // Without this Jackson outputs an ugly array:
        // [2025, 4, 1, 12, 30, 0]
        // With this it outputs a clean string:
        // "2025-04-01T12:30:00"
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Expiry datetime, null = never")
        LocalDateTime expiresAt,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "When this mapping was created")
        LocalDateTime createdAt,

        @Schema(description = "Creator identifier",
                nullable = true)
        String createdBy

) {
        /**
         * Static factory method
         * Converts a UrlMapping entity into this response DTO
         *
         * WHY a static factory method?
         * Cleaner than calling the constructor directly
         * One place where entity to DTO conversion happens
         * If entity fields change, you fix it here only
         *
         * Usage:
         * ShortenResponse.from(entity, "http://localhost:8080")
         */
        public static ShortenResponse from(
                        UrlMapping entity,
                        String baseUrl) {
                return new ShortenResponse(
                        entity.getShortCode(),
                        baseUrl + "/r/" + entity.getShortCode(),
                        entity.getOriginalUrl(),
                        entity.getClickCount(),
                        entity.getIsActive(),
                        entity.getExpiresAt(),
                        entity.getCreatedAt(),
                        entity.getCreatedBy()
                );
        }
}