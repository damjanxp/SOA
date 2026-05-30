package com.soa.repositories;

import com.soa.models.TourPurchaseToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourPurchaseTokenRepository extends JpaRepository<TourPurchaseToken, Long> {
    List<TourPurchaseToken> findByTouristId(String touristId);
    Optional<TourPurchaseToken> findByTouristIdAndTourId(String touristId, Long tourId);
    boolean existsByTouristIdAndTourId(String touristId, Long tourId);
}
