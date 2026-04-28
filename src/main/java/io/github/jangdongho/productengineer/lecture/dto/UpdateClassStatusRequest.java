package io.github.jangdongho.productengineer.lecture.dto;

import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "강의 상태 변경 요청")
public record UpdateClassStatusRequest(
    @Schema(
            description = "변경할 강의 상태",
            example = "OPEN",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        ClassStatus status) {}
