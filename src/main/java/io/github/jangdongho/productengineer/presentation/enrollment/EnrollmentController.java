package io.github.jangdongho.productengineer.presentation.enrollment;

import io.github.jangdongho.productengineer.business.enrollment.EnrollmentService;
import io.github.jangdongho.productengineer.common.api.ApiResponse;
import io.github.jangdongho.productengineer.common.api.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Enrollments", description = "수강 신청 API")
public class EnrollmentController {

	private final EnrollmentService enrollmentService;

	@Operation(summary = "수강 신청", description = "모집 중인 강의에 수강 신청을 등록합니다. 상태는 PENDING이며, 신청 시점에 정원(currentEnrollment)이 반영됩니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "201",
					description = "수강 신청 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": {
									    "id": 1,
									    "status": "PENDING"
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "입력 검증 실패 또는 모집 중이 아닌 강의",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "강의를 찾을 수 없음",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "409",
					description = "정원 마감 또는 중복 신청",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PostMapping
	public ResponseEntity<ApiResponse<EnrollmentCreatedResponse>> enroll(@Valid @RequestBody CreateEnrollmentRequest request) {
		EnrollmentCreatedResponse response = enrollmentService.enroll(request.getUserId(), request.getClassId());
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	@Operation(
			summary = "결제 확정",
			description = "PENDING 수강 신청을 CONFIRMED로 전이하고 결제 확정 시각을 기록합니다. 정원(currentEnrollment)은 신청 시 이미 반영되므로 변경하지 않습니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "결제 확정 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": {
									    "id": 1,
									    "status": "CONFIRMED",
									    "confirmedAt": "2026-05-01T12:00:00"
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "PENDING이 아닌 신청에 대한 확정 시도",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "수강 신청을 찾을 수 없음",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PatchMapping("/{id}/confirm")
	public ResponseEntity<ApiResponse<EnrollmentConfirmedResponse>> confirm(
			@Parameter(description = "수강 신청 ID", example = "1")
			@PathVariable long id) {
		EnrollmentConfirmedResponse response = enrollmentService.confirm(id);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
