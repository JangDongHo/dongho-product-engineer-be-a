package io.github.jangdongho.productengineer.enrollment.controller;

import io.github.jangdongho.productengineer.common.api.ApiResponse;
import io.github.jangdongho.productengineer.common.api.ErrorResponse;
import io.github.jangdongho.productengineer.common.api.PageMeta;
import io.github.jangdongho.productengineer.enrollment.dto.CreateEnrollmentRequest;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentCancelledResponse;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentConfirmedResponse;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentCreatedResponse;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentListItemResponse;
import io.github.jangdongho.productengineer.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
@RequestMapping("/enrollments")
@RequiredArgsConstructor
@Validated
@Tag(name = "Enrollments", description = "수강 신청 API")
public class EnrollmentController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final EnrollmentService enrollmentService;

	@Operation(
			summary = "내 수강 신청 목록",
			description = "특정 사용자의 수강 신청을 생성일 최신순으로 페이지 조회합니다. 각 항목에 강의 요약(강의 목록 항목과 동일)과 신청 상태가 포함됩니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "조회 성공 (신청이 없으면 빈 배열)",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": [
									    {
									      "enrollmentId": 1,
									      "status": "PENDING",
									      "confirmedAt": null,
									      "lecture": {
									        "id": 10,
									        "creatorId": 2,
									        "title": "Spring Boot 실전 클래스",
									        "status": "OPEN",
									        "price": 10000,
									        "capacity": 30,
									        "startDate": "2026-05-01T10:00:00",
									        "endDate": "2026-05-30T18:00:00"
									      }
									    }
									  ],
									  "meta": {
									    "page": 0,
									    "size": 20,
									    "totalElements": 1,
									    "totalPages": 1,
									    "hasNext": false,
									    "hasPrevious": false
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "userId 누락·양수가 아님",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "수강이 참조하는 강의가 없음(데이터 불일치)",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@GetMapping
	public ResponseEntity<ApiResponse<List<EnrollmentListItemResponse>>> list(
			@Parameter(description = "사용자 ID", example = "1", required = true)
			@RequestParam("userId") @NotNull @Positive Long userId,
			@Parameter(description = "페이지 번호(0부터 시작)", example = "0")
			@RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) @PositiveOrZero int page,
			@Parameter(description = "페이지 크기(1~100)", example = "20")
			@RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		Page<EnrollmentListItemResponse> response = enrollmentService.listByUserId(userId, PageRequest.of(page, size));
		return ResponseEntity.ok(ApiResponse.success(response.getContent(), PageMeta.from(response)));
	}

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

	@Operation(
			summary = "수강 취소",
			description = "PENDING은 제한 없이 취소합니다. CONFIRMED는 결제 확정 시각(confirmedAt) 기준 7일 이내에만 취소할 수 있습니다. 성공 시 강의 정원(currentEnrollment)이 1 감소합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "취소 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": {
									    "id": 1,
									    "status": "CANCELLED"
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "이미 취소됨, 확정 후 7일 초과 등 취소 불가",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "수강 신청 또는 강의를 찾을 수 없음",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PatchMapping("/{id}/cancel")
	public ResponseEntity<ApiResponse<EnrollmentCancelledResponse>> cancel(
			@Parameter(description = "수강 신청 ID", example = "1")
			@PathVariable long id) {
		EnrollmentCancelledResponse response = enrollmentService.cancel(id);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
