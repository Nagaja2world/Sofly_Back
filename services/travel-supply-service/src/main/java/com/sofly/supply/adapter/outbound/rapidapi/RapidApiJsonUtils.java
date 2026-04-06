package com.sofly.supply.adapter.outbound.rapidapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RapidApiJsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RapidApiJsonUtils() {}

    public static JsonNode parseJson(String response) {
        try {
            return OBJECT_MAPPER.readTree(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("RapidAPI 응답 파싱 실패", e);
        }
    }

    public static JsonNode nullNode() {
        return OBJECT_MAPPER.nullNode();
    }

    public static String textOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asText() : null;
    }

    public static Double doubleOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asDouble() : null;
    }

    public static Integer intOrNull(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).asInt() : null;
    }
}
