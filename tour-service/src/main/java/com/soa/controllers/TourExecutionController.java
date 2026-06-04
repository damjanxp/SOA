package com.soa.controllers;

import com.soa.dtos.CheckNearbyRequest;
import com.soa.dtos.CheckNearbyResponse;
import com.soa.dtos.StartExecutionRequest;
import com.soa.dtos.TourExecutionResponse;
import com.soa.services.TourExecutionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tour-execution")
@RequiredArgsConstructor
public class TourExecutionController {

    private final TourExecutionService executionService;

    /**
     * Pokreni turu — kreira novu TourExecution sesiju.
     * POST /api/tour-execution/start
     * Body: { tourId, startLat, startLong }
     */
    @PostMapping("/start")
    public ResponseEntity<TourExecutionResponse> startExecution(
            @Valid @RequestBody StartExecutionRequest request,
            HttpServletRequest httpRequest) {

        String touristId = getUserId(httpRequest);
        TourExecutionResponse response = executionService.startExecution(touristId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Provjeri blizinu ključnih tačaka i ažuriraj lastActivityAt.
     * POST /api/tour-execution/{executionId}/check-nearby
     * Body: { lat, lon }
     */
    @PostMapping("/{executionId}/check-nearby")
    public ResponseEntity<CheckNearbyResponse> checkNearby(
            @PathVariable Long executionId,
            @Valid @RequestBody CheckNearbyRequest request) {

        CheckNearbyResponse response = executionService.checkNearby(executionId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Završi turu (status → COMPLETED).
     * POST /api/tour-execution/{executionId}/complete
     */
    @PostMapping("/{executionId}/complete")
    public ResponseEntity<TourExecutionResponse> completeExecution(
            @PathVariable Long executionId,
            HttpServletRequest httpRequest) {

        String touristId = getUserId(httpRequest);
        TourExecutionResponse response = executionService.completeExecution(executionId, touristId);
        return ResponseEntity.ok(response);
    }

    /**
     * Napusti turu (status → ABANDONED).
     * POST /api/tour-execution/{executionId}/abandon
     */
    @PostMapping("/{executionId}/abandon")
    public ResponseEntity<TourExecutionResponse> abandonExecution(
            @PathVariable Long executionId,
            HttpServletRequest httpRequest) {

        String touristId = getUserId(httpRequest);
        TourExecutionResponse response = executionService.abandonExecution(executionId, touristId);
        return ResponseEntity.ok(response);
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private String getUserId(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("Unauthorized: User ID not found");
        }
        return (String) userIdObj;
    }
}