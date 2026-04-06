package com.sofly.supply.adapter.outbound.rapidapi.flights;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

class BookingComFlightResponseFilter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BookingComFlightResponseFilter() {}

    static JsonNode filter(JsonNode root) {
        ObjectNode result = MAPPER.createObjectNode();
        result.put("status", root.path("status").asBoolean());

        JsonNode data = root.path("data");
        if (data.isMissingNode()) return result;

        ObjectNode filteredData = MAPPER.createObjectNode();
        filteredData.set("flightOffers", filterOffers(data.path("flightOffers")));
        result.set("data", filteredData);
        return result;
    }

    private static ArrayNode filterOffers(JsonNode offers) {
        ArrayNode result = MAPPER.createArrayNode();
        for (JsonNode offer : offers) {
            result.add(filterOffer(offer));
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
