package com.soa.services;

import com.soa.dtos.CreateTransportTimeRequest;
import com.soa.dtos.TransportTimeResponse;
import com.soa.models.Tour;
import com.soa.models.TransportTime;
import com.soa.repositories.TourRepository;
import com.soa.repositories.TransportTimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportTimeService {

    private final TourRepository tourRepository;
    private final TransportTimeRepository transportTimeRepository;

    public TransportTimeResponse addTransportTime(Long tourId, CreateTransportTimeRequest request, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only tour author can add transport times");
        }

        TransportTime.TransportType type;
        try {
            type = TransportTime.TransportType.valueOf(request.getTransportType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid transport type. Valid values: WALK, BIKE, CAR");
        }

        TransportTime tt = TransportTime.builder()
                .tour(tour)
                .transportType(type)
                .timeMinutes(request.getTimeMinutes())
                .build();

        return mapToResponse(transportTimeRepository.save(tt));
    }

    public List<TransportTimeResponse> getTransportTimes(Long tourId) {
        return transportTimeRepository.findByTourId(tourId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void deleteTransportTime(Long tourId, Long ttId, String authorId) {
        Tour tour = tourRepository.findById(tourId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tour not found"));

        if (!tour.getAuthorId().equals(authorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only tour author can delete transport times");
        }

        TransportTime tt = transportTimeRepository.findById(ttId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transport time not found"));

        transportTimeRepository.delete(tt);
    }

    private TransportTimeResponse mapToResponse(TransportTime tt) {
        return TransportTimeResponse.builder()
                .id(tt.getId())
                .tourId(tt.getTour().getId())
                .transportType(tt.getTransportType().name())
                .timeMinutes(tt.getTimeMinutes())
                .build();
    }
}

