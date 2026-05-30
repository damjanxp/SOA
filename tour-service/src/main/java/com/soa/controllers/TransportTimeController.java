package com.soa.controllers;

import com.soa.dtos.CreateTransportTimeRequest;
import com.soa.dtos.TransportTimeResponse;
import com.soa.services.TransportTimeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tours/{tourId}/transport-times")
@RequiredArgsConstructor
public class TransportTimeController {

    private final TransportTimeService transportTimeService;

    @PostMapping
    public ResponseEntity<TransportTimeResponse> addTransportTime(
            @PathVariable Long tourId,
            @Valid @RequestBody CreateTransportTimeRequest request,
            HttpServletRequest httpRequest) {

        String userId = getUserIdFromRequest(httpRequest);
        TransportTimeResponse response = transportTimeService.addTransportTime(tourId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TransportTimeResponse>> getTransportTimes(@PathVariable Long tourId) {
        List<TransportTimeResponse> times = transportTimeService.getTransportTimesForTour(tourId);
        return ResponseEntity.ok(times);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransportTimeResponse> updateTransportTime(
            @PathVariable Long tourId,
            @PathVariable Long id,
            @Valid @RequestBody CreateTransportTimeRequest request,
            HttpServletRequest httpRequest) {

        String userId = getUserIdFromRequest(httpRequest);
        TransportTimeResponse response = transportTimeService.updateTransportTime(tourId, id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransportTime(
            @PathVariable Long tourId,
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        String userId = getUserIdFromRequest(httpRequest);
        transportTimeService.deleteTransportTime(tourId, id, userId);
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
