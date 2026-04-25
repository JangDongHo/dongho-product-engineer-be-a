package io.github.jangdongho.productengineer.common.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/__test")
public class TestExceptionController {

	@GetMapping("/business")
	public void business() {
		throw new BusinessException(ErrorCode.NOT_FOUND, "custom not found");
	}

	@GetMapping("/system")
	public void system() {
		throw new SystemException("internal detail");
	}

	@GetMapping("/unexpected")
	public void unexpected() {
		throw new IllegalStateException("unexpected");
	}

	@PostMapping("/validate")
	public void validate(@Valid @RequestBody TestRequest request) {
		// validated only
	}

	public record TestRequest(@NotBlank String name) {
	}
}
