package com.soa.repositories;

import com.soa.models.Tour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TourRepository extends JpaRepository<Tour, Long> {
    List<Tour> findByAuthorId(String authorId);
    List<Tour> findByStatus(Tour.TourStatus status);
}
