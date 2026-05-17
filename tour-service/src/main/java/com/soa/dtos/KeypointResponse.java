package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KeypointResponse {
    private Long id;
    private Double lat;
    private Double lon;
    private String name;
    private String description;
    private String imageUrl;
    private Integer orderIndex;
}
