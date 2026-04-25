package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의 상태 응답")
public record ClassStatusResponse(
		@Schema(description = "현재 강의 상태", example = "OPEN")
		ClassStatus status
) {
}
