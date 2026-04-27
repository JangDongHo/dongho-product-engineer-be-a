package io.github.jangdongho.productengineer.lecture.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.jangdongho.productengineer.lecture.domain.ClassStatus;
import io.github.jangdongho.productengineer.lecture.domain.Lecture;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

	List<Lecture> findAllByOrderByCreatedAtDesc();

	List<Lecture> findByStatusOrderByCreatedAtDesc(ClassStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select lecture from Lecture lecture where lecture.id = :id")
	Optional<Lecture> findByIdForUpdate(@Param("id") Long id);
}
