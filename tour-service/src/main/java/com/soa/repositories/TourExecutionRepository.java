package com.soa.repositories;

import com.soa.models.TourExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TourExecutionRepository extends JpaRepository<TourExecution, Long> {

    List<TourExecution> findByTouristId(String touristId);

    Optional<TourExecution> findByIdAndTouristId(Long id, String touristId);

    boolean existsByTouristIdAndTourIdAndStatus(String touristId, Long tourId,
                                                TourExecution.ExecutionStatus status);

    Optional<TourExecution> findByTouristIdAndTourIdAndStatus(String touristId, Long tourId,
                                                               TourExecution.ExecutionStatus status);
}