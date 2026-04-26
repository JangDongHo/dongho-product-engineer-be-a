package io.github.jangdongho.productengineer.persistence.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

	boolean existsByUserIdAndClassId(Long userId, Long classId);
}
