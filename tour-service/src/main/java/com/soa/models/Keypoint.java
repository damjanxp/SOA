package com.soa.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "keypoints")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Keypoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonBackReference
    private Tour tour;

    @NotNull(message = "Latitude is required")
    @Column(nullable = false)
    private Double lat;

    @NotNull(message = "Longitude is required")
    @Column(name = "lon", nullable = false)
    private Double lon;

    @NotBlank(message = "Keypoint name is required")
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
