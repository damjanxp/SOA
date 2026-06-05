package com.soa.dtos;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourExecutionResponse {
    private Long id;
    private String touristId;
    private Long tourId;
    private String status;
    private Double startLat;
    private Double startLong;
    private LocalDateTime startedAt;
    private LocalDateTime lastActivityAt;
    private LocalDateTime endedAt;
    private List<CompletedKeyPointResponse> completedKeyPoints;
}