package io.github.jangdongho.productengineer.lecture.service;

import io.github.jangdongho.productengineer.common.exception.BusinessException;
import io.github.jangdongho.productengineer.common.exception.ErrorCode;
import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import io.github.jangdongho.productengineer.enrollment.dto.ClassConfirmedEnrollmentItemResponse;
import io.github.jangdongho.productengineer.enrollment.repository.EnrollmentRepository;
import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.domain.Lecture;
import io.github.jangdongho.productengineer.lecture.dto.ClassCreatedResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassDetailResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassListItemResponse;
import io.github.jangdongho.productengineer.lecture.dto.ClassStatusResponse;
import io.github.jangdongho.productengineer.lecture.dto.CreateClassRequest;
import io.github.jangdongho.productengineer.lecture.repository.LectureRepository;

import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LectureService {

	private final LectureRepository lectureRepository;
	private final EnrollmentRepository enrollmentRepository;

	@Transactional(readOnly = true)
	public List<ClassListItemResponse> listClasses(@Nullable ClassStatus status) {
		List<Lecture> lectures = status == null
				? lectureRepository.findAllByOrderByCreatedAtDesc()
				: lectureRepository.findByStatusOrderByCreatedAtDesc(status);
		return lectures.stream().map(this::toListItem).toList();
	}

	@Transactional(readOnly = true)
	public ClassDetailResponse getClassById(long id) {
		Lecture lecture = lectureRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		return toDetail(lecture);
	}

	@Transactional(readOnly = true)
	public List<ClassConfirmedEnrollmentItemResponse> listConfirmedEnrollmentsForCreator(long classId, long creatorId) {
		Lecture lecture = lectureRepository.findById(classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

		if (!Objects.equals(lecture.getCreatorId(), creatorId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		
		return enrollmentRepository
				.findByClassIdAndStatusOrderByCreatedAtDescIdDesc(classId, EnrollmentStatus.CONFIRMED)
				.stream()
				.map(this::toClassConfirmedItem)
				.toList();
	}

	@Transactional
	public ClassCreatedResponse create(long creatorId, CreateClassRequest request) {
		Lecture lecture = new Lecture();

		lecture.setCreatorId(creatorId);
		lecture.setTitle(request.getTitle());
		lecture.setDescription(request.getDescription());
		lecture.setPrice(request.getPrice());
		lecture.setCapacity(request.getCapacity());
		lecture.setStartDate(request.getStartDate());
		lecture.setEndDate(request.getEndDate());
		lecture.setStatus(ClassStatus.DRAFT);
		lecture.setCurrentEnrollment(0);

		lectureRepository.save(lecture);
		
		return new ClassCreatedResponse(lecture.getId());
	}

	@Transactional
	public ClassStatusResponse updateStatus(long id, ClassStatus targetStatus) {
		Lecture lecture = lectureRepository.findById(id)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
		
		ClassStatus from = lecture.getStatus();

		if (!isTransitionAllowed(from, targetStatus)) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "허용되지 않는 상태 전이입니다.");
		}

		lecture.setStatus(targetStatus);

		return new ClassStatusResponse(lecture.getStatus());
	}

	private static boolean isTransitionAllowed(ClassStatus from, ClassStatus to) {
		return (from == ClassStatus.DRAFT && to == ClassStatus.OPEN)
				|| (from == ClassStatus.OPEN && to == ClassStatus.CLOSED);
	}

	private ClassListItemResponse toListItem(Lecture lecture) {
		return new ClassListItemResponse(
				lecture.getId(),
				lecture.getCreatorId(),
				lecture.getTitle(),
				lecture.getStatus(),
				lecture.getPrice(),
				lecture.getCapacity(),
				lecture.getStartDate(),
				lecture.getEndDate());
	}

	private ClassDetailResponse toDetail(Lecture lecture) {
		return new ClassDetailResponse(
				lecture.getId(),
				lecture.getCreatorId(),
				lecture.getTitle(),
				lecture.getDescription(),
				lecture.getStatus(),
				lecture.getPrice(),
				lecture.getCapacity(),
				lecture.getCurrentEnrollment(),
				lecture.getStartDate(),
				lecture.getEndDate());
	}

	private ClassConfirmedEnrollmentItemResponse toClassConfirmedItem(Enrollment e) {
		return new ClassConfirmedEnrollmentItemResponse(
				e.getId(),
				e.getUserId(),
				e.getStatus(),
				e.getConfirmedAt());
	}
}
