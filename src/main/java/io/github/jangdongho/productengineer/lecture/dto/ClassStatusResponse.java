package io.github.jangdongho.productengineer.lecture.dto;

import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의 상태 응답")
public record ClassStatusResponse(
    @Schema(description = "현재 강의 상태", example = "OPEN") ClassStatus status) {}
