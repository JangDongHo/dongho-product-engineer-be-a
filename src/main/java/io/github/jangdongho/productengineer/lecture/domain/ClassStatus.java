package io.github.jangdongho.productengineer.lecture.domain;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 강의 모집 상태. 전이: {@code DRAFT → OPEN → CLOSED}
 */
@Schema(description = "강의 모집 상태. 전이: DRAFT -> OPEN -> CLOSED", allowableValues = {"DRAFT", "OPEN", "CLOSED"})
public enum ClassStatus {
	DRAFT,
	OPEN,
	CLOSED
}
