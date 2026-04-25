package io.github.jangdongho.productengineer.common.api;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

@Schema(description = "성공 응답 래퍼")
public record ApiResponse<T>(
		@Schema(description = "요청 성공 여부", example = "true")
		boolean success,

		@Nullable
		@Schema(description = "응답 데이터")
		T data
) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data);
	}
}
