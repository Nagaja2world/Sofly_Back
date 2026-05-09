package com.sofly.supply.adapter.outbound.google;

import com.sofly.supply.application.dto.PlacesResponse;
import com.sofly.supply.application.dto.PlacesResponse.PhotoMedia;
import com.sofly.supply.application.port.outbound.PlaceInfoPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;
import java.util.Optional;

@Component
public class GooglePlacesClient implements PlaceInfoPort {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesClient.class);

    private final WebClient googlePlacesWebClient;
    private final GooglePlacesProperties props;

    public GooglePlacesClient(@Qualifier("googlePlacesWebClient") WebClient googlePlacesWebClient,
                               GooglePlacesProperties props) {
        this.googlePlacesWebClient = googlePlacesWebClient;
        this.props = props;
    }

    public Optional<PlacesResponse> searchText(String text) {
        String normalizedText = normalizeSearchText(text);

        if (normalizedText.isBlank()) {
            log.warn("Search text is null or blank");
            return Optional.empty();
        }

        if (props.apiKey() == null || props.apiKey().isBlank()) {
            log.warn("Google Places API key is not configured");
            return Optional.empty();
        }

        try{
            // Places API (New): POST /v1/places:searchText
            PlacesResponse response = googlePlacesWebClient.post()
                    .uri("/v1/places:searchText")
                    .header("X-Goog-Api-Key", props.apiKey())
                    .header("X-Goog-FieldMask",
                            "places.id," +
                                    "places.displayName," +
                                    "places.primaryType," +
                                    "places.formattedAddress," +
                                    "places.location," +
                                    "places.rating," +
                                    "places.userRatingCount," +
                                    "places.editorialSummary," +
                                    "places.websiteUri," +
                                    "places.nationalPhoneNumber," +
                                    "places.googleMapsUri," +
                                    "places.businessStatus," +
                                    "places.priceLevel," +
                                    "places.photos"
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "textQuery", normalizedText,
                            "languageCode", "ko"
                            ))
                    .retrieve()
                    .bodyToMono(PlacesResponse.class)
                    .block();

            return Optional.ofNullable(response);

        } catch (WebClientResponseException e) {
            log.warn("Google Places API error for text '{}': {} {}", normalizedText, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error during Google Places enrichment for text '{}': {}", normalizedText, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<PhotoMedia> getPhotoMedia(String photoName, int maxWidthPx) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            log.warn("Google Places API key is not configured");
            return Optional.empty();
        }

        try {
            PhotoMedia response = googlePlacesWebClient.get()
                    .uri("/v1/" + photoName + "/media?maxWidthPx=" + maxWidthPx + "&skipHttpRedirect=true")
                    .header("X-Goog-Api-Key", props.apiKey())
                    .retrieve()
                    .bodyToMono(PhotoMedia.class)
                    .block();

            return Optional.ofNullable(response);

        } catch (WebClientResponseException e) {
            log.warn("Google Places photo error for '{}': {} {}", photoName, e.getStatusCode(), e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error fetching photo '{}': {}", photoName, e.getMessage());
            return Optional.empty();
        }
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("[\\u200B-\\u200D\\u2060\\uFEFF]", "")
                .trim();
    }
}
