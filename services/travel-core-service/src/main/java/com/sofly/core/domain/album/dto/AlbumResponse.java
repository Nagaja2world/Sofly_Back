package com.sofly.core.domain.album.dto;

import java.util.List;

public record AlbumResponse(
        Long albumId,
        Long workspaceId,
        List<PhotoResponse> photos
) {}
