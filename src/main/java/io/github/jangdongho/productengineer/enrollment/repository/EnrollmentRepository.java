package io.github.jangdongho.productengineer.enrollment.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByUserIdAndClassId(Long userId, Long classId);

	List<Enrollment> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
