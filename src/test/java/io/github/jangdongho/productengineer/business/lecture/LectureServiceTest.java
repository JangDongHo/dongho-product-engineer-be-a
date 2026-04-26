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
import io.github.jangdongho.productengineer.presentation.lecture.ClassDetailResponse;
import io.github.jangdongho.productengineer.presentation.lecture.ClassListItemResponse;
import io.github.jangdongho.productengineer.presentation.lecture.ClassStatusResponse;
import java.time.LocalDateTime;
import java.util.List;
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
	@DisplayName("listClasses: status 가 없으면 전체(createdAt 최신순)를 반환한다")
	void listClasses_nullStatus_usesFindAll() {
		Lecture a = sampleLecture(1L, ClassStatus.DRAFT);
		Lecture b = sampleLecture(2L, ClassStatus.OPEN);
		when(lectureRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(b, a));

		List<ClassListItemResponse> result = lectureService.listClasses(null);

		assertThat(result).hasSize(2);
		assertThat(result.getFirst().id()).isEqualTo(2L);
		assertThat(result.get(1).id()).isEqualTo(1L);
		verify(lectureRepository).findAllByOrderByCreatedAtDesc();
	}

	@Test
	@DisplayName("listClasses: status 가 있으면 해당 상태만 반환한다")
	void listClasses_withStatus_filters() {
		Lecture open = sampleLecture(1L, ClassStatus.OPEN);
		when(lectureRepository.findByStatusOrderByCreatedAtDesc(ClassStatus.OPEN))
				.thenReturn(List.of(open));

		List<ClassListItemResponse> result = lectureService.listClasses(ClassStatus.OPEN);

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().status()).isEqualTo(ClassStatus.OPEN);
		verify(lectureRepository).findByStatusOrderByCreatedAtDesc(ClassStatus.OPEN);
	}

	@Test
	@DisplayName("getClassById: currentEnrollment 를 상세 DTO에 반영한다")
	void getClassById_mapsCurrentEnrollment() {
		Lecture lecture = sampleLecture(5L, ClassStatus.OPEN);
		lecture.setCurrentEnrollment(7);
		when(lectureRepository.findById(5L)).thenReturn(Optional.of(lecture));

		ClassDetailResponse result = lectureService.getClassById(5L);

		assertThat(result.id()).isEqualTo(5L);
		assertThat(result.currentEnrollment()).isEqualTo(7);
	}

	@Test
	@DisplayName("getClassById: 강의가 없으면 NOT_FOUND 를 던진다")
	void getClassById_notFound() {
		when(lectureRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> lectureService.getClassById(99L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
	}

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

	private static Lecture sampleLecture(long id, ClassStatus status) {
		Lecture lecture = new Lecture();
		lecture.setId(id);
		lecture.setCreatorId(1L);
		lecture.setTitle("t");
		lecture.setDescription("d");
		lecture.setPrice(1000L);
		lecture.setCapacity(10);
		lecture.setStartDate(LocalDateTime.parse("2026-05-01T10:00:00"));
		lecture.setEndDate(LocalDateTime.parse("2026-05-30T18:00:00"));
		lecture.setStatus(status);
		lecture.setCurrentEnrollment(0);
		return lecture;
	}
}
