package com.flower.cultivation.repository;

import com.flower.cultivation.entity.SeedGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeedGroupRepository extends JpaRepository<SeedGroup, Long> {

    List<SeedGroup> findAllByOrderBySortOrderAsc();

    Optional<SeedGroup> findByGroupCode(String groupCode);

    List<SeedGroup> findByCategoryOrderBySortOrderAsc(String category);

    boolean existsByGroupCode(String groupCode);
}
