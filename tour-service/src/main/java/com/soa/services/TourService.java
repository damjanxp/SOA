package com.soa.services;

import com.soa.dtos.CreateTourRequest;
import com.soa.dtos.KeypointResponse;
import com.soa.dtos.TourResponse;
import com.soa.models.Keypoint;
import com.soa.models.Tour;
import com.soa.repositories.KeypointRepository;
import com.soa.repositories.TourRepository;
import com.soa.repositories.TransportTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TourService {

    private final TourRepository tourRepository;
    private final KeypointRepository keypointRepository;
    private final TransportTimeRepository transportTimeRepository;

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
     * Get all published tours
     */
    public List<TourResponse> getPublishedTours() {
        return tourRepository.findByStatus(Tour.TourStatus.PUBLISHED)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get tour by ID
     */
    public TourResponse getTourById(Long tourId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));
        return mapToResponse(tour);
    }

    /**
     * Update tour
     */
    public TourResponse updateTour(Long tourId, CreateTourRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unauthorized: Only tour author can update");
        }

        tour.setName(request.getName());
        tour.setDescription(request.getDescription());
        tour.setDifficulty(Tour.Difficulty.valueOf(request.getDifficulty().toUpperCase()));
        tour.setTags(request.getTags() != null ? request.getTags() : List.of());
        tour.setLengthKm(request.getLengthKm());

        return mapToResponse(tourRepository.save(tour));
    }

    /**
     * Delete tour
     */
    public void deleteTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unauthorized: Only tour author can delete");
        }

        tourRepository.delete(tour);
    }

    /**
     * Publish tour — HTTP endpoint
     * Ista validacija kao u TourGrpcService.publishTour()
     */
    public TourResponse publishTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));

        // Provjera vlasnistva
        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unauthorized: Only tour author can publish");
        }

        // Provjera 1: tura mora biti u DRAFT statusu
        if (tour.getStatus() != Tour.TourStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only draft tours can be published");
        }

        // Provjera 2: minimum 2 kljucne tacke
        List<Keypoint> keypoints = keypointRepository.findByTourId(tour.getId());
        if (keypoints == null || keypoints.size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tour must have at least 2 keypoints. Current count: "
                            + (keypoints == null ? 0 : keypoints.size()));
        }

        // Provjera 3: minimum 1 transport vreme
        long transportCount = transportTimeRepository.countByTourId(tour.getId());
        if (transportCount < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tour must have at least one transport time (WALK, BIKE or CAR) before publishing");
        }

        // Provjera 4: obavezna polja
        if (tour.getName() == null || tour.getName().isBlank()
                || tour.getDescription() == null || tour.getDescription().isBlank()
                || tour.getDifficulty() == null
                || tour.getTags() == null || tour.getTags().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tour is missing required fields: name, description, difficulty or tags");
        }

        // Sve validacije prosle — objavi turu
        tour.setStatus(Tour.TourStatus.PUBLISHED);
        tour.setPublishedAt(LocalDateTime.now());
        return mapToResponse(tourRepository.save(tour));
    }

    /**
     * Archive tour
     */
    public TourResponse archiveTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unauthorized: Only tour author can archive");
        }

        // Tura mora biti PUBLISHED prije arhiviranja
        if (tour.getStatus() != Tour.TourStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only published tours can be archived");
        }

        tour.setStatus(Tour.TourStatus.ARCHIVED);
        tour.setArchivedAt(LocalDateTime.now());
        return mapToResponse(tourRepository.save(tour));
    }

    /**
     * Reactivate tour (ARCHIVED -> PUBLISHED)
     */
    public TourResponse reactivateTour(Long tourId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Unauthorized: Only tour author can reactivate");
        }

        // Tura mora biti ARCHIVED prije reaktiviranja
        if (tour.getStatus() != Tour.TourStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Only archived tours can be reactivated");
        }

        tour.setStatus(Tour.TourStatus.PUBLISHED);
        tour.setArchivedAt(null);
        return mapToResponse(tourRepository.save(tour));
    }

    // ─── Mapper ──────────────────────────────────────────────────────────────────

    private TourResponse mapToResponse(Tour tour) {
        List<KeypointResponse> keypointResponses = tour.getKeypoints() != null
                ? tour.getKeypoints().stream()
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
                .keypoints(keypointResponses)
                .build();
    }
}
