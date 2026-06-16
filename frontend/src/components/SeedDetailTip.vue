<template>
  <div v-if="seedDetail" class="seed-detail-tip">
    <div class="tip-header">
      <span class="tip-title">📋 种子详情</span>
      <el-tag :type="expireStatus.type" size="small" class="expire-tag">
        {{ expireStatus.text }}
      </el-tag>
    </div>

    <div class="info-grid">
      <div class="info-item">
        <span class="info-label">剩余数量</span>
        <span class="info-value" :class="{ 'low-stock': seedDetail.remainingQuantity < 10 }">
          {{ seedDetail.remainingQuantity }} 粒
          <span class="info-sub">/ 初始 {{ seedDetail.initialQuantity }} 粒</span>
        </span>
      </div>

      <div class="info-item">
        <span class="info-label">来源</span>
        <span class="info-value">
          <el-tag :type="seedDetail.sourceType === 'PURCHASE' ? 'primary' : 'success'" size="small">
            {{ seedDetail.sourceTypeName }}
          </el-tag>
          <span v-if="seedDetail.supplier" class="info-sub">{{ seedDetail.supplier }}</span>
        </span>
      </div>

      <div class="info-item">
        <span class="info-label">保质期</span>
        <span class="info-value">
          <template v-if="seedDetail.expireDate">
            {{ seedDetail.expireDate }}
            <span class="info-sub">({{ seedDetail.shelfLife }}个月)</span>
          </template>
          <template v-else class="info-sub">未设置</template>
        </span>
      </div>

      <div class="info-item">
        <span class="info-label">品种发芽天数</span>
        <span class="info-value">
          <template v-if="seedDetail.germinationDays">
            约 {{ seedDetail.germinationDays }} 天
          </template>
          <template v-else class="info-sub">未设置</template>
        </span>
      </div>
    </div>

    <div v-if="seedDetail.germinationHistory" class="history-section">
      <div class="history-header">
        <span class="history-title">📊 历史发芽表现</span>
        <span class="history-stat">
          共 {{ seedDetail.germinationHistory.totalSowings }} 次记录
          <template v-if="seedDetail.germinationHistory.averageGerminationRate != null">
            · 平均发芽率 <strong :class="getRateClass(seedDetail.germinationHistory.averageGerminationRate)">
              {{ seedDetail.germinationHistory.averageGerminationRate }}%
            </strong>
          </template>
        </span>
      </div>
      <div v-if="seedDetail.germinationHistory.recentRecords?.length" class="history-list">
        <div
          v-for="record in seedDetail.germinationHistory.recentRecords"
          :key="record.sowingId"
          class="history-item"
        >
          <span class="history-date">{{ record.sowingDate }}</span>
          <span class="history-qty">播种 {{ record.sowingQuantity }} 粒</span>
          <span class="history-survival">发芽 {{ record.estimatedSurvival }} 株</span>
          <span class="history-rate" :class="getRateClass(record.germinationRate)">
            {{ record.germinationRate }}%
          </span>
        </div>
      </div>
      <div v-else class="history-empty">暂无历史发芽记录</div>
    </div>

    <div v-if="suggestion" class="suggestion-box">
      <span class="suggestion-icon">💡</span>
      <span class="suggestion-text">{{ suggestion }}</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getSeedDetail } from '@/api/seed'

const props = defineProps({
  seedId: {
    type: [Number, String],
    default: null
  }
})

const seedDetail = ref(null)
const loading = ref(false)

const expireStatus = computed(() => {
  if (!seedDetail.value?.expireDate) {
    return { type: 'info', text: '未设置' }
  }
  const today = new Date()
  const expireDate = new Date(seedDetail.value.expireDate)
  const daysLeft = Math.ceil((expireDate - today) / (1000 * 60 * 60 * 24))

  if (daysLeft < 0) {
    return { type: 'danger', text: '已过期' }
  } else if (daysLeft <= 30) {
    return { type: 'warning', text: `临期(${daysLeft}天)` }
  } else {
    return { type: 'success', text: '正常' }
  }
})

const suggestion = computed(() => {
  if (!seedDetail.value) return ''
  const tips = []

  if (seedDetail.value.remainingQuantity < 10) {
    tips.push('库存不足，建议谨慎控制播种数量')
  }

  if (expireStatus.value.type === 'danger') {
    tips.push('种子已过期，发芽率可能大幅下降，建议谨慎播种')
  } else if (expireStatus.value.type === 'warning') {
    tips.push('种子即将过期，建议优先使用')
  }

  if (seedDetail.value.germinationHistory?.averageGerminationRate != null) {
    const rate = seedDetail.value.germinationHistory.averageGerminationRate
    if (rate < 50) {
      tips.push('历史发芽率偏低，建议适当增加播种量')
    } else if (rate >= 80) {
      tips.push('历史发芽率良好，可按计划数量播种')
    }
  }

  return tips.join('；')
})

const getRateClass = (rate) => {
  if (rate == null) return ''
  if (rate >= 80) return 'rate-high'
  if (rate >= 50) return 'rate-medium'
  return 'rate-low'
}

const loadSeedDetail = async () => {
  if (!props.seedId) {
    seedDetail.value = null
    return
  }
  loading.value = true
  try {
    const data = await getSeedDetail(props.seedId)
    seedDetail.value = data || null
  } catch (e) {
    console.error(e)
    seedDetail.value = null
  } finally {
    loading.value = false
  }
}

watch(() => props.seedId, () => {
  loadSeedDetail()
}, { immediate: true })
</script>

<style scoped>
.seed-detail-tip {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.tip-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.tip-title {
  font-weight: 600;
  color: #334155;
  font-size: 14px;
}

.expire-tag {
  margin-left: 8px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #64748b;
}

.info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

.info-value.low-stock {
  color: #ef4444;
  font-weight: 600;
}

.info-sub {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
  margin-left: 4px;
}

.history-section {
  border-top: 1px solid #e2e8f0;
  padding-top: 12px;
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.history-title {
  font-weight: 600;
  color: #334155;
  font-size: 14px;
}

.history-stat {
  font-size: 12px;
  color: #64748b;
}

.history-stat strong {
  font-weight: 600;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  padding: 6px 10px;
  background: white;
  border-radius: 4px;
}

.history-date {
  color: #64748b;
  flex-shrink: 0;
}

.history-qty {
  color: #475569;
}

.history-survival {
  color: #475569;
}

.history-rate {
  margin-left: auto;
  font-weight: 600;
}

.history-empty {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 12px;
  background: white;
  border-radius: 4px;
}

.rate-high {
  color: #22c55e;
}

.rate-medium {
  color: #eab308;
}

.rate-low {
  color: #ef4444;
}

.suggestion-box {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 12px;
  padding: 10px 12px;
  background: #fef9c3;
  border: 1px solid #fde047;
  border-radius: 6px;
}

.suggestion-icon {
  flex-shrink: 0;
}

.suggestion-text {
  font-size: 12px;
  color: #854d0e;
  line-height: 1.5;
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .history-item {
    flex-wrap: wrap;
  }

  .history-rate {
    margin-left: 0;
  }
}
</style>
