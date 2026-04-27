package io.github.jangdongho.productengineer.lecture.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.domain.Lecture;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

	List<Lecture> findAllByOrderByCreatedAtDesc();

	List<Lecture> findByStatusOrderByCreatedAtDesc(ClassStatus status);
}
