package com.sofly.core.global.ai.tools;

import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.PlacesResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlaceVerificationTools {

    private final SupplyClient supplyClient;

    @Tool(description = "여행 일정에 포함될 장소 이름으로 실제 존재하는 장소인지 확인합니다. 장소명(한국어 또는 영어)을 입력하면 Google Places에서 실제 장소 정보를 반환합니다.")
    public String verifyPlace(
            @ToolParam(description = "검색할 장소명 (예: '경복궁', 'Eiffel Tower')") String placeName
    ) {
        PlacesResponse response = supplyClient.searchPlace(placeName);
        if (response.places() == null || response.places().isEmpty()) {
            return "'" + placeName + "'에 해당하는 장소를 찾을 수 없습니다.";
        }
        PlacesResponse.Place place = response.places().get(0);
        return String.format("장소 확인됨: %s | 주소: %s | 유형: %s | 평점: %s",
                place.displayName().text(),
                place.formattedAddress(),
                place.primaryType(),
                place.rating() != null ? place.rating() : "없음"
        );
    }
}
