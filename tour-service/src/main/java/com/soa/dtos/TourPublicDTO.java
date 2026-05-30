package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Ograniceni prikaz ture za turiste — samo prva kljucna tacka
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPublicDTO {
    private Long id;
    private String name;
    private String description;
    private String difficulty;
    private List<String> tags;
    private BigDecimal price;
    private Double lengthKm;
    private LocalDateTime publishedAt;
    private String status;
    // Samo prva kljucna tacka
    private KeypointDTO firstKeypoint;
    private List<KeypointDTO> keypoints; // sadrzi samo prvu tacku
}

