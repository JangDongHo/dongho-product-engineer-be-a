package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.business.lecture.LectureService;
import io.github.jangdongho.productengineer.common.api.ApiResponse;
import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@GetMapping
	public ResponseEntity<ApiResponse<List<ClassListItemResponse>>> list(
			@RequestParam(name = "status", required = false) ClassStatus status) {
		return ResponseEntity.ok(ApiResponse.success(lectureService.listClasses(status)));
	}

	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ClassDetailResponse>> getById(@PathVariable long id) {
		return ResponseEntity.ok(ApiResponse.success(lectureService.getClassById(id)));
	}

	@PostMapping
	public ResponseEntity<ApiResponse<ClassCreatedResponse>> create(
			@RequestParam("creatorId") @NotNull @Positive Long creatorId,
			@Valid @RequestBody CreateClassRequest request) {
		ClassCreatedResponse response = lectureService.create(creatorId, request);
		
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response));
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<ClassStatusResponse>> updateStatus(
			@PathVariable long id,
			@Valid @RequestBody UpdateClassStatusRequest request) {
		ClassStatusResponse response = lectureService.updateStatus(id, request.status());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
