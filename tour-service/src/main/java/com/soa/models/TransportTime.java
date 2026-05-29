package com.soa.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Tour tour;

    public enum TransportType {
        WALK, BIKE, CAR
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportType transportType;

    @Column(nullable = false)
    private Double timeMinutes;
}

