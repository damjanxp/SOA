package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportTimeResponse {
    private Long id;
    private Long tourId;
    private String transportType;
    private Double timeMinutes;
}

