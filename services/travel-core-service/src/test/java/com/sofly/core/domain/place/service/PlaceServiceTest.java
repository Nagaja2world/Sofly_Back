package com.sofly.core.domain.place.service;

import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.PlacesResponse;
import com.sofly.core.global.exception.ErrorCode;
import com.sofly.core.global.exception.SoflyException;
import feign.FeignException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock
    SupplyClient supplyClient;

    @InjectMocks
    PlaceService placeService;

    @Test
    @DisplayName("장소 검색 응답을 그대로 반환한다")
    void searchPlaces_returnsSupplyResponse() {
        PlacesResponse response = new PlacesResponse(List.of(new PlacesResponse.Place(
                "place-id",
                new PlacesResponse.DisplayName("경복궁", "ko"),
                "tourist_attraction",
                "서울특별시 종로구 사직로 161",
                new PlacesResponse.Location(37.5796, 126.9770),
                4.6,
                50000,
                null,
                null,
                null,
                "https://maps.google.com/?cid=1",
                "OPERATIONAL",
                null,
                List.of()
        )));
        given(supplyClient.searchPlace("경복궁")).willReturn(response);

        PlacesResponse result = placeService.searchPlaces("경복궁");

        assertThat(result.places()).hasSize(1);
        assertThat(result.places().getFirst().displayName().text()).isEqualTo("경복궁");
    }

    @Test
    @DisplayName("supply가 null을 반환해도 빈 places 배열로 정규화한다")
    void searchPlaces_normalizesNullResponse() {
        given(supplyClient.searchPlace("없는장소")).willReturn(null);

        PlacesResponse result = placeService.searchPlaces("없는장소");

        assertThat(result.places()).isEmpty();
    }

    @Test
    @DisplayName("supply가 places null을 반환해도 빈 places 배열로 정규화한다")
    void searchPlaces_normalizesNullPlaces() {
        given(supplyClient.searchPlace("없는장소")).willReturn(new PlacesResponse(null));

        PlacesResponse result = placeService.searchPlaces("없는장소");

        assertThat(result.places()).isEmpty();
    }

    @Test
    @DisplayName("supply Feign 오류는 SUPPLY_SERVICE_ERROR로 변환한다")
    void searchPlaces_wrapsFeignException() {
        given(supplyClient.searchPlace("경복궁")).willThrow(FeignException.ServiceUnavailable.class);

        assertThatThrownBy(() -> placeService.searchPlaces("경복궁"))
                .isInstanceOf(SoflyException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SUPPLY_SERVICE_ERROR);
    }
}
