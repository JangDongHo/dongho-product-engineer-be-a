package io.github.jangdongho.productengineer.common.exception;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TestExceptionController.class)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("BusinessException 응답에 code, message와 HTTP 상태가 ErrorCode와 일치한다")
  void businessException_returnsCodeMessageAndStatus() throws Exception {
    mockMvc
        .perform(get("/__test/business"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code", is("NOT_FOUND")))
        .andExpect(jsonPath("$.message", is("custom not found")));
  }

  @Test
  @DisplayName("SystemException 은 500과 INTERNAL_ERROR code, 일반화된 message 를 반환한다")
  void systemException_returnsInternalError() throws Exception {
    mockMvc
        .perform(get("/__test/system"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
        .andExpect(jsonPath("$.message", is(ErrorCode.INTERNAL_ERROR.getDefaultMessage())));
  }

  @Test
  @DisplayName("MethodArgumentNotValidException 은 400과 VALIDATION_ERROR code 를 반환한다")
  void validation_returnsValidationError() throws Exception {
    mockMvc
        .perform(post("/__test/validate").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
  }

  @Test
  @DisplayName("예상치 못한 예외는 500과 INTERNAL_ERROR 를 반환한다")
  void unexpectedException_returnsInternalError() throws Exception {
    mockMvc
        .perform(get("/__test/unexpected"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code", is("INTERNAL_ERROR")))
        .andExpect(jsonPath("$.message", is(ErrorCode.INTERNAL_ERROR.getDefaultMessage())));
  }
}
