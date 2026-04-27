package io.github.jangdongho.productengineer.enrollment.dto;

import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "수강 신청 결제 확정 응답")
public record EnrollmentConfirmedResponse(
		@Schema(description = "수강 신청 ID", example = "1")
		Long id,

		@Schema(description = "수강 신청 상태", example = "CONFIRMED")
		EnrollmentStatus status,

		@Schema(description = "결제 확정 시각(취소 가능 기간 기준)", example = "2026-05-01T12:00:00")
		LocalDateTime confirmedAt
) {
}
