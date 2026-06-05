package com.soa.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tour_executions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String touristId;

    @Column(nullable = false)
    private Long tourId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status = ExecutionStatus.ACTIVE;

    @Column(nullable = false)
    private Double startLat;

    @Column(nullable = false)
    private Double startLong;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column(nullable = false)
    private LocalDateTime lastActivityAt;

    @Column
    private LocalDateTime endedAt;

    @OneToMany(mappedBy = "tourExecution", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CompletedKeyPoint> completedKeyPoints = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        lastActivityAt = LocalDateTime.now();
    }

    public enum ExecutionStatus {
        ACTIVE, COMPLETED, ABANDONED
    }
}