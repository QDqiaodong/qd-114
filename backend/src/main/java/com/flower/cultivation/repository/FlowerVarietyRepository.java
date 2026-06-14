package com.flower.cultivation.repository;

import com.flower.cultivation.entity.FlowerVariety;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowerVarietyRepository extends JpaRepository<FlowerVariety, Long> {
    List<FlowerVariety> findByCategory(String category);
    List<FlowerVariety> findByNameContaining(String name);
}
