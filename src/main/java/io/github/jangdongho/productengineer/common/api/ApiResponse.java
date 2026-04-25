package io.github.jangdongho.productengineer.common.api;

import org.springframework.lang.Nullable;

public record ApiResponse<T>(boolean success, @Nullable T data) {

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(true, data);
	}
}
