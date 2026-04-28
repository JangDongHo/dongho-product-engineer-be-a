package io.github.jangdongho.productengineer.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.github.jangdongho.productengineer.enrollment.repository.EnrollmentRepository;
import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.domain.Lecture;
import io.github.jangdongho.productengineer.lecture.repository.LectureRepository;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EnrollmentConcurrencyIntegrationTest {

  @Autowired private EnrollmentService enrollmentService;

  @Autowired private LectureRepository lectureRepository;

  @Autowired private EnrollmentRepository enrollmentRepository;

  @AfterEach
  void tearDown() {
    enrollmentRepository.deleteAll();
    lectureRepository.deleteAll();
  }

  @Test
  @DisplayName("잔여 1석에 여러 명이 동시에 신청해도 비관적 락으로 정확히 1명만 성공한다")
  void enroll_lastSeat_concurrently_onlyOneSucceeds() throws InterruptedException {
    Lecture lecture = lectureRepository.save(openLectureWithOneSeatLeft());

    int applicants = 20;
    ExecutorService executor = Executors.newFixedThreadPool(applicants);

    CountDownLatch ready = new CountDownLatch(applicants);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(applicants);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger conflictCount = new AtomicInteger();

    Queue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    // 20개의 스레드 생성
    for (int i = 0; i < applicants; i++) {
      long userId = 1_000L + i; // 서로 다른 사용자 ID 생성 (1000~1019)
      executor.submit(
          () -> {
            ready.countDown();
            try {
              start.await();
              enrollmentService.enroll(userId, lecture.getId());
              successCount.incrementAndGet();
            } catch (BusinessException ex) {
              if (ex.getErrorCode() == ErrorCode.CONFLICT) {
                conflictCount.incrementAndGet();
              } else {
                unexpectedErrors.add(ex);
              }
            } catch (Throwable ex) {
              unexpectedErrors.add(ex);
            } finally {
              done.countDown();
            }
          });
    }

    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    executor.shutdown();
    assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    Lecture savedLecture = lectureRepository.findById(lecture.getId()).orElseThrow();
    assertThat(unexpectedErrors).isEmpty(); // 예상치 못한 예외는 없어야 함
    assertThat(successCount).hasValue(1); // 정확히 한 명만 성공해야 함
    assertThat(conflictCount).hasValue(applicants - 1); // 나머지는 CONFLICT
    assertThat(savedLecture.getCurrentEnrollment())
        .isEqualTo(savedLecture.getCapacity()); // DB의 강의: 현재 신청 인원 == capacity
    assertThat(enrollmentRepository.findAll()).hasSize(1); // 수강 신청 레코드는 1개만 있어야 함
  }

  @Test
  @DisplayName("같은 신청을 동시에 여러 번 취소해도 정원은 한 번만 감소한다")
  void cancel_sameEnrollmentConcurrently_onlyOneSucceeds() throws InterruptedException {
    Lecture lecture = lectureRepository.save(openLecture(1));
    Enrollment enrollment = enrollmentRepository.save(pendingEnrollment(lecture.getId()));

    int cancelRequests = 10;
    ExecutorService executor = Executors.newFixedThreadPool(cancelRequests);

    CountDownLatch ready = new CountDownLatch(cancelRequests);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(cancelRequests);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger validationErrorCount = new AtomicInteger();
    Queue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    for (int i = 0; i < cancelRequests; i++) {
      executor.submit(
          () -> {
            ready.countDown();
            try {
              start.await();
              enrollmentService.cancel(enrollment.getId());
              successCount.incrementAndGet();
            } catch (BusinessException ex) {
              if (ex.getErrorCode() == ErrorCode.VALIDATION_ERROR) {
                validationErrorCount.incrementAndGet();
              } else {
                unexpectedErrors.add(ex);
              }
            } catch (Throwable ex) {
              unexpectedErrors.add(ex);
            } finally {
              done.countDown();
            }
          });
    }

    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    executor.shutdown();
    assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    Enrollment savedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
    Lecture savedLecture = lectureRepository.findById(lecture.getId()).orElseThrow();

    assertThat(unexpectedErrors).isEmpty();
    assertThat(successCount).hasValue(1);
    assertThat(validationErrorCount).hasValue(cancelRequests - 1);
    assertThat(savedEnrollment.getStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(savedLecture.getCurrentEnrollment()).isZero();
  }

  @Test
  @DisplayName("확정과 취소를 동시에 실행해도 신청 상태와 현재 수강 인원은 일치한다")
  void confirmAndCancel_sameEnrollmentConcurrently_keepsStateConsistent()
      throws InterruptedException {
    Lecture lecture = lectureRepository.save(openLecture(1));
    Enrollment enrollment = enrollmentRepository.save(pendingEnrollment(lecture.getId()));

    int requests = 2;
    ExecutorService executor = Executors.newFixedThreadPool(requests);

    CountDownLatch ready = new CountDownLatch(requests);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(requests);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger validationErrorCount = new AtomicInteger();
    Queue<Throwable> unexpectedErrors = new ConcurrentLinkedQueue<>();

    executor.submit(
        () -> {
          ready.countDown();
          try {
            start.await();
            enrollmentService.confirm(enrollment.getId());
            successCount.incrementAndGet();
          } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.VALIDATION_ERROR) {
              validationErrorCount.incrementAndGet();
            } else {
              unexpectedErrors.add(ex);
            }
          } catch (Throwable ex) {
            unexpectedErrors.add(ex);
          } finally {
            done.countDown();
          }
        });

    executor.submit(
        () -> {
          ready.countDown();
          try {
            start.await();
            enrollmentService.cancel(enrollment.getId());
            successCount.incrementAndGet();
          } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.VALIDATION_ERROR) {
              validationErrorCount.incrementAndGet();
            } else {
              unexpectedErrors.add(ex);
            }
          } catch (Throwable ex) {
            unexpectedErrors.add(ex);
          } finally {
            done.countDown();
          }
        });

    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
    start.countDown();
    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    executor.shutdown();
    assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

    Enrollment savedEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();
    Lecture savedLecture = lectureRepository.findById(lecture.getId()).orElseThrow();

    assertThat(unexpectedErrors).isEmpty();
    assertThat(successCount.get() + validationErrorCount.get()).isEqualTo(requests);
    assertThat(successCount.get()).isBetween(1, 2);
    assertThat(savedEnrollment.getStatus())
        .isIn(EnrollmentStatus.CONFIRMED, EnrollmentStatus.CANCELLED);
    if (savedEnrollment.getStatus() == EnrollmentStatus.CONFIRMED) {
      assertThat(savedLecture.getCurrentEnrollment()).isEqualTo(1);
    } else {
      assertThat(savedLecture.getCurrentEnrollment()).isZero();
    }
  }

  private static Lecture openLectureWithOneSeatLeft() {
    Lecture lecture = new Lecture();
    lecture.setCreatorId(1L);
    lecture.setTitle("Concurrency Class");
    lecture.setDescription("Pessimistic lock integration test");
    lecture.setPrice(10_000L);
    lecture.setCapacity(2);
    lecture.setCurrentEnrollment(1);
    lecture.setStartDate(LocalDateTime.parse("2026-05-01T10:00:00"));
    lecture.setEndDate(LocalDateTime.parse("2026-06-01T10:00:00"));
    lecture.setStatus(ClassStatus.OPEN);
    return lecture;
  }

  private static Lecture openLecture(int currentEnrollment) {
    Lecture lecture = new Lecture();
    lecture.setCreatorId(1L);
    lecture.setTitle("Concurrency Class");
    lecture.setDescription("Pessimistic lock integration test");
    lecture.setPrice(10_000L);
    lecture.setCapacity(10);
    lecture.setCurrentEnrollment(currentEnrollment);
    lecture.setStartDate(LocalDateTime.parse("2026-05-01T10:00:00"));
    lecture.setEndDate(LocalDateTime.parse("2026-06-01T10:00:00"));
    lecture.setStatus(ClassStatus.OPEN);
    return lecture;
  }

  private static Enrollment pendingEnrollment(long classId) {
    Enrollment enrollment = new Enrollment();
    enrollment.setUserId(1L);
    enrollment.setClassId(classId);
    enrollment.setStatus(EnrollmentStatus.PENDING);
    return enrollment;
  }
}
