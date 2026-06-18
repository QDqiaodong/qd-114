package com.flower.cultivation.service;

import com.flower.cultivation.dto.TransplantReadinessDTO;
import com.flower.cultivation.entity.FlowerVariety;
import com.flower.cultivation.entity.GrowthTracking;
import com.flower.cultivation.entity.SowingRecord;
import com.flower.cultivation.repository.FlowerVarietyRepository;
import com.flower.cultivation.repository.GrowthTrackingRepository;
import com.flower.cultivation.repository.SowingRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransplantReadinessService {

    private final SowingRecordRepository sowingRecordRepository;
    private final GrowthTrackingRepository growthTrackingRepository;
    private final FlowerVarietyRepository flowerVarietyRepository;

    private static final Set<String> HEALTHY_STATUS = Set.of("健康", "正常", "良好", "健壮", "无病虫害", "-", "/");
    private static final Set<String> GOOD_ROOT_STATUS = Set.of("发达", "良好", "茂密", "健壮", "正常", "根系发达", "须根茂密", "根系良好");

    private static final BigDecimal WEIGHT_PLANT_HEIGHT = new BigDecimal("0.25");
    private static final BigDecimal WEIGHT_LEAF_COUNT = new BigDecimal("0.25");
    private static final BigDecimal WEIGHT_ROOT_DEVELOPMENT = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_HEALTH_STATUS = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_SEEDLING_AGE = new BigDecimal("0.15");

    public TransplantReadinessDTO assessReadiness(Long sowingId) {
        SowingRecord sowing = sowingRecordRepository.findById(sowingId).orElse(null);
        if (sowing == null) {
            throw new RuntimeException("播种记录不存在");
        }

        FlowerVariety variety = flowerVarietyRepository.findById(sowing.getVarietyId()).orElse(null);
        List<GrowthTracking> trackings = growthTrackingRepository.findBySowingIdOrderByRecordTimeAsc(sowingId);

        TransplantReadinessDTO result = new TransplantReadinessDTO();
        result.setSowingId(sowingId);
        result.setVarietyName(sowing.getVarietyName());
        result.setSowingTime(sowing.getSowingTime());
        result.setSeedlingDays(variety != null ? variety.getSeedlingDays() : null);

        if (trackings.isEmpty()) {
            result.setOverallResult(TransplantReadinessDTO.ReadinessResult.NEED_MORE_DATA);
            result.setOverallScore(BigDecimal.ZERO);
            result.setCriteria(new ArrayList<>());
            return result;
        }

        GrowthTracking latest = trackings.stream()
                .max(Comparator.comparing(GrowthTracking::getRecordTime))
                .orElse(null);

        if (latest != null) {
            result.setCurrentPlantHeight(latest.getPlantHeight());
            result.setCurrentLeafCount(latest.getLeafCount());
            result.setCurrentRootDevelopment(latest.getRootDevelopment());
            result.setCurrentHealthStatus(latest.getHealthStatus());
            result.setLatestRecordTime(latest.getRecordTime());
        }

        long actualDays = Duration.between(sowing.getSowingTime(), LocalDateTime.now()).toDays();
        result.setActualSeedlingDays(actualDays);

        List<TransplantReadinessDTO.CriterionAssessment> criteria = new ArrayList<>();
        criteria.add(assessPlantHeight(latest, variety));
        criteria.add(assessLeafCount(latest, variety));
        criteria.add(assessRootDevelopment(latest));
        criteria.add(assessHealthStatus(latest));
        criteria.add(assessSeedlingAge(sowing, variety, actualDays));
        result.setCriteria(criteria);

        boolean hasMissingData = criteria.stream()
                .anyMatch(c -> c.getScore() == null);

        if (hasMissingData) {
            result.setOverallResult(TransplantReadinessDTO.ReadinessResult.NEED_MORE_DATA);
            result.setOverallScore(BigDecimal.ZERO);
            return result;
        }

        BigDecimal totalScore = calculateWeightedScore(criteria);
        result.setOverallScore(totalScore);

        if (totalScore.compareTo(new BigDecimal("85")) >= 0) {
            result.setOverallResult(TransplantReadinessDTO.ReadinessResult.READY);
        } else if (totalScore.compareTo(new BigDecimal("70")) >= 0) {
            result.setOverallResult(TransplantReadinessDTO.ReadinessResult.ALMOST_READY);
        } else {
            result.setOverallResult(TransplantReadinessDTO.ReadinessResult.NOT_READY);
        }

        return result;
    }

    private TransplantReadinessDTO.CriterionAssessment assessPlantHeight(GrowthTracking latest, FlowerVariety variety) {
        TransplantReadinessDTO.CriterionAssessment criterion = new TransplantReadinessDTO.CriterionAssessment();
        criterion.setCriterionName("株高");
        criterion.setCriterionCode("PLANT_HEIGHT");
        criterion.setWeight(WEIGHT_PLANT_HEIGHT);

        if (latest == null || latest.getPlantHeight() == null) {
            criterion.setScore(null);
            criterion.setActualValue("未记录");
            criterion.setThreshold("≥5cm");
            criterion.setAssessment("缺少株高数据");
            criterion.setSuggestion("请在生长记录中填写株高信息");
            criterion.setPassed(false);
            return criterion;
        }

        BigDecimal height = latest.getPlantHeight();
        criterion.setActualValue(height + " cm");
        criterion.setThreshold("≥5cm（理想：8-15cm）");

        BigDecimal score;
        String assessment;
        String suggestion;
        boolean passed;

        if (height.compareTo(new BigDecimal("15")) > 0) {
            score = new BigDecimal("70");
            assessment = "株高过高，可能存在徒长";
            suggestion = "建议检查光照是否充足，避免氮肥过量";
            passed = false;
        } else if (height.compareTo(new BigDecimal("8")) >= 0) {
            score = new BigDecimal("100");
            assessment = "株高理想，适合移栽";
            suggestion = "株高条件满足，可以准备移栽";
            passed = true;
        } else if (height.compareTo(new BigDecimal("5")) >= 0) {
            score = new BigDecimal("80");
            assessment = "株高基本达标";
            suggestion = "可再生长2-3天，待株高达到8cm后移栽更佳";
            passed = true;
        } else if (height.compareTo(new BigDecimal("3")) >= 0) {
            score = new BigDecimal("50");
            assessment = "株高偏矮";
            suggestion = "建议继续培育，待株高达到5cm以上再考虑移栽";
            passed = false;
        } else {
            score = new BigDecimal("20");
            assessment = "株高过矮，幼苗太小";
            suggestion = "需要继续培育，加强水肥管理";
            passed = false;
        }

        criterion.setScore(score);
        criterion.setAssessment(assessment);
        criterion.setSuggestion(suggestion);
        criterion.setPassed(passed);
        return criterion;
    }

    private TransplantReadinessDTO.CriterionAssessment assessLeafCount(GrowthTracking latest, FlowerVariety variety) {
        TransplantReadinessDTO.CriterionAssessment criterion = new TransplantReadinessDTO.CriterionAssessment();
        criterion.setCriterionName("叶片数");
        criterion.setCriterionCode("LEAF_COUNT");
        criterion.setWeight(WEIGHT_LEAF_COUNT);

        if (latest == null || latest.getLeafCount() == null) {
            criterion.setScore(null);
            criterion.setActualValue("未记录");
            criterion.setThreshold("≥4片真叶");
            criterion.setAssessment("缺少叶片数数据");
            criterion.setSuggestion("请在生长记录中填写叶片数信息");
            criterion.setPassed(false);
            return criterion;
        }

        int leafCount = latest.getLeafCount();
        criterion.setActualValue(leafCount + " 片");
        criterion.setThreshold("≥4片真叶（理想：6-8片）");

        BigDecimal score;
        String assessment;
        String suggestion;
        boolean passed;

        if (leafCount >= 6) {
            score = new BigDecimal("100");
            assessment = "叶片充足，光合作用能力强";
            suggestion = "叶片条件完美，适合移栽";
            passed = true;
        } else if (leafCount >= 4) {
            score = new BigDecimal("85");
            assessment = "叶片数达标";
            suggestion = "叶片条件满足，可以移栽";
            passed = true;
        } else if (leafCount >= 2) {
            score = new BigDecimal("50");
            assessment = "叶片数偏少";
            suggestion = "建议再生长1-2周，待长出4片真叶后移栽";
            passed = false;
        } else {
            score = new BigDecimal("20");
            assessment = "叶片数太少，幼苗尚幼";
            suggestion = "需要继续培育，等待真叶长出";
            passed = false;
        }

        criterion.setScore(score);
        criterion.setAssessment(assessment);
        criterion.setSuggestion(suggestion);
        criterion.setPassed(passed);
        return criterion;
    }

    private TransplantReadinessDTO.CriterionAssessment assessRootDevelopment(GrowthTracking latest) {
        TransplantReadinessDTO.CriterionAssessment criterion = new TransplantReadinessDTO.CriterionAssessment();
        criterion.setCriterionName("根系发育");
        criterion.setCriterionCode("ROOT_DEVELOPMENT");
        criterion.setWeight(WEIGHT_ROOT_DEVELOPMENT);

        if (latest == null || latest.getRootDevelopment() == null || latest.getRootDevelopment().trim().isEmpty()) {
            criterion.setScore(null);
            criterion.setActualValue("未记录");
            criterion.setThreshold("根系发达、须根茂密");
            criterion.setAssessment("缺少根系发育数据");
            criterion.setSuggestion("请在生长记录中填写根系发育情况");
            criterion.setPassed(false);
            return criterion;
        }

        String rootDev = latest.getRootDevelopment().trim();
        criterion.setActualValue(rootDev);
        criterion.setThreshold("根系发达、须根茂密");

        BigDecimal score;
        String assessment;
        String suggestion;
        boolean passed;

        String lowerRootDev = rootDev.toLowerCase();
        boolean isGood = GOOD_ROOT_STATUS.stream()
                .anyMatch(s -> lowerRootDev.contains(s.toLowerCase()));
        boolean isBad = lowerRootDev.contains("差") || lowerRootDev.contains("弱")
                || lowerRootDev.contains("不发达") || lowerRootDev.contains("稀少")
                || lowerRootDev.contains("烂") || lowerRootDev.contains("病");

        if (isGood) {
            score = new BigDecimal("100");
            assessment = "根系发育良好，移栽后易成活";
            suggestion = "根系条件完美，非常适合移栽";
            passed = true;
        } else if (isBad) {
            score = new BigDecimal("30");
            assessment = "根系发育不良";
            suggestion = "建议改善浇水方式，避免积水，促进根系生长";
            passed = false;
        } else {
            score = new BigDecimal("70");
            assessment = "根系发育一般";
            suggestion = "可以移栽，但需特别注意缓苗期养护";
            passed = true;
        }

        criterion.setScore(score);
        criterion.setAssessment(assessment);
        criterion.setSuggestion(suggestion);
        criterion.setPassed(passed);
        return criterion;
    }

    private TransplantReadinessDTO.CriterionAssessment assessHealthStatus(GrowthTracking latest) {
        TransplantReadinessDTO.CriterionAssessment criterion = new TransplantReadinessDTO.CriterionAssessment();
        criterion.setCriterionName("健康状态");
        criterion.setCriterionCode("HEALTH_STATUS");
        criterion.setWeight(WEIGHT_HEALTH_STATUS);

        if (latest == null || latest.getHealthStatus() == null || latest.getHealthStatus().trim().isEmpty()) {
            criterion.setScore(null);
            criterion.setActualValue("未记录");
            criterion.setThreshold("健康、无病虫害");
            criterion.setAssessment("缺少健康状态数据");
            criterion.setSuggestion("请在生长记录中填写健康状态");
            criterion.setPassed(false);
            return criterion;
        }

        String health = latest.getHealthStatus().trim();
        criterion.setActualValue(health);
        criterion.setThreshold("健康、无病虫害");

        BigDecimal score;
        String assessment;
        String suggestion;
        boolean passed;

        String lowerHealth = health.toLowerCase();
        boolean isHealthy = HEALTHY_STATUS.stream()
                .anyMatch(s -> lowerHealth.contains(s.toLowerCase()));
        boolean hasProblem = lowerHealth.contains("病") || lowerHealth.contains("虫")
                || lowerHealth.contains("黄") || lowerHealth.contains("枯")
                || lowerHealth.contains("烂") || lowerHealth.contains("徒长")
                || lowerHealth.contains("萎") || lowerHealth.contains("斑点");

        if (isHealthy && !hasProblem) {
            score = new BigDecimal("100");
            assessment = "植株健康，无病虫害";
            suggestion = "健康状态良好，适合移栽";
            passed = true;
        } else if (hasProblem) {
            score = new BigDecimal("20");
            assessment = "植株存在健康问题";
            suggestion = "建议先治理病虫害，待植株恢复健康后再移栽";
            passed = false;
        } else {
            score = new BigDecimal("60");
            assessment = "健康状态一般";
            suggestion = "可以移栽，但需加强移栽后的养护管理";
            passed = true;
        }

        criterion.setScore(score);
        criterion.setAssessment(assessment);
        criterion.setSuggestion(suggestion);
        criterion.setPassed(passed);
        return criterion;
    }

    private TransplantReadinessDTO.CriterionAssessment assessSeedlingAge(SowingRecord sowing, FlowerVariety variety, long actualDays) {
        TransplantReadinessDTO.CriterionAssessment criterion = new TransplantReadinessDTO.CriterionAssessment();
        criterion.setCriterionName("苗龄");
        criterion.setCriterionCode("SEEDLING_AGE");
        criterion.setWeight(WEIGHT_SEEDLING_AGE);

        Integer expectedDays = variety != null ? variety.getSeedlingDays() : null;
        criterion.setActualValue(actualDays + " 天");
        criterion.setThreshold(expectedDays != null ?
                "约 " + expectedDays + " 天（±10天）" : "30-60 天（建议参考品种特性）");

        if (expectedDays == null) {
            if (actualDays >= 45 && actualDays <= 60) {
                criterion.setScore(new BigDecimal("100"));
                criterion.setAssessment("苗龄适中");
                criterion.setSuggestion("苗龄条件满足，适合移栽");
                criterion.setPassed(true);
            } else if (actualDays >= 30 && actualDays < 45) {
                criterion.setScore(new BigDecimal("75"));
                criterion.setAssessment("苗龄略短");
                criterion.setSuggestion("建议再培育几天，待苗龄达到45天左右更佳");
                criterion.setPassed(true);
            } else if (actualDays > 60 && actualDays <= 90) {
                criterion.setScore(new BigDecimal("70"));
                criterion.setAssessment("苗龄略长");
                criterion.setSuggestion("可以移栽，但建议尽快进行，避免根系盘结");
                criterion.setPassed(true);
            } else if (actualDays < 30) {
                criterion.setScore(new BigDecimal("30"));
                criterion.setAssessment("苗龄太短，幼苗尚幼");
                criterion.setSuggestion("建议继续培育至少15天以上");
                criterion.setPassed(false);
            } else {
                criterion.setScore(new BigDecimal("50"));
                criterion.setAssessment("苗龄过长，可能根系受限");
                criterion.setSuggestion("建议尽快移栽，注意检查根系是否盘结");
                criterion.setPassed(false);
            }
        } else {
            BigDecimal diff = new BigDecimal(actualDays).subtract(new BigDecimal(expectedDays));
            BigDecimal absDiff = diff.abs();

            if (absDiff.compareTo(new BigDecimal("5")) <= 0) {
                criterion.setScore(new BigDecimal("100"));
                criterion.setAssessment("苗龄与品种预期完全吻合");
                criterion.setSuggestion("苗龄条件完美，非常适合移栽");
                criterion.setPassed(true);
            } else if (absDiff.compareTo(new BigDecimal("10")) <= 0) {
                criterion.setScore(new BigDecimal("90"));
                criterion.setAssessment("苗龄基本符合预期");
                criterion.setSuggestion("苗龄条件满足，可以移栽");
                criterion.setPassed(true);
            } else if (diff.compareTo(new BigDecimal("-10")) > 0 && diff.compareTo(BigDecimal.ZERO) < 0) {
                criterion.setScore(new BigDecimal("70"));
                criterion.setAssessment("苗龄略短于预期");
                criterion.setSuggestion("建议再培育 " + (-diff.intValue()) + " 天左右更佳");
                criterion.setPassed(true);
            } else if (diff.compareTo(new BigDecimal("10")) > 0 && diff.compareTo(new BigDecimal("20")) <= 0) {
                criterion.setScore(new BigDecimal("65"));
                criterion.setAssessment("苗龄略长于预期");
                criterion.setSuggestion("可以移栽，建议尽快进行，避免根系盘结");
                criterion.setPassed(true);
            } else if (diff.compareTo(new BigDecimal("-20")) >= 0) {
                criterion.setScore(new BigDecimal("40"));
                criterion.setAssessment("苗龄太短，幼苗尚幼");
                criterion.setSuggestion("建议继续培育，待接近预期苗龄后再移栽");
                criterion.setPassed(false);
            } else {
                criterion.setScore(new BigDecimal("45"));
                criterion.setAssessment("苗龄过长，可能根系受限");
                criterion.setSuggestion("建议尽快移栽，注意检查根系是否盘结");
                criterion.setPassed(false);
            }
        }

        return criterion;
    }

    private BigDecimal calculateWeightedScore(List<TransplantReadinessDTO.CriterionAssessment> criteria) {
        BigDecimal total = BigDecimal.ZERO;
        for (TransplantReadinessDTO.CriterionAssessment c : criteria) {
            if (c.getScore() != null) {
                total = total.add(c.getScore().multiply(c.getWeight()));
            }
        }
        return total.setScale(1, RoundingMode.HALF_UP);
    }

    public List<TransplantReadinessDTO> assessBatchReadiness(List<Long> sowingIds) {
        return sowingIds.stream()
                .map(this::assessReadiness)
                .collect(Collectors.toList());
    }

    public List<TransplantReadinessDTO> assessAllReadiness() {
        List<SowingRecord> sowings = sowingRecordRepository.findAllByOrderBySowingTimeDesc();
        return sowings.stream()
                .map(s -> assessReadiness(s.getId()))
                .collect(Collectors.toList());
    }
}
