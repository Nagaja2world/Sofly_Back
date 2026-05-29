package com.sofly.core.domain.sns.dto;

public record TrendingDestinationResponse(
        int rank,
        String destination,
        String countryCode,
        long workspaceCount
) {}
