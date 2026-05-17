package com.soa.services;

import com.soa.dtos.CreateKeypointRequest;
import com.soa.dtos.KeypointResponse;
import com.soa.models.Keypoint;
import com.soa.models.Tour;
import com.soa.repositories.KeypointRepository;
import com.soa.repositories.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class KeypointService {

    private final KeypointRepository keypointRepository;
    private final TourRepository tourRepository;

    /**
     * Add keypoint to tour
     */
    public KeypointResponse addKeypoint(Long tourId, CreateKeypointRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        // Only author can add keypoints
        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can add keypoints");
        }

        Keypoint keypoint = Keypoint.builder()
                .tour(tour)
                .lat(request.getLat())
                .lon(request.getLon())
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .orderIndex(request.getOrderIndex())
                .build();

        Keypoint savedKeypoint = keypointRepository.save(keypoint);
        return mapToResponse(savedKeypoint);
    }

    /**
     * Get all keypoints for a tour
     */
    public List<KeypointResponse> getKeypointsByTourId(Long tourId) {
        return keypointRepository.findByTourIdOrderByOrderIndex(tourId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get keypoint by ID
     */
    public KeypointResponse getKeypointById(Long keypointId) {
        Keypoint keypoint = keypointRepository.findById(keypointId)
                .orElseThrow(() -> new RuntimeException("Keypoint not found with id: " + keypointId));
        return mapToResponse(keypoint);
    }

    /**
     * Update keypoint
     */
    public KeypointResponse updateKeypoint(Long tourId, Long keypointId, 
                                          CreateKeypointRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can update keypoints");
        }

        Keypoint keypoint = keypointRepository.findById(keypointId)
                .orElseThrow(() -> new RuntimeException("Keypoint not found with id: " + keypointId));

        if (!keypoint.getTour().getId().equals(tourId)) {
            throw new RuntimeException("Keypoint does not belong to this tour");
        }

        keypoint.setLat(request.getLat());
        keypoint.setLon(request.getLon());
        keypoint.setName(request.getName());
        keypoint.setDescription(request.getDescription());
        keypoint.setImageUrl(request.getImageUrl());
        keypoint.setOrderIndex(request.getOrderIndex());

        Keypoint updatedKeypoint = keypointRepository.save(keypoint);
        return mapToResponse(updatedKeypoint);
    }

    /**
     * Delete keypoint
     */
    public void deleteKeypoint(Long tourId, Long keypointId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can delete keypoints");
        }

        Keypoint keypoint = keypointRepository.findById(keypointId)
                .orElseThrow(() -> new RuntimeException("Keypoint not found with id: " + keypointId));

        if (!keypoint.getTour().getId().equals(tourId)) {
            throw new RuntimeException("Keypoint does not belong to this tour");
        }

        keypointRepository.delete(keypoint);
    }

    private KeypointResponse mapToResponse(Keypoint keypoint) {
        return KeypointResponse.builder()
                .id(keypoint.getId())
                .lat(keypoint.getLat())
                .lon(keypoint.getLon())
                .name(keypoint.getName())
                .description(keypoint.getDescription())
                .imageUrl(keypoint.getImageUrl())
                .orderIndex(keypoint.getOrderIndex())
                .build();
    }
}
