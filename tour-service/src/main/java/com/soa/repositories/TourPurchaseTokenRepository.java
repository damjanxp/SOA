package com.soa.repositories;

import com.soa.models.TourPurchaseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TourPurchaseTokenRepository extends JpaRepository<TourPurchaseToken, Long> {
    boolean existsByTouristIdAndTourId(String touristId, Long tourId);
}

