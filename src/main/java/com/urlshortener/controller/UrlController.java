package com.urlshortener.controller;

import com.urlshortener.dto.ShortenRequest;
import com.urlshortener.dto.ShortenResponse;
import com.urlshortener.entity.UrlMapping;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(
    name = "URL Shortener",
    description = "API for creating and managing short URLs"
)
public class UrlController {

    private final UrlService urlService;

    // ════════════════════════════════════════════════════════════════
    //  POST /api/v1/urls — Create a new short URL
    // ════════════════════════════════════════════════════════════════

    // @Valid triggers Bean Validation on ShortenRequest
    // If any @NotBlank @Pattern @Size fails →
    // Spring throws MethodArgumentNotValidException →
    // GlobalExceptionHandler catches it → 422 response
    // Controller never sees invalid data
    @Operation(
        summary = "Create a short URL",
        description = "Accepts a long URL and returns " +
                      "a 7-character short code"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Short URL created successfully"),
        @ApiResponse(
            responseCode = "409",
            description = "Custom alias already in use"),
        @ApiResponse(
            responseCode = "422",
            description = "Validation failed")
    })
    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortenResponse> createShortUrl(
            @Valid @RequestBody ShortenRequest request) {

        log.info("POST /api/v1/urls originalUrl={}",
            request.originalUrl());

        ShortenResponse response =
            urlService.shortenUrl(request);

        // 201 Created
        // Location header points to the new resource
        return ResponseEntity
            .created(URI.create(
                "/api/v1/urls/" + response.shortCode()))
            .body(response);
    }


    // ════════════════════════════════════════════════════════════════
    //  GET /r/{shortCode} — THE REDIRECT ENDPOINT
    //  Most called endpoint in the entire system
    //  Must be as fast as possible
    //  Served from Redis cache in < 1ms
    // ════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Redirect to original URL",
        description = "Resolves short code and returns " +
                      "302 redirect. Increments click count."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "302",
            description = "Redirect to original URL"),
        @ApiResponse(
            responseCode = "404",
            description = "Short code not found"),
        @ApiResponse(
            responseCode = "410",
            description = "URL expired or deactivated")
    })
   @GetMapping("/r/{shortCode}")
public ResponseEntity<Void> redirect(
        @PathVariable String shortCode) {

    log.debug("GET /r/{}", shortCode);

    // Call findLongUrlByShortCode through the injected
    // service proxy so @Cacheable is triggered correctly
    String longUrl =
        urlService.findLongUrlByShortCode(shortCode);

    if (longUrl == null) {
        return ResponseEntity.notFound().build();
    }

    // Record click after getting URL
    urlService.recordClick(shortCode);

    return ResponseEntity
        .status(HttpStatus.FOUND)
        .location(URI.create(longUrl))
        .build();
}


    // ════════════════════════════════════════════════════════════════
    //  GET /api/v1/urls/{shortCode} — Get metadata
    //  Returns info WITHOUT redirecting
    //  Used by dashboards and link preview cards
    // ════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Get URL metadata",
        description = "Returns info about a short URL " +
                      "without redirecting"
    )
    @GetMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<ShortenResponse> getUrlInfo(
            @PathVariable String shortCode) {

        ShortenResponse info =
            urlService.getUrlInfo(shortCode);

        return ResponseEntity.ok(info);
    }


    // ════════════════════════════════════════════════════════════════
    //  GET /api/v1/urls/user/{username} — List user URLs
    // ════════════════════════════════════════════════════════════════

    @Operation(
        summary = "List URLs by user",
        description = "Returns all short URLs created " +
                      "by a specific user"
    )
    @GetMapping("/api/v1/urls/user/{username}")
    public ResponseEntity<List<ShortenResponse>> getUserUrls(
            @PathVariable String username) {

        List<ShortenResponse> urls =
            urlService.getUrlsByUser(username);

        return ResponseEntity.ok(urls);
    }


    // ════════════════════════════════════════════════════════════════
    //  DELETE /api/v1/urls/{shortCode} — Deactivate a URL
    // ════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Deactivate a short URL",
        description = "Marks URL as inactive. " +
                      "Future redirects return 410."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "URL deactivated successfully"),
        @ApiResponse(
            responseCode = "404",
            description = "Short code not found")
    })
    @DeleteMapping("/api/v1/urls/{shortCode}")
    public ResponseEntity<Void> deactivateUrl(
            @PathVariable String shortCode) {

        log.info("DELETE /api/v1/urls/{}", shortCode);
        urlService.deactivateUrl(shortCode);

        // 204 No Content
        // Deactivated successfully, nothing to return
        return ResponseEntity.noContent().build();
    }


    // ════════════════════════════════════════════════════════════════
    //  GET /api/v1/stats — Platform statistics
    // ════════════════════════════════════════════════════════════════

    @Operation(
        summary = "Get platform statistics",
        description = "Returns total URLs, recent " +
                      "activity, and top links"
    )
    @GetMapping("/api/v1/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(urlService.getStats());
    }


    // ════════════════════════════════════════════════════════════════
    //  GET /api/v1/health — Simple liveness check
    // ════════════════════════════════════════════════════════════════

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status",  "UP",
            "service", "url-shortener",
            "version", "1.0.0"
        ));
    }
}