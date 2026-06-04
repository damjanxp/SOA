package com.soa.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckNearbyRequest {

    @NotNull
    private Double lat;

    @NotNull
    private Double lon;
}