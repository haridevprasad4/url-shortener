package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

// What the client sends to create a short URL
@Schema(description = "Request payload to create a short URL")
public record ShortenRequest(

        // ── Original URL ──────────────────────────────────────────
        // @NotBlank = must not be null AND not empty/whitespace
        // @Size     = maximum 2048 characters
        // @Pattern  = must start with http:// or https://
        @NotBlank(message = "Original URL is required")
        @Size(
            max = 2048,
            message = "URL must not exceed 2048 characters"
        )
        @Pattern(
            regexp = "^https?://.*",
            message = "URL must start with http:// or https://"
        )
        @Schema(
            description = "The long URL to shorten",
            example = "https://www.example.com/very/long/path"
        )
        String originalUrl,

        // ── Custom Alias ──────────────────────────────────────────
        // Optional — user can request a vanity URL
        // null = auto generate from Snowflake ID
        // "my-blog" = use this as the short code
        //
        // @Size    = between 3 and 50 characters
        // @Pattern = only letters, numbers, and hyphens
        //            no spaces, no special characters
        @Size(
            min = 3,
            max = 50,
            message = "Alias must be between 3 and 50 characters"
        )
        @Pattern(
            regexp = "^[a-zA-Z0-9-]*$",
            message = "Alias can only contain letters, " +
                      "numbers, and hyphens"
        )
        @Schema(
            description = "Optional custom alias",
            example = "my-blog",
            nullable = true
        )
        String customAlias,

        // ── Expiry Days ───────────────────────────────────────────
        // How many days until this short URL expires
        // null  = use server default from application.yml (30 days)
        // 0     = never expires
        // 30    = expires in 30 days
        //
        // @Min = minimum value is 0
        // @Max = maximum value is 3650 (10 years)
        @Min(
            value = 0,
            message = "Expiry days must be 0 or more"
        )
        @Max(
            value = 3650,
            message = "Expiry days cannot exceed 3650"
        )
        @Schema(
            description = "Days until expiry. 0 = never expires",
            example = "30",
            nullable = true
        )
        Integer expiryDays,

        // ── Created By ────────────────────────────────────────────
        // Who is creating this URL
        // Optional — for anonymous users this is null
        // In a real system this comes from JWT token
        // not from the request body
        @Size(max = 100)
        @Schema(
            description = "Creator identifier",
            nullable = true
        )
        String createdBy

) {}