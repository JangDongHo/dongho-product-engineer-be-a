package io.github.jangdongho.productengineer.lecture.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

import io.github.jangdongho.productengineer.common.domain.BaseEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
		name = "classes",
		indexes = @Index(name = "ix_classes_status_id", columnList = "status,id")
)
public class Lecture extends BaseEntity {

	@Column(name = "creator_id", nullable = false)
	private Long creatorId;

	@Column(nullable = false, length = 255)
	private String title;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private long price;

	@Column(nullable = false)
	private int capacity;

	@Column(name = "start_date", nullable = false)
	private LocalDateTime startDate;

	@Column(name = "end_date", nullable = false)
	private LocalDateTime endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ClassStatus status = ClassStatus.DRAFT;

	@Column(name = "current_enrollment", nullable = false)
	private int currentEnrollment = 0;
}
