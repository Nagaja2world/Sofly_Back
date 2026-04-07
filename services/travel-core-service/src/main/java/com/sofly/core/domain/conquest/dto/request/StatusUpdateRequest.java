package com.sofly.core.domain.conquest.dto.request;

import com.sofly.core.domain.conquest.enums.VisitStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class StatusUpdateRequest {

    @NotNull(message = "상태값은 필수입니다.")
    private VisitStatus status;
}
