package io.github.jangdongho.productengineer.presentation.lecture;

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

import io.github.jangdongho.productengineer.business.lecture.LectureService;
import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.common.exception.GlobalExceptionHandler;
import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
		ClassListItemResponse item = new ClassListItemResponse(
				1L, 10L, "A", ClassStatus.DRAFT, 5_000L, 20,
				LocalDateTime.parse("2026-05-01T10:00:00"), LocalDateTime.parse("2026-05-30T18:00:00"));
		when(lectureService.listClasses(null)).thenReturn(List.of(item));

		mockMvc.perform(get("/classes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success", is(true)))
				.andExpect(jsonPath("$.data[0].id", is(1)))
				.andExpect(jsonPath("$.data[0].title", is("A")));
	}

	@Test
	@DisplayName("GET /classes?status=OPEN 는 필터된 목록을 반환한다")
	void getList_withStatus() throws Exception {
		ClassListItemResponse item = new ClassListItemResponse(
				2L, 1L, "B", ClassStatus.OPEN, 0L, 5,
				LocalDateTime.parse("2026-06-01T09:00:00"), LocalDateTime.parse("2026-06-20T12:00:00"));
		when(lectureService.listClasses(ClassStatus.OPEN)).thenReturn(List.of(item));

		mockMvc.perform(get("/classes").param("status", "OPEN"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].status", is("OPEN")));
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
