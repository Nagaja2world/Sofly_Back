package com.sofly.supply.adapter.inbound.rest;

import com.sofly.supply.adapter.outbound.google.GooglePlacesClient;
import com.sofly.supply.application.dto.PlacesResponse;
import com.sofly.supply.application.dto.PlacesResponse.PhotoMedia;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Profile("!prod")
@Tag(name = "Google Places", description = "Google Places API 테스트 (비프로덕션 전용)")
@RestController
@RequestMapping("/supply/places")
public class GooglePlacesController {

    private final GooglePlacesClient googlePlacesClient;

    public GooglePlacesController(GooglePlacesClient googlePlacesClient) {
        this.googlePlacesClient = googlePlacesClient;
    }

    @Operation(summary = "장소 검색", description = "텍스트로 Google Places를 검색합니다.")
    @GetMapping
    public Optional<PlacesResponse> search(
            @Parameter(description = "검색 텍스트", example = "Lotte Hotel Seoul")
            @RequestParam String text
    ) {
        return googlePlacesClient.searchText(text);
    }

    @Operation(summary = "장소 사진 조회", description = "places:searchText 응답의 photos[].name 값을 넣으면 실제 이미지 URL(photoUri)을 반환합니다.")
    @GetMapping("/photo")
    public Optional<PhotoMedia> photo(
            @Parameter(description = "photo name (예: places/ChIJ.../photos/AXCi...)", example = "places/ChIJN1t_tDeuEmsRUsoyG83frY4/photos/AXCi2Q...")
            @RequestParam String name,
            @Parameter(description = "이미지 최대 너비 (px)", example = "800")
            @RequestParam(defaultValue = "800") int maxWidthPx
    ) {
        return googlePlacesClient.getPhotoMedia(name, maxWidthPx);
    }
}
