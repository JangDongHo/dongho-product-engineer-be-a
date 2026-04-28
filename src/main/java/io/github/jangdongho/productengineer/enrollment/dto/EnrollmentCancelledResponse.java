package io.github.jangdongho.productengineer.enrollment.dto;

import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수강 신청 취소 응답")
public record EnrollmentCancelledResponse(
    @Schema(description = "수강 신청 ID", example = "1") Long id,
    @Schema(description = "수강 신청 상태", example = "CANCELLED") EnrollmentStatus status) {}
