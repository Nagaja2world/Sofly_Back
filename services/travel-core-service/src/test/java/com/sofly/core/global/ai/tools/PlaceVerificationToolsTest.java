package com.sofly.core.global.ai.tools;

import com.sofly.core.global.client.SupplyClient;
import com.sofly.core.global.client.dto.PlacesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceVerificationToolsTest {

    @Mock
    SupplyClient supplyClient;

    @InjectMocks
    PlaceVerificationTools placeVerificationTools;

    private PlacesResponse.Place gyeongbokgung;

    @BeforeEach
    void setUp() {
        gyeongbokgung = new PlacesResponse.Place(
                "ChIJr-OBkIiffDUR_M5X3TBrFRY",
                new PlacesResponse.DisplayName("경복궁", "ko"),
                "tourist_attraction",
                "서울특별시 종로구 사직로 161",
                new PlacesResponse.Location(37.5796, 126.9770),
                4.6,
                50000,
                null, null, null, null, null, null,
                null
        );
    }

    @Test
    @DisplayName("장소가 존재하면 이름·주소·유형·평점을 포함한 결과를 반환한다")
    void verifyPlace_존재하는_장소_정보_반환() {
        given(supplyClient.searchPlace("경복궁"))
                .willReturn(new PlacesResponse(List.of(gyeongbokgung)));

        PlaceVerificationTools.PlaceVerificationResult result = placeVerificationTools.verifyPlace("경복궁");

        assertThat(result.found()).isTrue();
        assertThat(result.name()).isEqualTo("경복궁");
        assertThat(result.address()).isEqualTo("서울특별시 종로구 사직로 161");
        assertThat(result.type()).isEqualTo("tourist_attraction");
        assertThat(result.rating()).isEqualTo(4.6);
        verify(supplyClient).searchPlace("경복궁");
    }

    @Test
    @DisplayName("검색 결과가 비어있으면 found=false를 반환한다")
    void verifyPlace_결과_없음_메시지_반환() {
        given(supplyClient.searchPlace("없는장소xyz"))
                .willReturn(new PlacesResponse(List.of()));

        PlaceVerificationTools.PlaceVerificationResult result = placeVerificationTools.verifyPlace("없는장소xyz");

        assertThat(result.found()).isFalse();
    }

    @Test
    @DisplayName("places 필드가 null이면 found=false를 반환한다")
    void verifyPlace_null_응답_처리() {
        given(supplyClient.searchPlace("테스트"))
                .willReturn(new PlacesResponse(null));

        PlaceVerificationTools.PlaceVerificationResult result = placeVerificationTools.verifyPlace("테스트");

        assertThat(result.found()).isFalse();
    }

    @Test
    @DisplayName("평점이 null인 장소도 정상 반환한다")
    void verifyPlace_평점_없는_장소_처리() {
        var noRatingPlace = new PlacesResponse.Place(
                "id2",
                new PlacesResponse.DisplayName("신규 카페", "ko"),
                "cafe",
                "서울특별시 마포구 어딘가",
                new PlacesResponse.Location(37.55, 126.92),
                null, null, null, null, null, null, null, null, null
        );
        given(supplyClient.searchPlace("신규 카페"))
                .willReturn(new PlacesResponse(List.of(noRatingPlace)));

        PlaceVerificationTools.PlaceVerificationResult result = placeVerificationTools.verifyPlace("신규 카페");

        assertThat(result.found()).isTrue();
        assertThat(result.name()).isEqualTo("신규 카페");
        assertThat(result.rating()).isNull();
    }
}
