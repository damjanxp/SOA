package com.soa.services;

import com.soa.dtos.CreateTransportTimeRequest;
import com.soa.dtos.TransportTimeResponse;
import com.soa.models.Tour;
import com.soa.models.TransportTime;
import com.soa.repositories.TourRepository;
import com.soa.repositories.TransportTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportTimeService {

    private final TransportTimeRepository transportTimeRepository;
    private final TourRepository tourRepository;

    public TransportTimeResponse addTransportTime(Long tourId, CreateTransportTimeRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can add transport times");
        }

        TransportTime transportTime = TransportTime.builder()
                .tour(tour)
                .transportType(parseTransportType(request.getTransportType()))
                .durationMinutes(request.getDurationMinutes())
                .build();

        return mapToResponse(transportTimeRepository.save(transportTime));
    }

    @Transactional(readOnly = true)
    public List<TransportTimeResponse> getTransportTimesForTour(Long tourId) {
        if (!tourRepository.existsById(tourId)) {
            throw new RuntimeException("Tour not found with id: " + tourId);
        }
        return transportTimeRepository.findByTourId(tourId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TransportTimeResponse updateTransportTime(Long tourId, Long id,
                                                     CreateTransportTimeRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can update transport times");
        }

        TransportTime transportTime = transportTimeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TransportTime not found with id: " + id));

        if (!transportTime.getTour().getId().equals(tourId)) {
            throw new RuntimeException("TransportTime does not belong to this tour");
        }

        transportTime.setTransportType(parseTransportType(request.getTransportType()));
        transportTime.setDurationMinutes(request.getDurationMinutes());

        return mapToResponse(transportTimeRepository.save(transportTime));
    }

    public void deleteTransportTime(Long tourId, Long id, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Unauthorized: Only tour author can delete transport times");
        }

        TransportTime transportTime = transportTimeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("TransportTime not found with id: " + id));

        if (!transportTime.getTour().getId().equals(tourId)) {
            throw new RuntimeException("TransportTime does not belong to this tour");
        }

        transportTimeRepository.delete(transportTime);
    }

    private TransportTime.TransportType parseTransportType(String type) {
        try {
            return TransportTime.TransportType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid transport type: " + type + ". Must be one of: WALKING, BICYCLE, CAR");
        }
    }

    private TransportTimeResponse mapToResponse(TransportTime tt) {
        return TransportTimeResponse.builder()
                .id(tt.getId())
                .tourId(tt.getTour().getId())
                .transportType(tt.getTransportType().name())
                .durationMinutes(tt.getDurationMinutes())
                .build();
    }
}
