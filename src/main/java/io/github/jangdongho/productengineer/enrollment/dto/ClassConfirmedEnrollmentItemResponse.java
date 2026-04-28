package io.github.jangdongho.productengineer.enrollment.dto;

import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "강의별 확정 수강생 목록 항목 (크리에이터 조회용, CONFIRMED만)")
public record ClassConfirmedEnrollmentItemResponse(
    @Schema(description = "수강 신청 ID", example = "1") long enrollmentId,
    @Schema(description = "수강생 사용자 ID", example = "42") long userId,
    @Schema(description = "수강 신청 상태", example = "CONFIRMED") EnrollmentStatus status,
    @Schema(description = "결제 확정 시각", example = "2026-05-01T12:00:00") LocalDateTime confirmedAt) {
  public static ClassConfirmedEnrollmentItemResponse from(Enrollment enrollment) {
    return new ClassConfirmedEnrollmentItemResponse(
        enrollment.getId(),
        enrollment.getUserId(),
        enrollment.getStatus(),
        enrollment.getConfirmedAt());
  }
}
