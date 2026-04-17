package com.sofly.core.domain.place.controller;

import com.sofly.core.domain.place.service.PlaceService;
import com.sofly.core.global.client.dto.PlacesResponse;
import com.sofly.core.global.client.dto.PlacesResponse.PhotoMedia;
import com.sofly.core.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Place", description = "장소 검색")
@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @Operation(summary = "장소 검색")
    @GetMapping
    public ResponseEntity<ApiResponse<PlacesResponse>> searchPlaces(@RequestParam String text) {
        return ResponseEntity.ok(ApiResponse.success(placeService.searchPlaces(text)));
    }

    @Operation(summary = "장소 사진 조회")
    @GetMapping("/photo")
    public ResponseEntity<ApiResponse<PhotoMedia>> getPlacePhoto(
            @RequestParam String name,
            @RequestParam(defaultValue = "800") int maxWidthPx
    ) {
        return ResponseEntity.ok(ApiResponse.success(placeService.getPlacePhoto(name, maxWidthPx)));
    }
}
