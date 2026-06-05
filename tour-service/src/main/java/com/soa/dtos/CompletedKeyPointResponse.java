package com.soa.dtos;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompletedKeyPointResponse {
    private Long id;
    private Long keyPointId;
    private LocalDateTime completedAt;
}