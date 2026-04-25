package io.github.jangdongho.productengineer.persistence.lecture;

/**
 * 강의 모집 상태. 전이: {@code DRAFT → OPEN → CLOSED}
 */
public enum ClassStatus {
	DRAFT,
	OPEN,
	CLOSED
}
