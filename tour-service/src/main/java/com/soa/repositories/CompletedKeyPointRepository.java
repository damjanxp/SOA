package com.soa.repositories;

import com.soa.models.CompletedKeyPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompletedKeyPointRepository extends JpaRepository<CompletedKeyPoint, Long> {

    List<CompletedKeyPoint> findByTourExecutionId(Long executionId);

    boolean existsByTourExecutionIdAndKeyPointId(Long executionId, Long keyPointId);
}