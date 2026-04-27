package io.github.jangdongho.productengineer.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
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

	@Autowired
	private EnrollmentService enrollmentService;

	@Autowired
	private LectureRepository lectureRepository;

	@Autowired
	private EnrollmentRepository enrollmentRepository;

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
			executor.submit(() -> {
				ready.countDown();
				try {
					start.await();
					enrollmentService.enroll(userId, lecture.getId());
					successCount.incrementAndGet();
				} 
				catch (BusinessException ex) {
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
		assertThat(savedLecture.getCurrentEnrollment()).isEqualTo(savedLecture.getCapacity()); // DB의 강의: 현재 신청 인원 == capacity
		assertThat(enrollmentRepository.findAll()).hasSize(1); // 수강 신청 레코드는 1개만 있어야 함
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
}
