package io.github.jangdongho.productengineer.enrollment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "수강 신청 요청")
public class CreateEnrollmentRequest {

	@Schema(description = "신청 사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Positive
	private Long userId;

	@Schema(description = "강의 ID", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull
	@Positive
	private Long classId;
}
