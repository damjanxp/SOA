package com.soa.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tour_purchase_tokens",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tourist_id", "tour_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TourPurchaseToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tourist_id", nullable = false)
    private String touristId;

    @Column(name = "tour_id", nullable = false)
    private Long tourId;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime purchasedAt;

    @PrePersist
    protected void onCreate() {
        if (token == null) {
            token = UUID.randomUUID().toString();
        }
        purchasedAt = LocalDateTime.now();
    }
}
