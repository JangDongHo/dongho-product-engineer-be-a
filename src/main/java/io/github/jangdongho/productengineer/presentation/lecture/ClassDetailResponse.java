package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "강의 상세 응답")
public record ClassDetailResponse(
		@Schema(description = "강의 ID", example = "1")
		Long id,

		@Schema(description = "크리에이터 ID", example = "10")
		Long creatorId,

		@Schema(description = "강의 제목", example = "Spring Boot 실전 클래스")
		String title,

		@Schema(description = "강의 상세 설명", example = "실무에서 사용하는 Spring Boot API 개발을 다룹니다.")
		String description,

		@Schema(description = "강의 모집 상태", example = "OPEN")
		ClassStatus status,

		@Schema(description = "수강료. KRW 원 단위 정수", example = "10000")
		long price,

		@Schema(description = "모집 정원", example = "30")
		int capacity,

		@Schema(description = "현재 신청 인원", example = "4")
		int currentEnrollment,

		@Schema(description = "강의 시작 일시", example = "2026-05-01T10:00:00")
		LocalDateTime startDate,

		@Schema(description = "강의 종료 일시", example = "2026-05-30T18:00:00")
		LocalDateTime endDate
) {
}
