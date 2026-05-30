package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourResponse {
    private Long id;
    private String name;
    private String description;
    private String authorId;
    private String difficulty;
    private List<String> tags;
    private String status;
    private BigDecimal price;
    private Double lengthKm;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;
    private List<KeypointResponse> keypoints;
}
