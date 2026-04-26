package io.github.jangdongho.productengineer.common.exception;

import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
		ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
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

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
		ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
		String message = "Required request parameter is missing: " + e.getParameterName();
		ErrorResponse body = new ErrorResponse(errorCode.getCode(), message);
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
		String message = e.getConstraintViolations().stream()
				.map(v -> v.getPropertyPath() + ": " + v.getMessage())
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
