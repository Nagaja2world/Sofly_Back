package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sofly.supply.adapter.outbound.rapidapi.RapidApiJsonUtils;
import com.sofly.supply.application.dto.FlightSearchRequest;
import com.sofly.supply.application.port.outbound.FlightSupplierPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingComFlightSupplierAdapter implements FlightSupplierPort {

    private static final int MAX_FETCH_PAGES = 10;

    private final WebClient rapidApiWebClient;

    @Override
    public String supplierKey() {
        return "booking";
    }

    @Cacheable(
        value = "bookingFlights",
        key = "#request",
        unless = "#result == null || #result.path('data').path('flightOffers').size() == 0"
    )
    @Override
    public JsonNode searchFlightOffers(FlightSearchRequest request) {
        List<String> airlineFilter = request.getAirlines() != null ? request.getAirlines() : List.of();

        if (airlineFilter.isEmpty()) {
            String response = fetchPage(request, request.getPageNo());
            if (response == null) return RapidApiJsonUtils.nullNode();
            return BookingComFlightResponseFilter.filter(
                    RapidApiJsonUtils.parseJson(response),
                    List.of(),
                    request.getAdults() != null ? request.getAdults() : 1
            );
        }

        return fetchFilteredUntilFull(request, airlineFilter);
    }

    private JsonNode fetchFilteredUntilFull(FlightSearchRequest request, List<String> airlineFilter) {
        Set<String> filterSet = new HashSet<>(airlineFilter);
        List<ObjectNode> collected = new ArrayList<>();
        Map<String, ObjectNode> allAirlines = new LinkedHashMap<>();
        long totalCount = 0;
        boolean status = false;
        String nextPageCursor = null;

        int startPage;
        int startOffset;
        if (request.getCursor() != null && !request.getCursor().isBlank()) {
            String[] parts = request.getCursor().split(":");
            startPage = Integer.parseInt(parts[0]);
            startOffset = Integer.parseInt(parts[1]);
        } else {
            startPage = request.getPageNo() != null ? request.getPageNo() : 1;
            startOffset = 0;
        }
        int maxPage = startPage + MAX_FETCH_PAGES - 1;

        outer:
        for (int pageNo = startPage; pageNo <= maxPage; pageNo++) {
            String raw = fetchPage(request, pageNo);
            if (raw == null) break;

            JsonNode root = RapidApiJsonUtils.parseJson(raw);
            JsonNode data = root.path("data");
            if (data.isMissingNode()) break;

            if (pageNo == startPage) {
                status = root.path("status").asBoolean();
                totalCount = data.path("aggregation").path("totalCount").asLong();
                long apiMaxPage = (long) Math.ceil((double) totalCount / BookingComFlightResponseFilter.PAGE_SIZE);
                maxPage = (int) Math.min(maxPage, startPage + (int) apiMaxPage - 1);
            }

            JsonNode offers = data.path("flightOffers");
            if (!offers.isArray() || offers.isEmpty()) break;

            allAirlines.putAll(BookingComFlightResponseFilter.extractAvailableAirlines(offers));

            int offsetInPage = (pageNo == startPage) ? startOffset : 0;
            for (int i = offsetInPage; i < offers.size(); i++) {
                if (BookingComFlightResponseFilter.matchesAirlineFilter(offers.get(i), filterSet)) {
                    collected.add(BookingComFlightResponseFilter.mapOffer(offers.get(i), request.getAdults() != null ? request.getAdults() : 1));
                    if (collected.size() >= BookingComFlightResponseFilter.PAGE_SIZE) {
                        nextPageCursor = (i + 1 < offers.size())
                                ? pageNo + ":" + (i + 1)
                                : (pageNo + 1) + ":0";
                        break outer;
                    }
                }
            }
        }

        return BookingComFlightResponseFilter.buildResponse(status, totalCount, collected, allAirlines, nextPageCursor);
    }

    private String fetchPage(FlightSearchRequest request, int pageNo) {
        return rapidApiWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/api/v1/flights/searchFlights")
                            .queryParam("fromId", request.getFromId())
                            .queryParam("toId", request.getToId())
                            .queryParam("departDate", request.getDepartDate())
                            .queryParam("adults", request.getAdults())
                            .queryParam("pageNo", pageNo);

                    if (request.getReturnDate() != null)
                        builder.queryParam("returnDate", request.getReturnDate());
                    if (request.getStops() != null)
                        builder.queryParam("stops", request.getStops().getValue());
                    if (request.getSort() != null)
                        builder.queryParam("sort", request.getSort());
                    if (request.getCabinClass() != null)
                        builder.queryParam("cabinClass", request.getCabinClass());
                    if (request.getCurrencyCode() != null)
                        builder.queryParam("currency_code", request.getCurrencyCode());
                    if (request.getChildrenAge() != null && !request.getChildrenAge().isBlank())
                        builder.queryParam("children", request.getChildrenAge());

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    @Cacheable(
        value = "bookingFlightDetails",
        key = "#token + ':' + #currencyCode",
        unless = "#result == null " +
                "|| #result.path('status').asBoolean() == false " +
                "|| #result.path('data').isMissingNode() " +
                "|| #result.path('data').path('segments').size() == 0"
    )
    @Override
    public JsonNode getFlightDetails(String token, String currencyCode) {
        String response = rapidApiWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/api/v1/flights/getFlightDetails")
                            .queryParam("token", token);

                    if (currencyCode != null && !currencyCode.isBlank())
                        builder.queryParam("currency_code", currencyCode);

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (response == null) return RapidApiJsonUtils.nullNode();
        return RapidApiJsonUtils.parseJson(response);
    }
}
