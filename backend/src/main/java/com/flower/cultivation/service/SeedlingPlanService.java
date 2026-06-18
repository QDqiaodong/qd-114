package com.flower.cultivation.service;

import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.SeedInfo;
import com.flower.cultivation.entity.SeedlingPlan;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.SeedInfoRepository;
import com.flower.cultivation.repository.SeedlingPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeedlingPlanService {

    private final SeedlingPlanRepository seedlingPlanRepository;
    private final SeedInfoRepository seedInfoRepository;
    private final FlowerVarietyRepository flowerVarietyRepository;

    public List<SeedlingPlan> findAll() {
        return seedlingPlanRepository.findAllByOrderByPlannedSowDateAsc();
    }

    public SeedlingPlan findById(Long id) {
        return seedlingPlanRepository.findById(id).orElse(null);
    }

    public List<SeedlingPlan> findBySeedId(Long seedId) {
        return seedlingPlanRepository.findBySeedId(seedId);
    }

    public List<SeedlingPlan> findByVarietyId(Long varietyId) {
        return seedlingPlanRepository.findByVarietyId(varietyId);
    }

    public List<SeedlingPlan> findByStatus(String status) {
        return seedlingPlanRepository.findByStatus(status);
    }

    @Transactional
    public SeedlingPlan save(SeedlingPlan plan) {
        SeedInfo seed = seedInfoRepository.findById(plan.getSeedId()).orElse(null);
        if (seed == null) {
            throw new RuntimeException("种子不存在");
        }

        plan.setVarietyId(seed.getVarietyId());
        plan.setVarietyName(seed.getVarietyName());

        FlowerVariety variety = flowerVarietyRepository.findById(seed.getVarietyId()).orElse(null);
        if (variety != null) {
            plan.setGerminationDays(variety.getGerminationDays());
            plan.setSeedlingDays(variety.getSeedlingDays());
        }

        plan.setShelfLife(seed.getShelfLife());

        if (seed.getAcquireTime() != null && seed.getShelfLife() != null) {
            plan.setExpireDate(seed.getAcquireTime().plusMonths(seed.getShelfLife()));
        }

        if (plan.getPlannedSowDate() != null && variety != null && variety.getGerminationDays() != null) {
            plan.setPlannedTransplantDate(plan.getPlannedSowDate().plusDays(variety.getGerminationDays() + (variety.getSeedlingDays() != null ? variety.getSeedlingDays() : 0)));
        }

        if (plan.getStatus() == null) {
            plan.setStatus("PENDING");
        }

        return seedlingPlanRepository.save(plan);
    }

    @Transactional
    public SeedlingPlan updateStatus(Long id, String status, LocalDate actualDate) {
        SeedlingPlan plan = findById(id);
        if (plan == null) {
            throw new RuntimeException("育苗计划不存在");
        }

        plan.setStatus(status);

        if ("SOWING".equals(status) && actualDate != null) {
            plan.setActualSowDate(actualDate);
        }
        if ("TRANSPLANTED".equals(status) && actualDate != null) {
            plan.setActualTransplantDate(actualDate);
        }
        if ("DONE".equals(status)) {
            if (plan.getActualSowDate() == null) {
                plan.setActualSowDate(LocalDate.now());
            }
            if (plan.getActualTransplantDate() == null) {
                plan.setActualTransplantDate(LocalDate.now());
            }
        }

        return seedlingPlanRepository.save(plan);
    }

    @Transactional
    public void deleteById(Long id) {
        seedlingPlanRepository.deleteById(id);
    }
}
