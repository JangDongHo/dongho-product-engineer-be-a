package io.github.jangdongho.productengineer.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

@Schema(description = "성공 응답 래퍼")
public record ApiResponse<T>(
    @Schema(description = "요청 성공 여부", example = "true") boolean success,
    @Nullable @Schema(description = "응답 데이터") T data,
    @Nullable @JsonInclude(JsonInclude.Include.NON_NULL) @Schema(description = "응답 메타데이터")
        Object meta) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data, null);
  }

  public static <T> ApiResponse<T> success(T data, Object meta) {
    return new ApiResponse<>(true, data, meta);
  }
}
