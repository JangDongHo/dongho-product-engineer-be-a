package io.github.jangdongho.productengineer.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import org.springframework.lang.Nullable;

@Schema(description = "페이지네이션 메타데이터")
public record PageMeta(
    @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0") int page,
    @Schema(description = "페이지 크기", example = "20") int size,
    @Schema(description = "전체 건수", example = "42") long totalElements,
    @Schema(description = "전체 페이지 수", example = "3") int totalPages,
    @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext,
    @Schema(description = "이전 페이지 존재 여부", example = "false") boolean hasPrevious,
    @Nullable
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "다음 페이지 번호(없으면 null)", example = "1")
        Integer nextPage) {

  public static PageMeta from(Page<?> page) {
    return new PageMeta(
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext(),
        page.hasPrevious(),
        page.hasNext() ? page.getNumber() + 1 : null);
  }
}
