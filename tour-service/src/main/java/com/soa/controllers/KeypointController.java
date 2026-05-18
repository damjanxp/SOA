package com.soa.controllers;

import com.soa.dtos.CreateKeypointRequest;
import com.soa.dtos.KeypointResponse;
import com.soa.services.KeypointService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tours/{tourId}/keypoints")
@RequiredArgsConstructor
public class KeypointController {

    private final KeypointService keypointService;

    /**
     * Add keypoint to tour (POST /api/tours/{tourId}/keypoints)
     */
    @PostMapping
    public ResponseEntity<KeypointResponse> addKeypoint(
            @PathVariable Long tourId,
            @Valid @RequestBody CreateKeypointRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        KeypointResponse response = keypointService.addKeypoint(tourId, request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all keypoints for a tour (GET /api/tours/{tourId}/keypoints)
     */
    @GetMapping
    public ResponseEntity<List<KeypointResponse>> getKeypoints(@PathVariable Long tourId) {
        List<KeypointResponse> keypoints = keypointService.getKeypointsByTourId(tourId);
        return ResponseEntity.ok(keypoints);
    }

    /**
     * Get keypoint by ID (GET /api/tours/{tourId}/keypoints/{kpId})
     */
    @GetMapping("/{kpId}")
    public ResponseEntity<KeypointResponse> getKeypointById(
            @PathVariable Long tourId,
            @PathVariable Long kpId) {
        
        KeypointResponse keypoint = keypointService.getKeypointById(kpId);
        return ResponseEntity.ok(keypoint);
    }

    /**
     * Update keypoint (PUT /api/tours/{tourId}/keypoints/{kpId})
     */
    @PutMapping("/{kpId}")
    public ResponseEntity<KeypointResponse> updateKeypoint(
            @PathVariable Long tourId,
            @PathVariable Long kpId,
            @Valid @RequestBody CreateKeypointRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        KeypointResponse response = keypointService.updateKeypoint(tourId, kpId, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete keypoint (DELETE /api/tours/{tourId}/keypoints/{kpId})
     */
    @DeleteMapping("/{kpId}")
    public ResponseEntity<Void> deleteKeypoint(
            @PathVariable Long tourId,
            @PathVariable Long kpId,
            HttpServletRequest httpRequest) {
        
        String userId = getUserIdFromRequest(httpRequest);
        keypointService.deleteKeypoint(tourId, kpId, userId);
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
