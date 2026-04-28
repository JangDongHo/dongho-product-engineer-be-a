package io.github.jangdongho.productengineer.common.exception;

import lombok.Getter;
import org.springframework.lang.Nullable;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(ErrorCode errorCode, @Nullable String message) {
    super(message != null ? message : errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  public BusinessException(
      ErrorCode errorCode, @Nullable String message, @Nullable Throwable cause) {
    super(message != null ? message : errorCode.getDefaultMessage(), cause);
    this.errorCode = errorCode;
  }
}
