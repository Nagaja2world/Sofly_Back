package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.Map;

class BookingComFlightResponseFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static final int PAGE_SIZE = 15;

    private BookingComFlightResponseFilter() {}

    static JsonNode filter(JsonNode root, List<String> airlineFilter) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("status", root.path("status").asBoolean());

        JsonNode data = root.path("data");
        if (data.isMissingNode()) return result;

        JsonNode allOffers = data.path("flightOffers");
        Map<String, ObjectNode> availableAirlines = extractAvailableAirlines(allOffers);

        ObjectNode filteredData = MAPPER.createObjectNode();
        filteredData.set("flightOffers", filterOffers(allOffers, airlineFilter));
        filteredData.set("aggregation", buildAggregation(data.path("aggregation").path("totalCount").asLong(), availableAirlines, null));
        result.set("data", filteredData);
        return result;
    }

    static JsonNode buildResponse(boolean status, long totalCount, List<ObjectNode> offers,
                                   Map<String, ObjectNode> airlines, String nextPageCursor) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("status", status);

        ArrayNode offersArray = MAPPER.createArrayNode();
        offers.forEach(offersArray::add);

        ObjectNode data = MAPPER.createObjectNode();
        data.set("flightOffers", offersArray);
        data.set("aggregation", buildAggregation(totalCount, airlines, nextPageCursor));
        result.set("data", data);
        return result;
    }

    static Map<String, ObjectNode> extractAvailableAirlines(JsonNode offers) {
        Map<String, ObjectNode> airlines = new LinkedHashMap<>();
        for (JsonNode offer : offers) {
            for (JsonNode segment : offer.path("segments")) {
                for (JsonNode leg : segment.path("legs")) {
                    for (JsonNode c : leg.path("carriersData")) {
                        String code = c.path("code").asText();
                        if (!code.isEmpty() && !airlines.containsKey(code)) {
                            ObjectNode airline = MAPPER.createObjectNode();
                            airline.put("code", code);
                            airline.put("name", c.path("name").asText());
                            airline.put("logo", c.path("logo").asText());
                            airlines.put(code, airline);
                        }
                    }
                }
            }
        }
        return airlines;
    }

    static boolean matchesAirlineFilter(JsonNode offer, Set<String> filterSet) {
        for (JsonNode segment : offer.path("segments")) {
            for (JsonNode leg : segment.path("legs")) {
                for (JsonNode c : leg.path("carriersData")) {
                    if (filterSet.contains(c.path("code").asText())) return true;
                }
            }
        }
        return false;
    }

    static ObjectNode mapOffer(JsonNode offer) {
        return filterOffer(offer);
    }

    private static ObjectNode buildAggregation(long totalCount, Map<String, ObjectNode> availableAirlines,
                                                String nextPageCursor) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("totalCount", totalCount);
        if (nextPageCursor != null) node.put("nextPageCursor", nextPageCursor);
        ArrayNode airlineArray = MAPPER.createArrayNode();
        availableAirlines.values().forEach(airlineArray::add);
        node.set("availableAirlines", airlineArray);
        return node;
    }

    private static ArrayNode filterOffers(JsonNode offers, List<String> airlineFilter) {
        ArrayNode result = MAPPER.createArrayNode();
        Set<String> filterSet = airlineFilter != null && !airlineFilter.isEmpty()
                ? new java.util.HashSet<>(airlineFilter)
                : null;
        for (JsonNode offer : offers) {
            if (filterSet == null || matchesAirlineFilter(offer, filterSet)) {
                result.add(filterOffer(offer));
            }
        }
        return result;
    }

    private static ObjectNode filterOffer(JsonNode offer) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("token", offer.path("token").asText());
        node.put("tripType", offer.path("tripType").asText());
        node.put("offerKeyToHighlight", offer.path("offerKeyToHighlight").asText());

        JsonNode seat = offer.path("seatAvailability");
        if (!seat.isMissingNode()) node.set("seatAvailability", seat);

        JsonNode price = offer.path("priceBreakdown");
        if (!price.isMissingNode()) node.set("price", filterPrice(price));

        JsonNode fareInfo = offer.path("brandedFareInfo");
        if (!fareInfo.isMissingNode() && fareInfo.hasNonNull("fareName")) {
            ObjectNode fare = MAPPER.createObjectNode();
            fare.put("fareName", fareInfo.path("fareName").asText());
            fare.put("cabinClass", fareInfo.path("cabinClass").asText());
            node.set("brandedFareInfo", fare);
        }

        node.set("segments", filterSegments(offer.path("segments")));
        return node;
    }

    private static ObjectNode filterPrice(JsonNode priceBreakdown) {
        ObjectNode node = MAPPER.createObjectNode();
        copyMoneyField(node, "total", priceBreakdown.path("total"));
        copyMoneyField(node, "baseFare", priceBreakdown.path("baseFare"));
        copyMoneyField(node, "tax", priceBreakdown.path("tax"));
        return node;
    }

    private static void copyMoneyField(ObjectNode target, String key, JsonNode money) {
        if (money.isMissingNode()) return;
        ObjectNode node = MAPPER.createObjectNode();
        node.put("currencyCode", money.path("currencyCode").asText());
        node.put("units", money.path("units").asLong());
        node.put("nanos", money.path("nanos").asLong());
        target.set(key, node);
    }

    private static ArrayNode filterSegments(JsonNode segments) {
        ArrayNode result = MAPPER.createArrayNode();
        for (JsonNode segment : segments) {
            result.add(filterSegment(segment));
        }
        return result;
    }

    private static ObjectNode filterSegment(JsonNode segment) {
        ObjectNode node = MAPPER.createObjectNode();
        node.set("departureAirport", filterAirport(segment.path("departureAirport")));
        node.set("arrivalAirport", filterAirport(segment.path("arrivalAirport")));
        node.put("departureTime", segment.path("departureTime").asText());
        node.put("arrivalTime", segment.path("arrivalTime").asText());
        node.put("totalTime", segment.path("totalTime").asInt());
        node.set("legs", filterLegs(segment.path("legs")));
        return node;
    }

    private static ObjectNode filterAirport(JsonNode airport) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("code", airport.path("code").asText());
        node.put("cityName", airport.path("cityName").asText());
        node.put("countryName", airport.path("countryName").asText());
        String terminal = airport.path("terminal").asText(null);
        if (terminal != null && !terminal.isEmpty()) node.put("terminal", terminal);
        return node;
    }

    private static ArrayNode filterLegs(JsonNode legs) {
        ArrayNode result = MAPPER.createArrayNode();
        for (JsonNode leg : legs) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("departureTime", leg.path("departureTime").asText());
            node.put("arrivalTime", leg.path("arrivalTime").asText());
            node.put("totalTime", leg.path("totalTime").asInt());
            node.put("cabinClass", leg.path("cabinClass").asText());

            JsonNode flightInfo = leg.path("flightInfo");
            node.put("flightNumber", flightInfo.path("flightNumber").asInt());
            node.put("planeType", flightInfo.path("planeType").asText());

            JsonNode carrierInfo = flightInfo.path("carrierInfo");
            node.put("operatingCarrier", carrierInfo.path("operatingCarrier").asText());
            node.put("marketingCarrier", carrierInfo.path("marketingCarrier").asText());

            // carriersData 중복 제거 (code 기준)
            JsonNode carriersData = leg.path("carriersData");
            ArrayNode uniqueCarriers = MAPPER.createArrayNode();
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            for (JsonNode c : carriersData) {
                String code = c.path("code").asText();
                if (seen.add(code)) {
                    ObjectNode carrier = MAPPER.createObjectNode();
                    carrier.put("name", c.path("name").asText());
                    carrier.put("code", code);
                    carrier.put("logo", c.path("logo").asText());
                    uniqueCarriers.add(carrier);
                }
            }
            node.set("carriersData", uniqueCarriers);
            node.set("flightStops", leg.path("flightStops"));

            String depTerminal = leg.path("departureTerminal").asText(null);
            String arrTerminal = leg.path("arrivalTerminal").asText(null);
            if (depTerminal != null && !depTerminal.isEmpty()) node.put("departureTerminal", depTerminal);
            if (arrTerminal != null && !arrTerminal.isEmpty()) node.put("arrivalTerminal", arrTerminal);

            result.add(node);
        }
        return result;
    }
}
