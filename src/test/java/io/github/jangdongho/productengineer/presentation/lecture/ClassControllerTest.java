package io.github.jangdongho.productengineer.presentation.lecture;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.jangdongho.productengineer.business.lecture.LectureService;
import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.common.exception.GlobalExceptionHandler;
import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
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
