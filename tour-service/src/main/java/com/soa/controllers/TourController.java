package com.soa.controllers;

import com.soa.dtos.CreateTourRequest;
import com.soa.dtos.TourResponse;
import com.soa.services.TourService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {

    private final TourService tourService;

    /**
     * Create a new tour (POST /api/tours)
     */
    @PostMapping
    public ResponseEntity<TourResponse> createTour(
            @Valid @RequestBody CreateTourRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        TourResponse response = tourService.createTour(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all published tours (GET /api/tours/published)
     */
    @GetMapping("/published")
    public ResponseEntity<List<TourResponse>> getPublishedTours() {
        List<TourResponse> tours = tourService.getPublishedTours();
        return ResponseEntity.ok(tours);
    }

    /**
     * Get all tours by current author (GET /api/tours/my)
     */
    @GetMapping("/my")
    public ResponseEntity<List<TourResponse>> getMyTours(HttpServletRequest httpRequest) {
        String userId = getUserIdFromRequest(httpRequest);
        List<TourResponse> tours = tourService.getToursByAuthor(userId);
        return ResponseEntity.ok(tours);
    }

    /**
     * Get tour by ID (GET /api/tours/{id})
     */
    @GetMapping("/{id}")
    public ResponseEntity<TourResponse> getTourById(@PathVariable Long id) {
        TourResponse tour = tourService.getTourById(id);
        return ResponseEntity.ok(tour);
    }

    /**
     * Update tour (PUT /api/tours/{id})
     */
    @PutMapping("/{id}")
    public ResponseEntity<TourResponse> updateTour(
            @PathVariable Long id,
            @Valid @RequestBody CreateTourRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        TourResponse response = tourService.updateTour(id, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete tour (DELETE /api/tours/{id})
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTour(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        tourService.deleteTour(id, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publish tour (POST /api/tours/{id}/publish)
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<TourResponse> publishTour(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        TourResponse response = tourService.publishTour(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Archive tour (POST /api/tours/{id}/archive)
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<TourResponse> archiveTour(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = getUserIdFromRequest(httpRequest);
        TourResponse response = tourService.archiveTour(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Reactivate tour (POST /api/tours/{id}/reactivate)
     */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<TourResponse> reactivateTour(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = getUserIdFromRequest(httpRequest);
        TourResponse response = tourService.reactivateTour(id, userId);
        return ResponseEntity.ok(response);
    }

    private String getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("Unauthorized: User ID not found");
        }
        return (String) userIdObj;
    }

    // private Long getUserIdFromRequest(HttpServletRequest request) {
    //     return 1L;
    // }
}
