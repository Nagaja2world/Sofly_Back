package com.sofly.core.domain.travellog.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TravellogPhotoLinkRequest(
        @NotEmpty List<Long> photoIds
) {}
