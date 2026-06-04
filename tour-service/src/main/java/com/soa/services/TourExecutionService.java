package com.soa.services;

import com.soa.dtos.*;
import com.soa.models.CompletedKeyPoint;
import com.soa.models.Keypoint;
import com.soa.models.Tour;
import com.soa.models.TourExecution;
import com.soa.repositories.*;
import com.soa.util.HaversineUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TourExecutionService {

    // Radius u km unutar kojeg se smatra da je turista "blizu" tacke
    private static final double PROXIMITY_RADIUS_KM = 0.1; // 100 metara

    private final TourExecutionRepository executionRepository;
    private final CompletedKeyPointRepository completedKeyPointRepository;
    private final TourRepository tourRepository;
    private final KeypointRepository keypointRepository;
    private final TourPurchaseTokenRepository purchaseTokenRepository;

    /**
     * Pokrece novu TourExecution sesiju.
     * Preduslov: tura mora biti kupljena i mora biti PUBLISHED ili ARCHIVED.
     */
    public TourExecutionResponse startExecution(String touristId, StartExecutionRequest request) {

        Long tourId = request.getTourId();

        // 1. Provjera da li tura postoji
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));

        // 2. Tura mora biti PUBLISHED ili ARCHIVED (ne DRAFT)
        if (tour.getStatus() == Tour.TourStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot start a draft tour");
        }

        // 3. Preduslov: tura mora biti kupljena
        boolean purchased = purchaseTokenRepository.existsByTouristIdAndTour_Id(touristId, tourId);
        if (!purchased) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You must purchase this tour before starting it");
        }

        // 4. Kreiranje sesije
        TourExecution execution = TourExecution.builder()
                .touristId(touristId)
                .tourId(tourId)
                .status(TourExecution.ExecutionStatus.ACTIVE)
                .startLat(request.getStartLat())
                .startLong(request.getStartLong())
                .build();

        TourExecution saved = executionRepository.save(execution);
        return mapToResponse(saved);
    }

    /**
     * Provjerava da li je turista blizu neke ključne tačke.
     * Uvijek ažurira lastActivityAt, a ako je blizu — bilježi CompletedKeyPoint.
     */
    public CheckNearbyResponse checkNearby(Long executionId, CheckNearbyRequest request) {

        TourExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Execution not found with id: " + executionId));

        if (execution.getStatus() != TourExecution.ExecutionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tour execution is not active");
        }

        // Uvijek azuriraj lastActivityAt
        execution.setLastActivityAt(LocalDateTime.now());

        List<Keypoint> keypoints = keypointRepository.findByTourIdOrderByOrderIndex(execution.getTourId());

        Long foundKeyPointId = null;

        for (Keypoint kp : keypoints) {
            // Preskoci vec kompletiranu tacku
            if (completedKeyPointRepository.existsByTourExecutionIdAndKeyPointId(executionId, kp.getId())) {
                continue;
            }

            double distanceKm = HaversineUtil.calculateDistance(
                    request.getLat(), request.getLon(),
                    kp.getLat(), kp.getLon()
            );

            if (distanceKm <= PROXIMITY_RADIUS_KM) {
                // Turista je dostigao ovu tacku — evidentiraj
                CompletedKeyPoint completed = CompletedKeyPoint.builder()
                        .tourExecution(execution)
                        .keyPointId(kp.getId())
                        .build();
                completedKeyPointRepository.save(completed);
                execution.getCompletedKeyPoints().add(completed);
                foundKeyPointId = kp.getId();
                break; // jedna tacka po pozivu
            }
        }

        executionRepository.save(execution);

        return CheckNearbyResponse.builder()
                .nearbyFound(foundKeyPointId != null)
                .keyPointId(foundKeyPointId)
                .build();
    }

    /**
     * Turista završava turu (status → COMPLETED).
     */
    public TourExecutionResponse completeExecution(Long executionId, String touristId) {
        TourExecution execution = getActiveExecution(executionId, touristId);
        execution.setStatus(TourExecution.ExecutionStatus.COMPLETED);
        execution.setEndedAt(LocalDateTime.now());
        execution.setLastActivityAt(LocalDateTime.now());
        return mapToResponse(executionRepository.save(execution));
    }

    /**
     * Turista napušta turu (status → ABANDONED).
     */
    public TourExecutionResponse abandonExecution(Long executionId, String touristId) {
        TourExecution execution = getActiveExecution(executionId, touristId);
        execution.setStatus(TourExecution.ExecutionStatus.ABANDONED);
        execution.setEndedAt(LocalDateTime.now());
        execution.setLastActivityAt(LocalDateTime.now());
        return mapToResponse(executionRepository.save(execution));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private TourExecution getActiveExecution(Long executionId, String touristId) {
        TourExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Execution not found with id: " + executionId));

        if (!execution.getTouristId().equals(touristId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "This execution does not belong to you");
        }

        if (execution.getStatus() != TourExecution.ExecutionStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tour execution is not active (current status: " + execution.getStatus() + ")");
        }

        return execution;
    }

    private TourExecutionResponse mapToResponse(TourExecution exec) {
        List<CompletedKeyPointResponse> completedList = exec.getCompletedKeyPoints() == null
                ? List.of()
                : exec.getCompletedKeyPoints().stream()
                        .map(c -> CompletedKeyPointResponse.builder()
                                .id(c.getId())
                                .keyPointId(c.getKeyPointId())
                                .completedAt(c.getCompletedAt())
                                .build())
                        .collect(Collectors.toList());

        return TourExecutionResponse.builder()
                .id(exec.getId())
                .touristId(exec.getTouristId())
                .tourId(exec.getTourId())
                .status(exec.getStatus().name())
                .startLat(exec.getStartLat())
                .startLong(exec.getStartLong())
                .startedAt(exec.getStartedAt())
                .lastActivityAt(exec.getLastActivityAt())
                .endedAt(exec.getEndedAt())
                .completedKeyPoints(completedList)
                .build();
    }
}