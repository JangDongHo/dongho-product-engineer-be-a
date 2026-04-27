package io.github.jangdongho.productengineer.lecture.controller;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.common.exception.GlobalExceptionHandler;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.github.jangdongho.productengineer.enrollment.dto.ClassConfirmedEnrollmentItemResponse;
import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.dto.ClassCreatedResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassDetailResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassListItemResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassStatusResponse;
import io.github.jangdongho.productengineer.lecture.dto.CreateClassRequest;
import io.github.jangdongho.productengineer.lecture.service.LectureService;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ClassController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class ClassControllerTest {

	@MockitoBean
	private LectureService lectureService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("GET /classes 는 전체 목록을 반환한다")
	void getList_all() throws Exception {
		PageRequest pageable = PageRequest.of(0, 20);
		ClassListItemResponse item = new ClassListItemResponse(
				1L, 10L, "A", ClassStatus.DRAFT, 5_000L, 20,
				LocalDateTime.parse("2026-05-01T10:00:00"), LocalDateTime.parse("2026-05-30T18:00:00"));
		when(lectureService.listClasses(null, pageable))
				.thenReturn(new PageImpl<>(List.of(item), pageable, 1));

		mockMvc.perform(get("/classes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data[0].id", is(1)))
				.andExpect(jsonPath("$.data[0].title", is("A")))
				.andExpect(jsonPath("$.meta.page", is(0)))
				.andExpect(jsonPath("$.meta.size", is(20)))
				.andExpect(jsonPath("$.meta.totalElements", is(1)))
				.andExpect(jsonPath("$.meta.hasNext", is(false)));
	}

	@Test
	@DisplayName("GET /classes?status=OPEN 는 필터된 목록을 반환한다")
	void getList_withStatus() throws Exception {
		PageRequest pageable = PageRequest.of(0, 5);
		ClassListItemResponse item = new ClassListItemResponse(
				2L, 1L, "B", ClassStatus.OPEN, 0L, 5,
				LocalDateTime.parse("2026-06-01T09:00:00"), LocalDateTime.parse("2026-06-20T12:00:00"));
		when(lectureService.listClasses(ClassStatus.OPEN, pageable))
				.thenReturn(new PageImpl<>(List.of(item), pageable, 12));

		mockMvc.perform(get("/classes")
						.param("status", "OPEN")
						.param("page", "0")
						.param("size", "5"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].status", is("OPEN")))
				.andExpect(jsonPath("$.meta.totalElements", is(12)))
				.andExpect(jsonPath("$.meta.totalPages", is(3)))
				.andExpect(jsonPath("$.meta.nextPage", is(1)));
	}

	@Test
	@DisplayName("GET /classes 는 page 음수 또는 size 초과면 400 을 반환한다")
	void getList_invalidPagination_returns400() throws Exception {
		mockMvc.perform(get("/classes")
						.param("page", "-1")
						.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /classes 는 page 가 상한(10000) 초과면 400 을 반환한다")
	void getList_pageExceedsMax_returns400() throws Exception {
		mockMvc.perform(get("/classes")
						.param("page", "10001")
						.param("size", "20"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id} 는 상세와 currentEnrollment 를 반환한다")
	void getById_returnsDetail() throws Exception {
		ClassDetailResponse body = new ClassDetailResponse(
				1L, 3L, "C", "desc", ClassStatus.OPEN, 9_000L, 10, 4,
				LocalDateTime.parse("2026-07-01T10:00:00"), LocalDateTime.parse("2026-07-31T18:00:00"));
		when(lectureService.getClassById(1L)).thenReturn(body);

		mockMvc.perform(get("/classes/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentEnrollment", is(4)))
				.andExpect(jsonPath("$.data.description", is("desc")));
	}

	@Test
	@DisplayName("GET /classes/{id} 는 강의 없을 때 404 를 반환한다")
	void getById_notFound_returns404() throws Exception {
		when(lectureService.getClassById(99L))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		mockMvc.perform(get("/classes/99"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments?creatorId= 는 소유자·확정 수강생을 반환한다")
	void getEnrollments_confirmedList_returns200() throws Exception {
		PageRequest pageable = PageRequest.of(0, 20);
		ClassConfirmedEnrollmentItemResponse row = new ClassConfirmedEnrollmentItemResponse(
				1L, 100L, EnrollmentStatus.CONFIRMED, LocalDateTime.parse("2026-05-01T12:00:00"));
		when(lectureService.listConfirmedEnrollmentsForCreator(1L, 10L, pageable))
				.thenReturn(new PageImpl<>(List.of(row), pageable, 1));

		mockMvc.perform(get("/classes/1/enrollments").param("creatorId", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data[0].enrollmentId", is(1)))
				.andExpect(jsonPath("$.data[0].userId", is(100)))
				.andExpect(jsonPath("$.data[0].status", is("CONFIRMED")))
				.andExpect(jsonPath("$.meta.totalElements", is(1)));
		verify(lectureService).listConfirmedEnrollmentsForCreator(1L, 10L, pageable);
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 확정 수강생 없을 때 200 빈 배열")
	void getEnrollments_empty_returns200() throws Exception {
		PageRequest pageable = PageRequest.of(0, 20);
		when(lectureService.listConfirmedEnrollmentsForCreator(2L, 1L, pageable))
				.thenReturn(new PageImpl<>(List.of(), pageable, 0));

		mockMvc.perform(get("/classes/2/enrollments").param("creatorId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.length()", is(0)))
				.andExpect(jsonPath("$.meta.totalElements", is(0)))
				.andExpect(jsonPath("$.meta.totalPages", is(0)));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 강의 없을 때 404")
	void getEnrollments_notFound_returns404() throws Exception {
		when(lectureService.listConfirmedEnrollmentsForCreator(99L, 1L, PageRequest.of(0, 20)))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		mockMvc.perform(get("/classes/99/enrollments").param("creatorId", "1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 비소유자 403")
	void getEnrollments_forbidden_returns403() throws Exception {
		when(lectureService.listConfirmedEnrollmentsForCreator(1L, 2L, PageRequest.of(0, 20)))
				.thenThrow(new BusinessException(ErrorCode.FORBIDDEN));

		mockMvc.perform(get("/classes/1/enrollments").param("creatorId", "2"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", is(ErrorCode.FORBIDDEN.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 creatorId 누락 시 400")
	void getEnrollments_missingCreatorId_returns400() throws Exception {
		mockMvc.perform(get("/classes/1/enrollments"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 creatorId 가 음수면 400")
	void getEnrollments_invalidCreatorId_returns400() throws Exception {
		mockMvc.perform(get("/classes/1/enrollments").param("creatorId", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 page 음수 또는 size 초과면 400")
	void getEnrollments_invalidPagination_returns400() throws Exception {
		mockMvc.perform(get("/classes/1/enrollments")
						.param("creatorId", "1")
						.param("page", "-1")
						.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /classes/{id}/enrollments 는 page 가 상한(10000) 초과면 400")
	void getEnrollments_pageExceedsMax_returns400() throws Exception {
		mockMvc.perform(get("/classes/1/enrollments")
						.param("creatorId", "1")
						.param("page", "10001")
						.param("size", "20"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("POST /classes 는 유효한 요청 시 201 Created 와 id 를 반환한다")
	void create_returns201WithId() throws Exception {
		when(lectureService.create(eq(10L), any(CreateClassRequest.class)))
				.thenReturn(new ClassCreatedResponse(1L));

		String body = """
				{
					"title": "Test class",
					"description": "Description",
					"price": 10000,
					"capacity": 30,
					"startDate": "2026-05-01T10:00:00",
					"endDate": "2026-05-30T18:00:00"
				}
				""";
		mockMvc.perform(post("/classes")
						.param("creatorId", "10")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.id", is(1)));
		verify(lectureService).create(eq(10L), any(CreateClassRequest.class));
	}

	@Test
	@DisplayName("POST /classes 는 Bean Validation 위반 시 400 과 VALIDATION_ERROR code 를 반환한다")
	void create_invalidBody_returns400() throws Exception {
		String body = """
				{
					"title": "",
					"description": "x",
					"price": -1,
					"capacity": 0,
					"startDate": "2026-05-30T10:00:00",
					"endDate": "2026-05-01T10:00:00"
				}
				""";
		mockMvc.perform(post("/classes")
						.param("creatorId", "1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("POST /classes 는 creatorId 가 음수면 400 을 반환한다")
	void create_invalidCreatorId_returns400() throws Exception {
		String body = """
				{
					"title": "Test class",
					"description": "Description",
					"price": 10000,
					"capacity": 30,
					"startDate": "2026-05-01T10:00:00",
					"endDate": "2026-05-30T18:00:00"
				}
				""";
		mockMvc.perform(post("/classes")
						.param("creatorId", "-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("PATCH /classes/{id}/status 는 성공 시 200 과 status 를 반환한다")
	void patchStatus_returns200() throws Exception {
		when(lectureService.updateStatus(eq(1L), eq(ClassStatus.OPEN)))
				.thenReturn(new ClassStatusResponse(ClassStatus.OPEN));

		String body = """
				{ "status": "OPEN" }
				""";
		mockMvc.perform(patch("/classes/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.status", is("OPEN")));
		verify(lectureService).updateStatus(1L, ClassStatus.OPEN);
	}

	@Test
	@DisplayName("PATCH /classes/{id}/status 는 서비스가 전이 거부 시 400 을 반환한다")
	void patchStatus_invalidTransition_returns400() throws Exception {
		when(lectureService.updateStatus(eq(1L), eq(ClassStatus.CLOSED)))
				.thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 상태 전이입니다."));

		String body = """
				{ "status": "CLOSED" }
				""";
		mockMvc.perform(patch("/classes/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("PATCH /classes/{id}/status 는 강의 없을 때 404 를 반환한다")
	void patchStatus_notFound_returns404() throws Exception {
		when(lectureService.updateStatus(eq(1L), eq(ClassStatus.OPEN)))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		String body = """
				{ "status": "OPEN" }
				""";
		mockMvc.perform(patch("/classes/1/status")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}
}
