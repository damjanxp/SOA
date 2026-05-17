package com.soa.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateKeypointRequest {
    @NotNull(message = "Latitude is required")
    private Double lat;

    @NotNull(message = "Longitude is required")
    private Double lon;

    @NotBlank(message = "Keypoint name is required")
    private String name;

    private String description;

    private String imageUrl;

    @NotNull(message = "Order index is required")
    private Integer orderIndex;
}
