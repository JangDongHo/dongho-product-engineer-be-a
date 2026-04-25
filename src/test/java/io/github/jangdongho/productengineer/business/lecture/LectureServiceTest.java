package io.github.jangdongho.productengineer.business.lecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import io.github.jangdongho.productengineer.persistence.lecture.Lecture;
import io.github.jangdongho.productengineer.persistence.lecture.LectureRepository;
import io.github.jangdongho.productengineer.presentation.lecture.ClassStatusResponse;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LectureServiceTest {

	@Mock
	private LectureRepository lectureRepository;

	@InjectMocks
	private LectureService lectureService;

	@Test
	@DisplayName("DRAFT → OPEN 전이는 성공한다")
	void draftToOpen_succeeds() {
		Lecture lecture = new Lecture();
		lecture.setStatus(ClassStatus.DRAFT);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

		ClassStatusResponse result = lectureService.updateStatus(1L, ClassStatus.OPEN);

		assertThat(result.status()).isEqualTo(ClassStatus.OPEN);
		assertThat(lecture.getStatus()).isEqualTo(ClassStatus.OPEN);
	}

	@Test
	@DisplayName("OPEN → CLOSED 전이는 성공한다")
	void openToClosed_succeeds() {
		Lecture lecture = new Lecture();
		lecture.setStatus(ClassStatus.OPEN);
		when(lectureRepository.findById(2L)).thenReturn(Optional.of(lecture));

		ClassStatusResponse result = lectureService.updateStatus(2L, ClassStatus.CLOSED);

		assertThat(result.status()).isEqualTo(ClassStatus.CLOSED);
		assertThat(lecture.getStatus()).isEqualTo(ClassStatus.CLOSED);
	}

	@Test
	@DisplayName("DRAFT → CLOSED 는 허용되지 않는다")
	void draftToClosed_throws() {
		Lecture lecture = new Lecture();
		lecture.setStatus(ClassStatus.DRAFT);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

		assertThatThrownBy(() -> lectureService.updateStatus(1L, ClassStatus.CLOSED))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(lectureRepository).findById(eq(1L));
		verifyNoMoreInteractions(lectureRepository);
	}

	@Test
	@DisplayName("CLOSED → OPEN 은 허용되지 않는다")
	void closedToOpen_throws() {
		Lecture lecture = new Lecture();
		lecture.setStatus(ClassStatus.CLOSED);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

		assertThatThrownBy(() -> lectureService.updateStatus(1L, ClassStatus.OPEN))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	@DisplayName("강의가 없으면 NOT_FOUND 를 던진다")
	void notFound_throws() {
		when(lectureRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> lectureService.updateStatus(99L, ClassStatus.OPEN))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
	}
}
