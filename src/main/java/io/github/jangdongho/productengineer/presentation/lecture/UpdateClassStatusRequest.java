package io.github.jangdongho.productengineer.presentation.lecture;

import io.github.jangdongho.productengineer.persistence.lecture.ClassStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateClassStatusRequest(@NotNull ClassStatus status) {
}
