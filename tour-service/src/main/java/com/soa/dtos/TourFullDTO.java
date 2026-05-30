package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Kompletan prikaz ture za kupce — sve kljucne tacke
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourFullDTO {
    private Long id;
    private String name;
    private String description;
    private String difficulty;
    private List<String> tags;
    private BigDecimal price;
    private Double lengthKm;
    private LocalDateTime publishedAt;
    private String status;
    private List<KeypointDTO> keypoints; // sve kljucne tacke
    private List<ReviewResponse> reviews;
}

