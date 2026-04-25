package io.github.jangdongho.productengineer.presentation.lecture;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateClassRequest {

	@NotBlank
	@Size(max = 255)
	private String title;

	@NotBlank
	private String description;

	@NotNull
	@Positive
	private Long price;

	@NotNull
	@Min(1)
	@Max(10_000)
	private Integer capacity;

	@NotNull
	private LocalDateTime startDate;

	@NotNull
	private LocalDateTime endDate;

	@AssertTrue(message = "must be after startDate")
	public boolean isValidDateRange() {
		if (startDate == null || endDate == null) {
			return true;
		}
		return endDate.isAfter(startDate);
	}
}
