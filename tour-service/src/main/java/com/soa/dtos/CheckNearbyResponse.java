package com.soa.dtos;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckNearbyResponse {
    private boolean nearbyFound;
    private Long keyPointId;   // null ako nije pronadjena bliska tacka
}