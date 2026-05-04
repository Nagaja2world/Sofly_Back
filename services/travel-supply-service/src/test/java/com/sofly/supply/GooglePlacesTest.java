package com.sofly.supply;

import com.sofly.supply.adapter.outbound.google.GooglePlacesClient;
import com.sofly.supply.adapter.outbound.google.GooglePlacesProperties;
import com.sofly.supply.application.dto.PlacesResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class GooglePlacesTest {

    @Test
    void searchTextMapsGooglePlacesResponse() throws IOException {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBody("""
                            {
                              "places": [
                                {
                                  "id": "ChIJ92BD_-2ifDURWdfj5R8TWjs",
                                  "formattedAddress": "대한민국 서울특별시 중구 을지로 30",
                                  "rating": 4.5,
                                  "userRatingCount": 8734,
                                  "displayName": {
                                    "text": "롯데호텔 서울",
                                    "languageCode": "ko"
                                  }
                                }
                              ]
                            }
                            """));

            GooglePlacesClient client = new GooglePlacesClient(
                    WebClient.builder().baseUrl(server.url("/").toString()).build(),
                    new GooglePlacesProperties("test-api-key", server.url("/").toString())
            );

            Optional<PlacesResponse> response = client.searchText("Lotte Hotel Seoul");

            assertThat(response).isPresent();
            assertThat(response.get().places()).hasSize(1);
            PlacesResponse.Place place = response.get().places().getFirst();
            assertThat(place.displayName().text()).isEqualTo("롯데호텔 서울");
            assertThat(place.formattedAddress()).isEqualTo("대한민국 서울특별시 중구 을지로 30");
            assertThat(place.rating()).isEqualTo(4.5);
            assertThat(place.userRatingCount()).isEqualTo(8734);
        }
    }
}
