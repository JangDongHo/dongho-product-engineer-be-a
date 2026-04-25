package io.github.jangdongho.productengineer.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "에러 응답")
public record ErrorResponse(
		@Schema(description = "에러 코드", example = "VALIDATION_ERROR")
		String code,

		@Schema(description = "에러 메시지", example = "입력 값이 올바르지 않습니다.")
		String message
) {
}
