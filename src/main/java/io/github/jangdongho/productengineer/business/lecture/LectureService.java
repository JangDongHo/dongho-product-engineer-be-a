package io.github.jangdongho.productengineer.business.lecture;

import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import io.github.jangdongho.productengineer.persistence.lecture.Lecture;
import io.github.jangdongho.productengineer.persistence.lecture.LectureRepository;
import io.github.jangdongho.productengineer.presentation.lecture.ClassCreatedResponse;
import io.github.jangdongho.productengineer.presentation.lecture.CreateClassRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LectureService {

	private final LectureRepository lectureRepository;

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
}
