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

        String userId = getUserId(httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transportTimeService.addTransportTime(tourId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<TransportTimeResponse>> getTransportTimes(@PathVariable Long tourId) {
        return ResponseEntity.ok(transportTimeService.getTransportTimes(tourId));
    }

    @DeleteMapping("/{ttId}")
    public ResponseEntity<Void> deleteTransportTime(
            @PathVariable Long tourId,
            @PathVariable Long ttId,
            HttpServletRequest httpRequest) {

        String userId = getUserId(httpRequest);
        transportTimeService.deleteTransportTime(tourId, ttId, userId);
        return ResponseEntity.noContent().build();
    }

    private String getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) throw new RuntimeException("Unauthorized: User ID not found");
        return (String) userId;
    }
}

