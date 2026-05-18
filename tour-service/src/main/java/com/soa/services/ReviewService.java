package com.soa.services;

import com.soa.dtos.CreateReviewRequest;
import com.soa.dtos.ReviewResponse;
import com.soa.models.Review;
import com.soa.models.Tour;
import com.soa.repositories.ReviewRepository;
import com.soa.repositories.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final TourRepository tourRepository;

    /**
     * Create review for a tour (only tourists)
     */
    public ReviewResponse createReview(Long tourId, String touristId, CreateReviewRequest request) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        Review review = Review.builder()
                .tour(tour)
                .touristId(touristId)
                .rating(request.getRating())
                .comment(request.getComment())
                .visitDate(request.getVisitDate())
                .images(request.getImages() != null ? request.getImages() : List.of())
                .build();

        Review savedReview = reviewRepository.save(review);
        return mapToResponse(savedReview);
    }

    /**
     * Get all reviews for a tour
     */
    public List<ReviewResponse> getReviewsByTourId(Long tourId) {
        // Verify tour exists
        tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        return reviewRepository.findByTourIdOrderByCreatedAtDesc(tourId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get all reviews by a tourist
     */
    public List<ReviewResponse> getReviewsByTouristId(String touristId) {
        return reviewRepository.findByTouristId(touristId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get review by ID
     */
    public ReviewResponse getReviewById(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));
        return mapToResponse(review);
    }

    /**
     * Delete review (only by the tourist who created it)
     */
    public void deleteReview(Long reviewId, String touristId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found with id: " + reviewId));

        if (!review.getTouristId().equals(touristId)) {
            throw new RuntimeException("Unauthorized: Only review creator can delete");
        }

        reviewRepository.delete(review);
    }

    private ReviewResponse mapToResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .tourId(review.getTour().getId())
                .touristId(review.getTouristId())
                .rating(review.getRating())
                .comment(review.getComment())
                .visitDate(review.getVisitDate())
                .createdAt(review.getCreatedAt())
                .images(review.getImages())
                .build();
    }
}
