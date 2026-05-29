package com.soa.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransportTimeRequest {

    @NotNull(message = "Transport type is required (WALK, BIKE, CAR)")
    private String transportType;

    @NotNull(message = "Time in minutes is required")
    @Positive(message = "Time must be positive")
    private Double timeMinutes;
}

