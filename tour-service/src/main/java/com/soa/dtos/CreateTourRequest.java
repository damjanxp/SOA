package com.soa.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTourRequest {
    @NotBlank(message = "Tour name is required")
    private String name;

    private String description;

    @NotNull(message = "Difficulty is required")
    private String difficulty;

    private List<String> tags;

    private Double lengthKm;

    @PositiveOrZero(message = "Price cannot be negative")
    private BigDecimal price;
}
