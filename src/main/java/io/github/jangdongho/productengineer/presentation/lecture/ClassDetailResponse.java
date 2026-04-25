package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import java.time.LocalDateTime;

public record ClassDetailResponse(
		Long id,
		Long creatorId,
		String title,
		String description,
		ClassStatus status,
		long price,
		int capacity,
		int currentEnrollment,
		LocalDateTime startDate,
		LocalDateTime endDate
) {
}
