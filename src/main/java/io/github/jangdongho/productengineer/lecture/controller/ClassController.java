package io.github.jangdongho.productengineer.lecture.controller;

import io.github.jangdongho.productengineer.common.api.ApiResponse;
import io.github.jangdongho.productengineer.common.api.ErrorResponse;
import io.github.jangdongho.productengineer.common.api.PageMeta;
import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.dto.ClassCreatedResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassDetailResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassListItemResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassStatusResponse;
import io.github.jangdongho.productengineer.lecture.dto.CreateClassRequest;
import io.github.jangdongho.productengineer.lecture.dto.UpdateClassStatusRequest;
import io.github.jangdongho.productengineer.enrollment.dto.ClassConfirmedEnrollmentItemResponse;
import io.github.jangdongho.productengineer.lecture.service.LectureService;
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
@RequestMapping("/classes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Classes", description = "강의 생성, 조회, 상태 변경 API")
public class ClassController {

	private static final int DEFAULT_PAGE = 0;
	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_PAGE = 10_000;
	private static final int MAX_SIZE = 100;

	private final LectureService lectureService;

	@Operation(summary = "강의 목록 조회", description = "강의 목록을 생성일 최신순으로 페이지 조회합니다. status를 전달하면 해당 모집 상태로 필터링합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "강의 목록 조회 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": [
									    {
									      "id": 1,
									      "creatorId": 10,
									      "title": "Spring Boot 실전 클래스",
									      "status": "OPEN",
									      "price": 10000,
									      "capacity": 30,
									      "startDate": "2026-05-01T10:00:00",
									      "endDate": "2026-05-30T18:00:00"
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
					description = "잘못된 status 값",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@GetMapping
	public ResponseEntity<ApiResponse<List<ClassListItemResponse>>> list(
			@Parameter(description = "강의 모집 상태 필터", example = "OPEN")
			@RequestParam(required = false) ClassStatus status,
			@Parameter(description = "페이지 번호(0~10000)", example = "0")
			@RequestParam(defaultValue = "" + DEFAULT_PAGE) @PositiveOrZero @Max(MAX_PAGE) int page,
			@Parameter(description = "페이지 크기(1~100)", example = "20")
			@RequestParam(defaultValue = "" + DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		Page<ClassListItemResponse> response = lectureService.listClasses(status, PageRequest.of(page, size));
		return ResponseEntity.ok(ApiResponse.success(response.getContent(), PageMeta.from(response)));
	}

	@Operation(summary = "강의 상세 조회", description = "강의 ID로 상세 정보를 조회합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "강의 상세 조회 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": {
									    "id": 1,
									    "creatorId": 10,
									    "title": "Spring Boot 실전 클래스",
									    "description": "실무에서 사용하는 Spring Boot API 개발을 다룹니다.",
									    "status": "OPEN",
									    "price": 10000,
									    "capacity": 30,
									    "currentEnrollment": 4,
									    "startDate": "2026-05-01T10:00:00",
									    "endDate": "2026-05-30T18:00:00"
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "강의를 찾을 수 없음",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@GetMapping("/{id}")
	public ResponseEntity<ApiResponse<ClassDetailResponse>> getById(
			@Parameter(description = "강의 ID", example = "1")
			@PathVariable long id) {
		return ResponseEntity.ok(ApiResponse.success(lectureService.getClassById(id)));
	}

	@Operation(
			summary = "강의별 확정 수강생 목록",
			description = "강의 소유자(creatorId)가 해당 강의의 CONFIRMED 수강 신청만 조회합니다. 강의가 없으면 404, 소유자가 아니면 403입니다."
	)
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "조회 성공 (확정 수강생 없으면 빈 배열)",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": [
									    {
									      "enrollmentId": 1,
									      "userId": 100,
									      "status": "CONFIRMED",
									      "confirmedAt": "2026-05-01T12:00:00"
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
					description = "creatorId 누락 또는 유효하지 않은 값",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "403",
					description = "강의 소유자가 아님",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "강의를 찾을 수 없음",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@GetMapping("/{id}/enrollments")
	public ResponseEntity<ApiResponse<List<ClassConfirmedEnrollmentItemResponse>>> listConfirmedEnrollments(
			@Parameter(description = "강의 ID", example = "1")
			@PathVariable long id,
			@Parameter(description = "강의 소유자(크리에이터) ID", example = "10", required = true)
			@RequestParam("creatorId") @NotNull @Positive Long creatorId,
			@Parameter(description = "페이지 번호(0~10000)", example = "0")
			@RequestParam(name = "page", defaultValue = "" + DEFAULT_PAGE) @PositiveOrZero @Max(MAX_PAGE) int page,
			@Parameter(description = "페이지 크기(1~100)", example = "20")
			@RequestParam(name = "size", defaultValue = "" + DEFAULT_SIZE) @Positive @Max(MAX_SIZE) int size) {
		Page<ClassConfirmedEnrollmentItemResponse> response = lectureService.listConfirmedEnrollmentsForCreator(
				id,
				creatorId,
				PageRequest.of(page, size));
		return ResponseEntity.ok(ApiResponse.success(response.getContent(), PageMeta.from(response)));
	}

	@Operation(summary = "강의 생성", description = "크리에이터 ID와 강의 정보를 받아 강의를 DRAFT 상태로 생성합니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "201",
					description = "강의 생성 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": {
									    "id": 1
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "요청 값 검증 실패",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PostMapping
	public ResponseEntity<ApiResponse<ClassCreatedResponse>> create(
			@Parameter(description = "크리에이터 ID", example = "10", required = true)
			@RequestParam("creatorId") @NotNull @Positive Long creatorId,
			@Valid @RequestBody CreateClassRequest request) {
		ClassCreatedResponse response = lectureService.create(creatorId, request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(response));
	}

	@Operation(summary = "강의 상태 변경", description = "강의 상태를 변경합니다. 허용 전이는 DRAFT -> OPEN -> CLOSED 입니다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "강의 상태 변경 성공",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ApiResponse.class),
							examples = @ExampleObject(value = """
									{
									  "success": true,
									  "data": {
									    "status": "OPEN"
									  }
									}
									""")
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "요청 값 검증 실패 또는 허용되지 않는 상태 전이",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "강의를 찾을 수 없음",
					content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))
			)
	})
	@PatchMapping("/{id}/status")
	public ResponseEntity<ApiResponse<ClassStatusResponse>> updateStatus(
			@Parameter(description = "강의 ID", example = "1")
			@PathVariable long id,
			@Valid @RequestBody UpdateClassStatusRequest request) {
		ClassStatusResponse response = lectureService.updateStatus(id, request.status());
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
