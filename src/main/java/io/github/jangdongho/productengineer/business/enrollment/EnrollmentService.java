package io.github.jangdongho.productengineer.business.enrollment;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.persistence.enrollment.Enrollment;
import io.github.jangdongho.productengineer.persistence.enrollment.EnrollmentRepository;
import io.github.jangdongho.productengineer.persistence.enrollment.EnrollmentStatus;
import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import io.github.jangdongho.productengineer.persistence.lecture.Lecture;
import io.github.jangdongho.productengineer.persistence.lecture.LectureRepository;
import io.github.jangdongho.productengineer.presentation.enrollment.EnrollmentConfirmedResponse;
import io.github.jangdongho.productengineer.presentation.enrollment.EnrollmentCreatedResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

	private final LectureRepository lectureRepository;
	private final EnrollmentRepository enrollmentRepository;

	@Transactional
	public EnrollmentCreatedResponse enroll(long userId, long classId) {
		Lecture lecture = lectureRepository.findById(classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		if (lecture.getStatus() != ClassStatus.OPEN) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "모집 중인 강의만 신청할 수 있습니다.");
		}

		if (lecture.getCurrentEnrollment() >= lecture.getCapacity()) {
			throw new BusinessException(ErrorCode.CONFLICT, "정원이 마감되었습니다.");
		}

		if (enrollmentRepository.existsByUserIdAndClassId(userId, classId)) {
			throw new BusinessException(ErrorCode.CONFLICT, "이미 신청한 강의입니다.");
		}

		lecture.setCurrentEnrollment(lecture.getCurrentEnrollment() + 1);
		lectureRepository.save(lecture);

		Enrollment enrollment = new Enrollment();
		enrollment.setUserId(userId);
		enrollment.setClassId(classId);
		enrollment.setStatus(EnrollmentStatus.PENDING);
		enrollmentRepository.save(enrollment);

		return new EnrollmentCreatedResponse(enrollment.getId(), enrollment.getStatus());
	}

	@Transactional
	public EnrollmentConfirmedResponse confirm(long enrollmentId) {
		Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		if (enrollment.getStatus() != EnrollmentStatus.PENDING) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "결제 대기(PENDING) 상태의 신청만 확정할 수 있습니다.");
		}

		enrollment.setStatus(EnrollmentStatus.CONFIRMED);
		enrollment.setConfirmedAt(LocalDateTime.now());
		enrollmentRepository.save(enrollment);

		return new EnrollmentConfirmedResponse(enrollment.getId(), enrollment.getStatus(), enrollment.getConfirmedAt());
	}
}
