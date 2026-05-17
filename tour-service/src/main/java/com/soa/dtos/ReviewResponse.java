package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long tourId;
    private String touristId;
    private Integer rating;
    private String comment;
    private LocalDateTime visitDate;
    private LocalDateTime createdAt;
    private List<String> images;
}
