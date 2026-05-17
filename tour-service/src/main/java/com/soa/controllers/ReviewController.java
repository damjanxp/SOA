package com.soa.controllers;

import com.soa.dtos.CreateReviewRequest;
import com.soa.dtos.ReviewResponse;
import com.soa.services.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tours/{tourId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Create review for a tour (POST /api/tours/{tourId}/reviews)
     */
    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long tourId,
            @Valid @RequestBody CreateReviewRequest request,
            HttpServletRequest httpRequest) {
        
        String touristId = getUserIdFromRequest(httpRequest);
        ReviewResponse response = reviewService.createReview(tourId, touristId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all reviews for a tour (GET /api/tours/{tourId}/reviews)
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponse>> getReviews(@PathVariable Long tourId) {
        List<ReviewResponse> reviews = reviewService.getReviewsByTourId(tourId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Get review by ID (GET /api/tours/{tourId}/reviews/{reviewId})
     */
    @GetMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> getReviewById(
            @PathVariable Long tourId,
            @PathVariable Long reviewId) {
        
        ReviewResponse review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(review);
    }

    /**
     * Delete review (DELETE /api/tours/{tourId}/reviews/{reviewId})
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long tourId,
            @PathVariable Long reviewId,
            HttpServletRequest httpRequest) {
        
        String touristId = getUserIdFromRequest(httpRequest);
        reviewService.deleteReview(reviewId, touristId);
        return ResponseEntity.noContent().build();
    }

    private String getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("Unauthorized: User ID not found");
        }
        return (String) userIdObj;
    }
}
