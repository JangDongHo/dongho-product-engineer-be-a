package io.github.jangdongho.productengineer.common.exception;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.jangdongho.productengineer.common.api.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
		ErrorCode errorCode = e.getErrorCode();
		ErrorResponse body = new ErrorResponse(errorCode.getCode(), e.getMessage());
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}

	@ExceptionHandler(SystemException.class)
	public ResponseEntity<ErrorResponse> handleSystemException(SystemException e) {
		log.error("SystemException", e);
		ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
		ErrorResponse body = new ErrorResponse(errorCode.getCode(), errorCode.getDefaultMessage());
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
		String message = e.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining(", "));
		if (message.isEmpty()) {
			message = ErrorCode.VALIDATION_ERROR.getDefaultMessage();
		}
		ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
		ErrorResponse body = new ErrorResponse(errorCode.getCode(), message);
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unhandled exception", e);
		ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
		ErrorResponse body = new ErrorResponse(errorCode.getCode(), errorCode.getDefaultMessage());
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}
}
