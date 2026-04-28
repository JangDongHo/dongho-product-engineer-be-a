package io.github.jangdongho.productengineer.common.exception;

import org.springframework.lang.Nullable;

public class SystemException extends RuntimeException {

  public SystemException(String message) {
    super(message);
  }

  public SystemException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
