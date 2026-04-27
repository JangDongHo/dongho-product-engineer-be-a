package io.github.jangdongho.productengineer.enrollment.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 수강 신청 상태. 전이: {@code PENDING → CONFIRMED}, 또는 {@code PENDING|CONFIRMED → CANCELLED}
 */
@Schema(
		description = "수강 신청 상태. 전이: PENDING -> CONFIRMED, 또는 PENDING|CONFIRMED -> CANCELLED",
		allowableValues = {"PENDING", "CONFIRMED", "CANCELLED"})
public enum EnrollmentStatus {
	PENDING,
	CONFIRMED,
	CANCELLED
}
