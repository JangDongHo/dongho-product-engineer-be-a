package io.github.jangdongho.productengineer.presentation.enrollment;

import io.github.jangdongho.productengineer.persistence.enrollment.EnrollmentStatus;
import io.github.jangdongho.productengineer.presentation.lecture.ClassListItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "사용자 수강 신청 목록 항목 (강의 요약 + 신청 상태)")
public record EnrollmentListItemResponse(
		@Schema(description = "수강 신청 ID", example = "1")
		long enrollmentId,

		@Schema(description = "신청 상태", example = "PENDING")
		EnrollmentStatus status,

		@Schema(description = "결제 확정 시각 (CONFIRMED일 때만)", example = "2026-05-01T12:00:00", nullable = true)
		LocalDateTime confirmedAt,
		
		@Schema(description = "강의 요약 (강의 목록 항목과 동일 구조)")
		ClassListItemResponse lecture) {
}
