package com.sofly.core.global.ai.tools;

import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.PlacesResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceVerificationTools {

    private final SupplyClient supplyClient;

    public record PlaceVerificationResult(
            boolean found,
            String name,
            String address,
            String placeId,
            String photoReference,
            Double latitude,
            Double longitude,
            String type,
            Double rating
    ) {}

    @Tool(description = "여행 일정에 포함될 장소 이름으로 실제 존재하는 장소인지 확인합니다. 장소명(한국어 또는 영어)을 입력하면 Google Places에서 실제 장소 정보를 반환합니다. 반환된 placeId와 photoReference를 JSON 출력에 그대로 사용하세요.")
    public PlaceVerificationResult verifyPlace(
            @ToolParam(description = "검색할 장소명 (예: '경복궁', 'Eiffel Tower')") String placeName
    ) {
        try {
            log.info("verifyPlace tool called - placeName={}", placeName);
            PlacesResponse response = supplyClient.searchPlace(placeName);
            if (response.places() == null || response.places().isEmpty()) {
                log.info("verifyPlace tool - place not found - placeName={}", placeName);
                return new PlaceVerificationResult(false, placeName, null, null, null, null, null, null, null);
            }
            PlacesResponse.Place place = response.places().get(0);
            String photoRef = (place.photos() != null && !place.photos().isEmpty())
                    ? place.photos().get(0).name()
                    : null;
            Double lat = place.location() != null ? place.location().latitude() : null;
            Double lng = place.location() != null ? place.location().longitude() : null;
            log.info("verifyPlace tool resolved - placeName={}, resolvedName={}, placeId={}", placeName, place.displayName().text(), place.id());
            return new PlaceVerificationResult(
                    true,
                    place.displayName().text(),
                    place.formattedAddress(),
                    place.id(),
                    photoRef,
                    lat,
                    lng,
                    place.primaryType(),
                    place.rating()
            );
        } catch (Exception e) {
            log.warn("verifyPlace tool failed - placeName={}", placeName, e);
            return new PlaceVerificationResult(false, placeName, null, null, null, null, null, null, null);
        }
    }
}
