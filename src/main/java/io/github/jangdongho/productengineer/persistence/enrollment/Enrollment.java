package io.github.jangdongho.productengineer.persistence.enrollment;

import io.github.jangdongho.productengineer.persistence.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "enrollments",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_enrollments_user_id_class_id",
				columnNames = {"user_id", "class_id"}),
		indexes = @Index(name = "ix_enrollments_user_id", columnList = "user_id"))
public class Enrollment extends BaseEntity {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "class_id", nullable = false)
	private Long classId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private EnrollmentStatus status = EnrollmentStatus.PENDING;

	@Column(name = "confirmed_at")
	private LocalDateTime confirmedAt;
}
