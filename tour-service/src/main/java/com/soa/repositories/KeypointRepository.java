package com.soa.repositories;

import com.soa.models.Keypoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface KeypointRepository extends JpaRepository<Keypoint, Long> {
    List<Keypoint> findByTourIdOrderByOrderIndex(Long tourId);
    void deleteByTourId(Long tourId);
}
