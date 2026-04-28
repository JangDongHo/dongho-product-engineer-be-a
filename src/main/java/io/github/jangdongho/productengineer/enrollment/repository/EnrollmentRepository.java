package io.github.jangdongho.productengineer.enrollment.repository;

import io.github.jangdongho.productengineer.enrollment.domain.Enrollment;
import io.github.jangdongho.productengineer.enrollment.domain.EnrollmentStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select enrollment from Enrollment enrollment where enrollment.id = :id")
  Optional<Enrollment> findByIdForUpdate(@Param("id") Long id);

  boolean existsByUserIdAndClassId(Long userId, Long classId);

  Page<Enrollment> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

  Page<Enrollment> findByClassIdAndStatusOrderByCreatedAtDescIdDesc(
      Long classId, EnrollmentStatus status, Pageable pageable);
}
