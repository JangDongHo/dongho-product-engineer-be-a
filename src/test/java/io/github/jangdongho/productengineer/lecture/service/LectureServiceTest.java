package io.github.jangdongho.productengineer.lecture.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.github.jangdongho.productengineer.enrollment.dto.ClassConfirmedEnrollmentItemResponse;
import io.github.jangdongho.productengineer.enrollment.repository.EnrollmentRepository;
import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.domain.Lecture;
import io.github.jangdongho.productengineer.lecture.dto.ClassDetailResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassListItemResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassStatusResponse;
import io.github.jangdongho.productengineer.lecture.repository.LectureRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class LectureServiceTest {

  @Mock private LectureRepository lectureRepository;

  @Mock private EnrollmentRepository enrollmentRepository;

  @InjectMocks private LectureService lectureService;

  @Test
  @DisplayName("listClasses: status 가 없으면 전체(createdAt 최신순)를 반환한다")
  void listClasses_nullStatus_usesFindAll() {
    PageRequest pageable = PageRequest.of(0, 20);
    Lecture a = sampleLecture(1L, ClassStatus.DRAFT);
    Lecture b = sampleLecture(2L, ClassStatus.OPEN);
    when(lectureRepository.findAllByOrderByCreatedAtDesc(pageable))
        .thenReturn(new PageImpl<>(List.of(b, a), pageable, 2));

    Page<ClassListItemResponse> result = lectureService.listClasses(null, pageable);

    assertThat(result.getContent()).hasSize(2);
    assertThat(result.getContent().getFirst().id()).isEqualTo(2L);
    assertThat(result.getContent().get(1).id()).isEqualTo(1L);
    verify(lectureRepository).findAllByOrderByCreatedAtDesc(pageable);
  }

  @Test
  @DisplayName("listClasses: status 가 있으면 해당 상태만 반환한다")
  void listClasses_withStatus_filters() {
    PageRequest pageable = PageRequest.of(0, 20);
    Lecture open = sampleLecture(1L, ClassStatus.OPEN);
    when(lectureRepository.findByStatusOrderByCreatedAtDesc(ClassStatus.OPEN, pageable))
        .thenReturn(new PageImpl<>(List.of(open), pageable, 1));

    Page<ClassListItemResponse> result = lectureService.listClasses(ClassStatus.OPEN, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().status()).isEqualTo(ClassStatus.OPEN);
    verify(lectureRepository).findByStatusOrderByCreatedAtDesc(ClassStatus.OPEN, pageable);
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
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
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
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR));

    verify(lectureRepository).findById(eq(1L));
    verifyNoMoreInteractions(lectureRepository);
    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  @DisplayName("CLOSED → OPEN 은 허용되지 않는다")
  void closedToOpen_throws() {
    Lecture lecture = new Lecture();
    lecture.setStatus(ClassStatus.CLOSED);
    when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

    assertThatThrownBy(() -> lectureService.updateStatus(1L, ClassStatus.OPEN))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_ERROR));
  }

  @Test
  @DisplayName("강의가 없으면 NOT_FOUND 를 던진다")
  void notFound_throws() {
    when(lectureRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> lectureService.updateStatus(99L, ClassStatus.OPEN))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
  }

  @Test
  @DisplayName("listConfirmedEnrollmentsForCreator: 강의 없으면 NOT_FOUND, enrollmentRepository 미호출")
  void listConfirmed_classNotFound() {
    PageRequest pageable = PageRequest.of(0, 20);
    when(lectureRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> lectureService.listConfirmedEnrollmentsForCreator(99L, 1L, pageable))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));

    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  @DisplayName("listConfirmedEnrollmentsForCreator: 소유자 아님 FORBIDDEN, enrollmentRepository 미호출")
  void listConfirmed_forbidden() {
    PageRequest pageable = PageRequest.of(0, 20);
    Lecture lecture = sampleLecture(1L, ClassStatus.OPEN);
    lecture.setCreatorId(10L);
    when(lectureRepository.findById(1L)).thenReturn(Optional.of(lecture));

    assertThatThrownBy(() -> lectureService.listConfirmedEnrollmentsForCreator(1L, 99L, pageable))
        .isInstanceOf(BusinessException.class)
        .satisfies(
            ex ->
                assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

    verifyNoInteractions(enrollmentRepository);
  }

  @Test
  @DisplayName("listConfirmedEnrollmentsForCreator: 소유자·CONFIRMED 없으면 빈 목록")
  void listConfirmed_empty() {
    PageRequest pageable = PageRequest.of(0, 20);
    Lecture lecture = sampleLecture(5L, ClassStatus.OPEN);
    lecture.setCreatorId(2L);
    when(lectureRepository.findById(5L)).thenReturn(Optional.of(lecture));
    when(enrollmentRepository.findByClassIdAndStatusOrderByCreatedAtDescIdDesc(
            5L, EnrollmentStatus.CONFIRMED, pageable))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    Page<ClassConfirmedEnrollmentItemResponse> result =
        lectureService.listConfirmedEnrollmentsForCreator(5L, 2L, pageable);

    assertThat(result.getContent()).isEmpty();
  }

  @Test
  @DisplayName("listConfirmedEnrollmentsForCreator: CONFIRMED 항목을 DTO로 반환")
  void listConfirmed_mapsRows() {
    PageRequest pageable = PageRequest.of(0, 20);
    Lecture lecture = sampleLecture(3L, ClassStatus.OPEN);
    lecture.setCreatorId(7L);
    Enrollment e = new Enrollment();
    e.setId(100L);
    e.setUserId(20L);
    e.setClassId(3L);
    e.setStatus(EnrollmentStatus.CONFIRMED);
    e.setConfirmedAt(LocalDateTime.parse("2026-06-10T15:00:00"));
    when(lectureRepository.findById(3L)).thenReturn(Optional.of(lecture));
    when(enrollmentRepository.findByClassIdAndStatusOrderByCreatedAtDescIdDesc(
            3L, EnrollmentStatus.CONFIRMED, pageable))
        .thenReturn(new PageImpl<>(List.of(e), pageable, 1));

    Page<ClassConfirmedEnrollmentItemResponse> result =
        lectureService.listConfirmedEnrollmentsForCreator(3L, 7L, pageable);

    assertThat(result.getContent()).hasSize(1);
    assertThat(result.getContent().getFirst().enrollmentId()).isEqualTo(100L);
    assertThat(result.getContent().getFirst().userId()).isEqualTo(20L);
    assertThat(result.getContent().getFirst().status()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(result.getContent().getFirst().confirmedAt())
        .isEqualTo(LocalDateTime.parse("2026-06-10T15:00:00"));
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
