package io.github.jangdongho.productengineer.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
  VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력 값이 올바르지 않습니다."),

  NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "리소스를 찾을 수 없습니다."),

  FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 리소스에 접근할 권한이 없습니다."),

  CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "요청이 현재 리소스 상태와 충돌합니다."),

  INTERNAL_ERROR(
      HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");

  private final HttpStatus status;
  private final String code;
  private final String defaultMessage;
}
