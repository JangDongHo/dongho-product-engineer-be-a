package io.github.jangdongho.productengineer.enrollment.repository;

import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  boolean existsByUserIdAndClassId(Long userId, Long classId);

  Page<Enrollment> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

  Page<Enrollment> findByClassIdAndStatusOrderByCreatedAtDescIdDesc(
      Long classId, EnrollmentStatus status, Pageable pageable);
}
