package com.soa.repositories;

import com.soa.models.TransportTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransportTimeRepository extends JpaRepository<TransportTime, Long> {
    List<TransportTime> findByTourId(Long tourId);
    long countByTourId(Long tourId);
}

