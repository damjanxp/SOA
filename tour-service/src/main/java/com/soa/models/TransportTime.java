package com.soa.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "transport_times")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tour_id", nullable = false)
    @JsonBackReference
    private Tour tour;

    @NotNull(message = "Transport type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false)
    private TransportType transportType;

    @NotNull(message = "Duration in minutes is required")
    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    public enum TransportType {
        WALKING, BICYCLE, CAR
    }
}
