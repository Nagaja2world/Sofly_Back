package com.sofly.supply.adapter.outbound.google;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GooglePlacesClient {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);
    private static final int MAX_PHOTOS = 3;

    private final WebClient googlePlacesWebClient;
    private final GooglePlacesProperties props;

    public GooglePlacesClient(@Qualifier("googlePlacesWebClient") WebClient googlePlacesWebClient,
                               GooglePlacesProperties props) {
        this.googlePlacesWebClient = googlePlacesWebClient;
        this.props = props;
    }

    /**
     * 호텔 이름과 도시 코드로 Google Places 정보(평점, 리뷰 수, 사진 URL) 조회.
     * API 키 미설정 또는 조회 실패 시 null 반환 (enrichment 실패가 전체 응답을 깨지 않음).
     */
    public PlaceInfo fetchPlaceInfo(String hotelName, String cityCode) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            log.warn("Google Places API key is not configured");
            return null;
        }

        try {
            String query = hotelName + " hotel " + cityCode;

            // Places API (New): POST /v1/places:searchText
            JsonNode response = googlePlacesWebClient.post()
                    .uri("/v1/places:searchText")
                    .header("X-Goog-Api-Key", props.apiKey())
                    .header("X-Goog-FieldMask", "places.id,places.rating,places.userRatingCount,places.photos")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "textQuery", query,
                            "includedType", "lodging"
                    ))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null || !response.has("places") || response.get("places").isEmpty()) {
                log.info("Google Places (New) empty results. response: {}", response);
                return null;
            }

            JsonNode place = response.get("places").get(0);

            String placeId = place.has("id") ? place.get("id").asText() : null;
            Double rating = place.has("rating") ? place.get("rating").asDouble() : null;
            Integer userRatingsTotal = place.has("userRatingCount") ? place.get("userRatingCount").asInt() : null;

            List<String> photoUrls = new ArrayList<>();
            if (place.has("photos")) {
                for (JsonNode photo : place.get("photos")) {
                    if (photoUrls.size() >= MAX_PHOTOS) break;
                    // 새 API: photo.name = "places/{placeId}/photos/{photoRef}"
                    String photoName = photo.get("name").asText();
                    photoUrls.add(buildPhotoUrl(photoName));
                }
            }

            return new PlaceInfo(placeId, rating, userRatingsTotal); //photoUrls 추가 예정

        } catch (WebClientResponseException e) {
            log.warn("Google Places API error for hotel '{}': {} {}", hotelName, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            log.warn("Unexpected error during Google Places enrichment for hotel '{}': {}", hotelName, e.getMessage());
            return null;
        }
    }

    private String buildPhotoUrl(String photoName) {
        // 새 Places API 사진 URL: /v1/{photoName}/media?maxWidthPx=800&key=...
        return props.baseUrl()
                + "/v1/" + photoName
                + "/media?maxWidthPx=800"
                + "&key=" + props.apiKey();
    }
}
