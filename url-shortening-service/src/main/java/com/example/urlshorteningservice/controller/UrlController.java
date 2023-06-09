package com.example.urlshorteningservice.controller;

import com.example.urlshorteningservice.dto.RequestUrlDto;
import com.example.urlshorteningservice.dto.ResponseUrlDto;
import com.example.urlshorteningservice.model.Url;
import com.example.urlshorteningservice.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "URL Controller", description = "Endpoints for URL operations")
public class UrlController {

    private final UrlService urlService;
    private final ConversionService conversionService;

    @GetMapping("/{shortUrl}")
    @Operation(summary = "Get URL for Redirect", description = "Get the original URL for the provided short URL")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "301", description = "Moved Permanently", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(schema = @Schema()))
    })
    public RedirectView getUrlForRedirect(
            @Parameter(description = "Short URL", example = "d7dcca4")
            @PathVariable String shortUrl) {
        Optional<Url> originalUrl = urlService.getOriginalUrl(shortUrl);
        RedirectView redirectView;
        if (originalUrl.isPresent()) {
            urlService.increaseCounter(shortUrl);
            redirectView = new RedirectView(originalUrl.get().getOriginalUrl());
            redirectView.setStatusCode(HttpStatus.MOVED_PERMANENTLY);
        } else {
            redirectView = new RedirectView("/" + shortUrl);
            redirectView.setStatusCode(HttpStatus.NOT_FOUND);
        }
        return redirectView;
    }

    @PostMapping("/api/v1/url/create-short")
    @Operation(summary = "Get Short URL", description = "Generate a short URL for the provided long URL", security = { @SecurityRequirement(name = "bearer-jwt") })
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = ResponseUrlDto.class)))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema()))
    public ResponseEntity<ResponseUrlDto> createShortUrl(
            @AuthenticationPrincipal Jwt principal,
            @RequestBody() RequestUrlDto urlDto) {
        Url url = conversionService.convert(urlDto, Url.class);
        String userId = principal.getSubject();
        url.setUserId(userId);
        url = urlService.getShortUrl(url);
        return ResponseEntity.ok(conversionService.convert(url, ResponseUrlDto.class));
    }

    @GetMapping("/api/v1/user/urls")
    @Operation(summary = "Get All User URLs", description = "Get all URLs for a specific user", security = { @SecurityRequirement(name = "bearer-jwt") })
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(array = @ArraySchema(schema = @Schema(implementation = ResponseUrlDto.class))))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema()))
    public ResponseEntity<List<ResponseUrlDto>> getAllUserUrls(
            @AuthenticationPrincipal Jwt principal
    ) {
        String userId = principal.getSubject();
        List<Url> allUserUrls = urlService.getAllUserUrls(userId);
        List<ResponseUrlDto> urlsDto = allUserUrls.stream().map(x -> conversionService.convert(x, ResponseUrlDto.class)).toList();
        return ResponseEntity.ok(urlsDto);
    }

    @DeleteMapping("/api/v1/user/urls/{shortUrl}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Disable URL", description = "Disable a URL", security = { @SecurityRequirement(name = "bearer-jwt") })
    @ApiResponse(responseCode = "200", description = "Url disabled", content = @Content(schema = @Schema()))
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema()))
    public void disableUrl(
            @AuthenticationPrincipal Jwt principal,
            @Parameter(description = "ShorUrl", example = "d7dcca4")
            @PathVariable String shortUrl
    ) {
        String userId = principal.getSubject();
        urlService.disableUrl(shortUrl, userId);
    }
}

