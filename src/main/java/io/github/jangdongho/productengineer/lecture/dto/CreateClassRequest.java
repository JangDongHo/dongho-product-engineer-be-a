package io.github.jangdongho.productengineer.lecture.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "강의 생성 요청")
public class CreateClassRequest {

	@Schema(description = "강의 제목", example = "Spring Boot 실전 클래스", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	@Size(max = 255)
	private String title;

	@Schema(description = "강의 상세 설명", example = "실무에서 사용하는 Spring Boot API 개발을 다룹니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank
	private String description;

	@Schema(description = "수강료. KRW 원 단위 정수", example = "10000", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Positive
	private Long price;

	@Schema(description = "모집 정원", example = "30", minimum = "1", maximum = "10000", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Min(1)
	@Max(10_000)
	private Integer capacity;

	@Schema(description = "강의 시작 일시", example = "2026-05-01T10:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private LocalDateTime startDate;

	@Schema(description = "강의 종료 일시. 시작 일시보다 이후여야 합니다.", example = "2026-05-30T18:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	private LocalDateTime endDate;

	@Schema(hidden = true)
	@AssertTrue(message = "must be after startDate")
	public boolean isValidDateRange() {
		if (startDate == null || endDate == null) {
			return true;
		}
		return endDate.isAfter(startDate);
	}
}
