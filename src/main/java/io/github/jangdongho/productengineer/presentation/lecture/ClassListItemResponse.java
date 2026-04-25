package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import java.time.LocalDateTime;

public record ClassListItemResponse(
		Long id,
		Long creatorId,
		String title,
		ClassStatus status,
		long price,
		int capacity,
		LocalDateTime startDate,
		LocalDateTime endDate
) {
}
