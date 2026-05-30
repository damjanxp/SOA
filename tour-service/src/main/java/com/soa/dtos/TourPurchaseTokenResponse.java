package com.soa.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPurchaseTokenResponse {
    private Long id;
    private String touristId;
    private Long tourId;
    private String token;
    private LocalDateTime purchasedAt;
}
