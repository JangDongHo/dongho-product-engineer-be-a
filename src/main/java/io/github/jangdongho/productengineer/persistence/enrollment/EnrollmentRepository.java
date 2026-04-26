package io.github.jangdongho.productengineer.persistence.enrollment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByUserIdAndClassId(Long userId, Long classId);

	List<Enrollment> findByUserIdOrderByCreatedAtDescIdDesc(Long userId);
}
