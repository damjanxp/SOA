package com.soa.services;

import com.soa.dtos.CreateTourRequest;
import com.soa.dtos.KeypointResponse;
import com.soa.dtos.TourResponse;
import com.soa.models.Tour;
import com.soa.repositories.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TourService {

    private final TourRepository tourRepository;

    /**
     * Create a new tour (initially with DRAFT status and price 0)
     */
    public TourResponse createTour(String authorId, CreateTourRequest request) {
        Tour tour = Tour.builder()
                .authorId(authorId)
                .name(request.getName())
                .description(request.getDescription())
                .difficulty(Tour.Difficulty.valueOf(request.getDifficulty().toUpperCase()))
                .tags(request.getTags() != null ? request.getTags() : List.of())
                .status(Tour.TourStatus.DRAFT)
                .price(BigDecimal.ZERO)
                .lengthKm(request.getLengthKm())
                .build();

        Tour savedTour = tourRepository.save(tour);
        return mapToResponse(savedTour);
    }

    /**
     * Get all tours by author ID
     */
    public List<TourResponse> getToursByAuthor(String authorId) {
        return tourRepository.findByAuthorId(authorId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get tour by ID
     */
    public TourResponse getTourById(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));
        return mapToResponse(tour);
    }

    /**
     * Update tour
     */
    public TourResponse updateTour(Long tourId, CreateTourRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        // Only author can update
        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can update");
        }

        tour.setName(request.getName());
        tour.setDescription(request.getDescription());
        tour.setDifficulty(Tour.Difficulty.valueOf(request.getDifficulty().toUpperCase()));
        tour.setTags(request.getTags() != null ? request.getTags() : List.of());
        tour.setLengthKm(request.getLengthKm());

        Tour updatedTour = tourRepository.save(tour);
        return mapToResponse(updatedTour);
    }

    /**
     * Delete tour
     */
    public void deleteTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        // Only author can delete
        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can delete");
        }

        tourRepository.delete(tour);
    }

    /**
     * Publish tour
     */
    public TourResponse publishTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can publish");
        }

        tour.setStatus(Tour.TourStatus.PUBLISHED);
        Tour updatedTour = tourRepository.save(tour);
        return mapToResponse(updatedTour);
    }

    /**
     * Archive tour
     */
    public TourResponse archiveTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can archive");
        }

        tour.setStatus(Tour.TourStatus.ARCHIVED);
        tour.setArchivedAt(java.time.LocalDateTime.now());
        Tour updatedTour = tourRepository.save(tour);
        return mapToResponse(updatedTour);
    }

    /**
     * Reactivate tour (ARCHIVED → PUBLISHED)
     */
    public TourResponse reactivateTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can reactivate");
        }

        tour.setStatus(Tour.TourStatus.PUBLISHED);
        tour.setArchivedAt(null);
        Tour updatedTour = tourRepository.save(tour);
        return mapToResponse(updatedTour);
    }

    private TourResponse mapToResponse(Tour tour) {
        List<KeypointResponse> keypointResponses = tour.getKeypoints() != null ?
                tour.getKeypoints().stream()
                        .map(kp -> KeypointResponse.builder()
                                .id(kp.getId())
                                .lat(kp.getLat())
                                .lon(kp.getLon())
                                .name(kp.getName())
                                .description(kp.getDescription())
                                .imageUrl(kp.getImageUrl())
                                .orderIndex(kp.getOrderIndex())
                                .build())
                        .collect(Collectors.toList())
                : List.of();

        return TourResponse.builder()
                .id(tour.getId())
                .name(tour.getName())
                .description(tour.getDescription())
                .authorId(tour.getAuthorId())
                .difficulty(tour.getDifficulty().toString())
                .tags(tour.getTags())
                .status(tour.getStatus().toString())
                .price(tour.getPrice())
                .lengthKm(tour.getLengthKm())
                .createdAt(tour.getCreatedAt())
                .updatedAt(tour.getUpdatedAt())
                .archivedAt(tour.getArchivedAt())
                .keypoints(keypointResponses)
                .build();
    }

    public List<TourResponse> getPublishedTours() {
        return tourRepository.findByStatus(Tour.TourStatus.PUBLISHED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
}
