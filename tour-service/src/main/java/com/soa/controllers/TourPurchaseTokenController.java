package com.soa.controllers;

import com.soa.dtos.TourPurchaseTokenResponse;
import com.soa.repositories.TourPurchaseTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchases")
@RequiredArgsConstructor
public class TourPurchaseTokenController {

    private final TourPurchaseTokenRepository tourPurchaseTokenRepository;

    /**
     * Get all purchase tokens for a tourist.
     * GET /api/purchases/{touristId}
     */
    @GetMapping("/{touristId}")
    public ResponseEntity<List<TourPurchaseTokenResponse>> getPurchases(
            @PathVariable String touristId) {

        List<TourPurchaseTokenResponse> tokens = tourPurchaseTokenRepository
                .findByTouristId(touristId)
                .stream()
                .map(t -> TourPurchaseTokenResponse.builder()
                        .id(t.getId())
                        .touristId(t.getTouristId())
                        .tourId(t.getTourId())
                        .token(t.getToken())
                        .purchasedAt(t.getPurchasedAt())
                        .build())
                .toList();

        return ResponseEntity.ok(tokens);
    }

    /**
     * Check whether a specific tour has been purchased by a tourist.
     * GET /api/purchases/{touristId}/{tourId}
     *
     * Returns { "purchased": true/false }
     */
    @GetMapping("/{touristId}/{tourId}")
    public ResponseEntity<Map<String, Boolean>> isPurchased(
            @PathVariable String touristId,
            @PathVariable Long tourId) {

        boolean purchased = tourPurchaseTokenRepository.existsByTouristIdAndTour_Id(touristId, tourId);
        return ResponseEntity.ok(Map.of("purchased", purchased));
    }
}
