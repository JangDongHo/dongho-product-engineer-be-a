package io.github.jangdongho.productengineer.business.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.persistence.enrollment.Enrollment;
import io.github.jangdongho.productengineer.persistence.enrollment.EnrollmentRepository;
import io.github.jangdongho.productengineer.persistence.enrollment.EnrollmentStatus;
import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import io.github.jangdongho.productengineer.persistence.lecture.Lecture;
import io.github.jangdongho.productengineer.persistence.lecture.LectureRepository;
import io.github.jangdongho.productengineer.presentation.enrollment.EnrollmentCancelledResponse;
import io.github.jangdongho.productengineer.presentation.enrollment.EnrollmentConfirmedResponse;
import io.github.jangdongho.productengineer.presentation.enrollment.EnrollmentCreatedResponse;
import io.github.jangdongho.productengineer.presentation.enrollment.EnrollmentListItemResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	@Mock
	private LectureRepository lectureRepository;

	@Mock
	private EnrollmentRepository enrollmentRepository;

	private Clock clock;
	private EnrollmentService enrollmentService;

	@BeforeEach
	void setUp() {
		clock = Clock.fixed(Instant.parse("2026-05-01T03:00:00Z"), SEOUL);
		enrollmentService = new EnrollmentService(lectureRepository, enrollmentRepository, clock);
	}

	@Test
	@DisplayName("listByUserId: 신청이 없으면 빈 목록")
	void listByUserId_empty() {
		when(enrollmentRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of());

		assertThat(enrollmentService.listByUserId(1L)).isEmpty();

		verify(lectureRepository, never()).findAllById(any());
	}

	@Test
	@DisplayName("listByUserId: 신청·강의를 조합해 반환한다")
	void listByUserId_mapsLecture() {
		Enrollment e1 = pendingEnrollment(10L, 1L, 20L);
		Lecture lec = openLecture(20L, 30, 2);
		when(enrollmentRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(e1));
		when(lectureRepository.findAllById(List.of(20L))).thenReturn(List.of(lec));

		List<EnrollmentListItemResponse> result = enrollmentService.listByUserId(1L);

		assertThat(result).hasSize(1);
		EnrollmentListItemResponse row = result.get(0);
		assertThat(row.enrollmentId()).isEqualTo(10L);
		assertThat(row.status()).isEqualTo(EnrollmentStatus.PENDING);
		assertThat(row.confirmedAt()).isNull();
		assertThat(row.lecture().id()).isEqualTo(20L);
		assertThat(row.lecture().title()).isEqualTo("t");
		assertThat(row.lecture().status()).isEqualTo(ClassStatus.OPEN);
	}

	@Test
	@DisplayName("listByUserId: 강의가 없으면 NOT_FOUND")
	void listByUserId_lectureMissing() {
		Enrollment e1 = pendingEnrollment(1L, 1L, 99L);
		when(enrollmentRepository.findByUserIdOrderByIdAsc(1L)).thenReturn(List.of(e1));
		when(lectureRepository.findAllById(List.of(99L))).thenReturn(List.of());

		assertThatThrownBy(() -> enrollmentService.listByUserId(1L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
	}

	@Test
	@DisplayName("enroll: OPEN 강의이고 정원 여유·미중복이면 PENDING 저장 및 currentEnrollment 1 증가")
	void enroll_success() {
		Lecture lecture = openLecture(10L, 5, 3);
		when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));
		when(enrollmentRepository.existsByUserIdAndClassId(1L, 10L)).thenReturn(false);
		when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(inv -> {
			Enrollment e = inv.getArgument(0);
			e.setId(100L);
			return e;
		});

		EnrollmentCreatedResponse result = enrollmentService.enroll(1L, 10L);

		assertThat(result.id()).isEqualTo(100L);
		assertThat(result.status()).isEqualTo(EnrollmentStatus.PENDING);
		assertThat(lecture.getCurrentEnrollment()).isEqualTo(4);

		verify(lectureRepository).save(lecture);
		ArgumentCaptor<Enrollment> enrollmentCaptor = ArgumentCaptor.forClass(Enrollment.class);
		verify(enrollmentRepository).save(enrollmentCaptor.capture());
		assertThat(enrollmentCaptor.getValue().getUserId()).isEqualTo(1L);
		assertThat(enrollmentCaptor.getValue().getClassId()).isEqualTo(10L);
		assertThat(enrollmentCaptor.getValue().getStatus()).isEqualTo(EnrollmentStatus.PENDING);
	}

	@Test
	@DisplayName("enroll: 강의 없으면 NOT_FOUND")
	void enroll_classNotFound() {
		when(lectureRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.enroll(1L, 99L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

		verify(lectureRepository).findById(eq(99L));
		verifyNoMoreInteractions(lectureRepository);
		verifyNoMoreInteractions(enrollmentRepository);
	}

	@Test
	@DisplayName("enroll: DRAFT 강의면 VALIDATION_ERROR")
	void enroll_draftClass() {
		Lecture lecture = lectureWithStatus(1L, ClassStatus.DRAFT, 10, 0);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

		assertThatThrownBy(() -> enrollmentService.enroll(2L, 1L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(enrollmentRepository, never()).save(any());
	}

	@Test
	@DisplayName("enroll: CLOSED 강의면 VALIDATION_ERROR")
	void enroll_closedClass() {
		Lecture lecture = lectureWithStatus(1L, ClassStatus.CLOSED, 10, 0);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

		assertThatThrownBy(() -> enrollmentService.enroll(2L, 1L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
	}

	@Test
	@DisplayName("enroll: 정원 초과면 CONFLICT")
	void enroll_capacityFull() {
		Lecture lecture = openLecture(1L, 10, 10);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

		assertThatThrownBy(() -> enrollmentService.enroll(2L, 1L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
	}

	@Test
	@DisplayName("enroll: 동일 강의 중복 신청이면 CONFLICT")
	void enroll_duplicate() {
		Lecture lecture = openLecture(1L, 10, 2);
		when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));
		when(enrollmentRepository.existsByUserIdAndClassId(2L, 1L)).thenReturn(true);

		assertThatThrownBy(() -> enrollmentService.enroll(2L, 1L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
	}

	@Test
	@DisplayName("confirm: PENDING이면 CONFIRMED·confirmedAt 기록")
	void confirm_success() {
		Enrollment pending = pendingEnrollment(7L, 1L, 10L);
		when(enrollmentRepository.findById(7L)).thenReturn(Optional.of(pending));
		when(enrollmentRepository.save(pending)).thenReturn(pending);

		EnrollmentConfirmedResponse result = enrollmentService.confirm(7L);

		assertThat(result.id()).isEqualTo(7L);
		assertThat(result.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
		assertThat(result.confirmedAt()).isEqualTo(LocalDateTime.parse("2026-05-01T12:00:00"));
		assertThat(pending.getConfirmedAt()).isEqualTo(result.confirmedAt());
		verify(enrollmentRepository).save(pending);
		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("confirm: 이미 CONFIRMED면 VALIDATION_ERROR")
	void confirm_alreadyConfirmed() {
		Enrollment e = pendingEnrollment(1L, 1L, 10L);
		e.setStatus(EnrollmentStatus.CONFIRMED);
		when(enrollmentRepository.findById(1L)).thenReturn(Optional.of(e));

		assertThatThrownBy(() -> enrollmentService.confirm(1L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(enrollmentRepository, never()).save(any());
		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("confirm: CANCELLED면 VALIDATION_ERROR")
	void confirm_cancelled() {
		Enrollment e = pendingEnrollment(2L, 1L, 10L);
		e.setStatus(EnrollmentStatus.CANCELLED);
		when(enrollmentRepository.findById(2L)).thenReturn(Optional.of(e));

		assertThatThrownBy(() -> enrollmentService.confirm(2L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(enrollmentRepository, never()).save(any());
		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("confirm: 신청 없으면 NOT_FOUND")
	void confirm_notFound() {
		when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.confirm(99L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("confirm: LectureRepository를 호출하지 않아 currentEnrollment는 변경되지 않음")
	void confirm_doesNotTouchLecture() {
		Enrollment pending = pendingEnrollment(3L, 2L, 5L);
		when(enrollmentRepository.findById(3L)).thenReturn(Optional.of(pending));
		when(enrollmentRepository.save(pending)).thenReturn(pending);

		enrollmentService.confirm(3L);

		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("cancel: PENDING이면 CANCELLED 전환 및 currentEnrollment 1 감소")
	void cancel_pending_success() {
		Enrollment enrollment = pendingEnrollment(20L, 1L, 10L);
		Lecture lecture = openLecture(10L, 5, 4);
		when(enrollmentRepository.findById(20L)).thenReturn(Optional.of(enrollment));
		when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));
		when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

		EnrollmentCancelledResponse result = enrollmentService.cancel(20L);

		assertThat(result.id()).isEqualTo(20L);
		assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
		assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
		assertThat(lecture.getCurrentEnrollment()).isEqualTo(3);
		verify(lectureRepository).save(lecture);
		verify(enrollmentRepository).save(enrollment);
	}

	@Test
	@DisplayName("cancel: CONFIRMED이고 확정 후 7일 이내면 취소 및 currentEnrollment 1 감소")
	void cancel_confirmed_within7Days() {
		Clock c = Clock.fixed(Instant.parse("2026-05-10T03:00:00Z"), SEOUL);
		EnrollmentService svc = new EnrollmentService(lectureRepository, enrollmentRepository, c);
		Enrollment enrollment = pendingEnrollment(21L, 1L, 10L);
		enrollment.setStatus(EnrollmentStatus.CONFIRMED);
		enrollment.setConfirmedAt(LocalDateTime.parse("2026-05-05T12:00:00"));
		Lecture lecture = openLecture(10L, 5, 2);
		when(enrollmentRepository.findById(21L)).thenReturn(Optional.of(enrollment));
		when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));
		when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

		EnrollmentCancelledResponse result = svc.cancel(21L);

		assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
		assertThat(lecture.getCurrentEnrollment()).isEqualTo(1);
		verify(lectureRepository).save(lecture);
		verify(enrollmentRepository).save(enrollment);
	}

	@Test
	@DisplayName("cancel: CONFIRMED이고 now가 마감 시각과 같으면 취소 허용(경계)")
	void cancel_confirmed_atDeadline_inclusive() {
		Clock c = Clock.fixed(Instant.parse("2026-05-12T03:00:00Z"), SEOUL);
		EnrollmentService svc = new EnrollmentService(lectureRepository, enrollmentRepository, c);
		Enrollment enrollment = pendingEnrollment(22L, 1L, 10L);
		enrollment.setStatus(EnrollmentStatus.CONFIRMED);
		enrollment.setConfirmedAt(LocalDateTime.parse("2026-05-05T12:00:00"));
		Lecture lecture = openLecture(10L, 5, 1);
		when(enrollmentRepository.findById(22L)).thenReturn(Optional.of(enrollment));
		when(lectureRepository.findById(10L)).thenReturn(Optional.of(lecture));
		when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);

		EnrollmentCancelledResponse result = svc.cancel(22L);

		assertThat(result.status()).isEqualTo(EnrollmentStatus.CANCELLED);
		assertThat(lecture.getCurrentEnrollment()).isEqualTo(0);
		verify(lectureRepository).save(lecture);
		verify(enrollmentRepository).save(enrollment);
	}

	@Test
	@DisplayName("cancel: CONFIRMED이고 확정 후 7일 초과면 VALIDATION_ERROR(경계 직후)")
	void cancel_confirmed_afterDeadline() {
		Clock c = Clock.fixed(Instant.parse("2026-05-12T03:00:01Z"), SEOUL);
		EnrollmentService svc = new EnrollmentService(lectureRepository, enrollmentRepository, c);
		Enrollment enrollment = pendingEnrollment(23L, 1L, 10L);
		enrollment.setStatus(EnrollmentStatus.CONFIRMED);
		enrollment.setConfirmedAt(LocalDateTime.parse("2026-05-05T12:00:00"));
		when(enrollmentRepository.findById(23L)).thenReturn(Optional.of(enrollment));

		assertThatThrownBy(() -> svc.cancel(23L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(enrollmentRepository, never()).save(any());
		verify(lectureRepository, never()).save(any());
	}

	@Test
	@DisplayName("cancel: 이미 CANCELLED면 VALIDATION_ERROR")
	void cancel_alreadyCancelled() {
		Enrollment enrollment = pendingEnrollment(24L, 1L, 10L);
		enrollment.setStatus(EnrollmentStatus.CANCELLED);
		when(enrollmentRepository.findById(24L)).thenReturn(Optional.of(enrollment));

		assertThatThrownBy(() -> enrollmentService.cancel(24L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(enrollmentRepository, never()).save(any());
		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("cancel: 신청 없으면 NOT_FOUND")
	void cancel_notFound() {
		when(enrollmentRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.cancel(99L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

		verifyNoInteractions(lectureRepository);
	}

	@Test
	@DisplayName("cancel: 강의 없으면 NOT_FOUND")
	void cancel_lectureNotFound() {
		Enrollment enrollment = pendingEnrollment(25L, 1L, 10L);
		when(enrollmentRepository.findById(25L)).thenReturn(Optional.of(enrollment));
		when(lectureRepository.findById(10L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> enrollmentService.cancel(25L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

		verify(enrollmentRepository, never()).save(any());
		verify(lectureRepository, never()).save(any());
	}

	@Test
	@DisplayName("cancel: CONFIRMED인데 confirmedAt 없으면 VALIDATION_ERROR")
	void cancel_confirmed_missingConfirmedAt() {
		Enrollment enrollment = pendingEnrollment(26L, 1L, 10L);
		enrollment.setStatus(EnrollmentStatus.CONFIRMED);
		when(enrollmentRepository.findById(26L)).thenReturn(Optional.of(enrollment));

		assertThatThrownBy(() -> enrollmentService.cancel(26L))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));

		verify(enrollmentRepository, never()).save(any());
		verifyNoInteractions(lectureRepository);
	}

	private static Enrollment pendingEnrollment(long id, long userId, long classId) {
		Enrollment e = new Enrollment();
		e.setId(id);
		e.setUserId(userId);
		e.setClassId(classId);
		e.setStatus(EnrollmentStatus.PENDING);
		return e;
	}

	private static Lecture openLecture(long id, int capacity, int currentEnrollment) {
		return lectureWithStatus(id, ClassStatus.OPEN, capacity, currentEnrollment);
	}

	private static Lecture lectureWithStatus(long id, ClassStatus status, int capacity, int currentEnrollment) {
		Lecture lecture = new Lecture();
		lecture.setId(id);
		lecture.setCreatorId(1L);
		lecture.setTitle("t");
		lecture.setDescription("d");
		lecture.setPrice(1000L);
		lecture.setCapacity(capacity);
		lecture.setStartDate(LocalDateTime.parse("2026-05-01T10:00:00"));
		lecture.setEndDate(LocalDateTime.parse("2026-05-30T18:00:00"));
		lecture.setStatus(status);
		lecture.setCurrentEnrollment(currentEnrollment);
		return lecture;
	}
}
