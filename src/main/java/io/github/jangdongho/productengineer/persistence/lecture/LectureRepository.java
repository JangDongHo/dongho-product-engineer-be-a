package io.github.jangdongho.productengineer.persistence.lecture;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LectureRepository extends JpaRepository<Lecture, Long> {

	List<Lecture> findAllByOrderByIdAsc();

	List<Lecture> findByStatusOrderByIdAsc(ClassStatus status);
}
