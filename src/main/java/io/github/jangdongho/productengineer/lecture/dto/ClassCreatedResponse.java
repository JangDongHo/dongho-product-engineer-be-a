package io.github.jangdongho.productengineer.lecture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "강의 생성 응답")
public record ClassCreatedResponse(
		@Schema(description = "생성된 강의 ID", example = "1")
		Long id
) {
}
