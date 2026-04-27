package io.github.jangdongho.productengineer.enrollment.controller;

import static org.hamcrest.Matchers.is;
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
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentCancelledResponse;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentConfirmedResponse;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentCreatedResponse;
import io.github.jangdongho.productengineer.enrollment.dto.EnrollmentListItemResponse;
import io.github.jangdongho.productengineer.enrollment.service.EnrollmentService;
import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.dto.ClassListItemResponse;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EnrollmentController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class EnrollmentControllerTest {

	@MockitoBean
	private EnrollmentService enrollmentService;

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("POST /enrollments 는 성공 시 201 과 id, PENDING status 를 반환한다")
	void enroll_returns201() throws Exception {
		when(enrollmentService.enroll(eq(1L), eq(10L)))
				.thenReturn(new EnrollmentCreatedResponse(5L, EnrollmentStatus.PENDING));

		String body = """
				{ "userId": 1, "classId": 10 }
				""";
		mockMvc.perform(post("/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.id", is(5)))
				.andExpect(jsonPath("$.data.status", is("PENDING")));
		verify(enrollmentService).enroll(1L, 10L);
	}

	@Test
	@DisplayName("POST /enrollments 는 모집 중이 아닌 강의면 400 을 반환한다")
	void enroll_notOpen_returns400() throws Exception {
		when(enrollmentService.enroll(eq(1L), eq(2L)))
				.thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "모집 중인 강의만 신청할 수 있습니다."));

		String body = """
				{ "userId": 1, "classId": 2 }
				""";
		mockMvc.perform(post("/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("POST /enrollments 는 강의 없을 때 404 를 반환한다")
	void enroll_notFound_returns404() throws Exception {
		when(enrollmentService.enroll(eq(1L), eq(99L)))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		String body = """
				{ "userId": 1, "classId": 99 }
				""";
		mockMvc.perform(post("/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}

	@Test
	@DisplayName("POST /enrollments 는 중복 신청 시 409 를 반환한다")
	void enroll_duplicate_returns409() throws Exception {
		when(enrollmentService.enroll(eq(1L), eq(3L)))
				.thenThrow(new BusinessException(ErrorCode.CONFLICT, "이미 신청한 강의입니다."));

		String body = """
				{ "userId": 1, "classId": 3 }
				""";
		mockMvc.perform(post("/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is(ErrorCode.CONFLICT.getCode())));
	}

	@Test
	@DisplayName("POST /enrollments 는 정원 마감 시 409 를 반환한다")
	void enroll_full_returns409() throws Exception {
		when(enrollmentService.enroll(eq(1L), eq(4L)))
				.thenThrow(new BusinessException(ErrorCode.CONFLICT, "정원이 마감되었습니다."));

		String body = """
				{ "userId": 1, "classId": 4 }
				""";
		mockMvc.perform(post("/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is(ErrorCode.CONFLICT.getCode())));
	}

	@Test
	@DisplayName("GET /enrollments?userId= 는 성공 시 200 과 목록을 반환한다")
	void list_returns200() throws Exception {
		PageRequest pageable = PageRequest.of(1, 2);
		ClassListItemResponse lecture = new ClassListItemResponse(
				10L, 2L, "Spring Boot", ClassStatus.OPEN, 10_000L, 30,
				LocalDateTime.parse("2026-05-01T10:00:00"), LocalDateTime.parse("2026-05-30T18:00:00"));
		when(enrollmentService.listByUserId(1L, pageable))
				.thenReturn(new PageImpl<>(
						List.of(new EnrollmentListItemResponse(5L, EnrollmentStatus.PENDING, null, lecture)),
						pageable,
						3));

		mockMvc.perform(get("/enrollments")
						.param("userId", "1")
						.param("page", "1")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data[0].enrollmentId", is(5)))
				.andExpect(jsonPath("$.data[0].status", is("PENDING")))
				.andExpect(jsonPath("$.data[0].confirmedAt").doesNotExist())
				.andExpect(jsonPath("$.data[0].lecture.id", is(10)))
				.andExpect(jsonPath("$.data[0].lecture.title", is("Spring Boot")))
				.andExpect(jsonPath("$.meta.page", is(1)))
				.andExpect(jsonPath("$.meta.size", is(2)))
				.andExpect(jsonPath("$.meta.totalElements", is(3)))
				.andExpect(jsonPath("$.meta.totalPages", is(2)))
				.andExpect(jsonPath("$.meta.hasNext", is(false)))
				.andExpect(jsonPath("$.meta.hasPrevious", is(true)))
				.andExpect(jsonPath("$.meta.nextPage").doesNotExist());
		verify(enrollmentService).listByUserId(1L, pageable);
	}

	@Test
	@DisplayName("GET /enrollments?userId= 는 userId 없으면 400 을 반환한다")
	void list_missingUserId_returns400() throws Exception {
		mockMvc.perform(get("/enrollments"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /enrollments?userId= 는 userId가 양수가 아니면 400 을 반환한다")
	void list_nonPositiveUserId_returns400() throws Exception {
		mockMvc.perform(get("/enrollments").param("userId", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /enrollments 는 page 음수 또는 size 초과면 400 을 반환한다")
	void list_invalidPagination_returns400() throws Exception {
		mockMvc.perform(get("/enrollments")
						.param("userId", "1")
						.param("page", "-1")
						.param("size", "101"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /enrollments 는 page 가 상한(10000) 초과면 400 을 반환한다")
	void list_pageExceedsMax_returns400() throws Exception {
		mockMvc.perform(get("/enrollments")
						.param("userId", "1")
						.param("page", "10001")
						.param("size", "20"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("GET /enrollments?userId= 는 데이터 불일치 시 404 를 반환한다")
	void list_lectureMissing_returns404() throws Exception {
		when(enrollmentService.listByUserId(1L, PageRequest.of(0, 20)))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		mockMvc.perform(get("/enrollments").param("userId", "1"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}

	@Test
	@DisplayName("POST /enrollments 는 Bean Validation 위반 시 400 을 반환한다")
	void enroll_invalidBody_returns400() throws Exception {
		String body = """
				{ "userId": -1, "classId": 1 }
				""";
		mockMvc.perform(post("/enrollments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("PATCH /enrollments/{id}/confirm 는 성공 시 200 과 id, CONFIRMED, confirmedAt 를 반환한다")
	void confirm_returns200() throws Exception {
		LocalDateTime at = LocalDateTime.parse("2026-05-01T12:00:00");
		when(enrollmentService.confirm(1L))
				.thenReturn(new EnrollmentConfirmedResponse(1L, EnrollmentStatus.CONFIRMED, at));

		mockMvc.perform(patch("/enrollments/1/confirm"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.id", is(1)))
				.andExpect(jsonPath("$.data.status", is("CONFIRMED")))
				.andExpect(jsonPath("$.data.confirmedAt", is("2026-05-01T12:00:00")));
		verify(enrollmentService).confirm(1L);
	}

	@Test
	@DisplayName("PATCH /enrollments/{id}/confirm 는 PENDING 이 아닐 때 400 을 반환한다")
	void confirm_notPending_returns400() throws Exception {
		when(enrollmentService.confirm(2L))
				.thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "결제 대기(PENDING) 상태의 신청만 확정할 수 있습니다."));

		mockMvc.perform(patch("/enrollments/2/confirm"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("PATCH /enrollments/{id}/confirm 는 신청이 없을 때 404 를 반환한다")
	void confirm_notFound_returns404() throws Exception {
		when(enrollmentService.confirm(99L))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		mockMvc.perform(patch("/enrollments/99/confirm"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}

	@Test
	@DisplayName("PATCH /enrollments/{id}/cancel 는 성공 시 200 과 id, CANCELLED 를 반환한다")
	void cancel_returns200() throws Exception {
		when(enrollmentService.cancel(1L))
				.thenReturn(new EnrollmentCancelledResponse(1L, EnrollmentStatus.CANCELLED));

		mockMvc.perform(patch("/enrollments/1/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data.id", is(1)))
				.andExpect(jsonPath("$.data.status", is("CANCELLED")));
		verify(enrollmentService).cancel(1L);
	}

	@Test
	@DisplayName("PATCH /enrollments/{id}/cancel 는 취소 불가 시 400 을 반환한다")
	void cancel_validationError_returns400() throws Exception {
		when(enrollmentService.cancel(2L))
				.thenThrow(new BusinessException(ErrorCode.VALIDATION_ERROR, "결제 확정 후 7일이 지나 취소할 수 없습니다."));

		mockMvc.perform(patch("/enrollments/2/cancel"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", is(ErrorCode.VALIDATION_ERROR.getCode())));
	}

	@Test
	@DisplayName("PATCH /enrollments/{id}/cancel 는 신청이 없을 때 404 를 반환한다")
	void cancel_notFound_returns404() throws Exception {
		when(enrollmentService.cancel(99L))
				.thenThrow(new BusinessException(ErrorCode.NOT_FOUND));

		mockMvc.perform(patch("/enrollments/99/cancel"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", is(ErrorCode.NOT_FOUND.getCode())));
	}
}
