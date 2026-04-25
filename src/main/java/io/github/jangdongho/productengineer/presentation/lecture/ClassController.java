package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.business.lecture.LectureService;
import io.github.jangdongho.productengineer.common.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Validated
public class ClassController {

	private final LectureService lectureService;

	@PostMapping
	public ResponseEntity<ApiResponse<ClassCreatedResponse>> create(
			@RequestParam("creatorId") @NotNull @Positive Long creatorId,
			@Valid @RequestBody CreateClassRequest request) {
		ClassCreatedResponse response = lectureService.create(creatorId, request);
		
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response));
	}
}
