package com.sofly.core.domain.place.controller;

import com.sofly.core.domain.place.service.PlaceService;
import com.sofly.core.global.client.dto.PlacesResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PlaceControllerTest {

    private final PlaceService placeService = mock(PlaceService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PlaceController(placeService))
            .build();

    @Test
    @DisplayName("GET /api/v1/places는 ApiResponse 안에 places 배열을 반환한다")
    void searchPlaces_returnsWrappedPlacesArray() throws Exception {
        given(placeService.searchPlaces("경복궁"))
                .willReturn(new PlacesResponse(List.of(new PlacesResponse.Place(
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
                ))));

        mockMvc.perform(get("/api/v1/places").param("text", "경복궁"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.places", hasSize(1)))
                .andExpect(jsonPath("$.data.places[0].displayName.text").value("경복궁"))
                .andExpect(jsonPath("$.data.places[0].formattedAddress").value("서울특별시 종로구 사직로 161"));
    }

    @Test
    @DisplayName("검색 결과가 없어도 data.places는 빈 배열로 반환한다")
    void searchPlaces_returnsEmptyPlacesArray() throws Exception {
        given(placeService.searchPlaces("없는장소"))
                .willReturn(new PlacesResponse(List.of()));

        mockMvc.perform(get("/api/v1/places").param("text", "없는장소"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.places", hasSize(0)));
    }
}
