<template>
  <div class="variety-detail-page">
    <div class="page-header">
      <div class="header-left">
        <el-button :icon="ArrowLeft" text @click="goBack">返回</el-button>
        <h2 class="page-title">
          <span>{{ varietyIcon }}</span>
          {{ reviewData?.name || '品种详情' }}
        </h2>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <template v-else-if="reviewData">
      <div class="card basic-info-card">
        <div class="basic-header">
          <div>
            <h3 class="variety-name">
              {{ reviewData.name }}
              <span v-if="reviewData.alias" class="variety-alias">（{{ reviewData.alias }}）</span>
            </h3>
            <div class="variety-tags">
              <el-tag type="success" effect="light">{{ reviewData.category || '未分类' }}</el-tag>
              <span v-if="reviewData.germinationDays" class="tag-item">
                🌱 发芽约 {{ reviewData.germinationDays }} 天
              </span>
              <span v-if="reviewData.seedlingDays" class="tag-item">
                🌿 育苗约 {{ reviewData.seedlingDays }} 天
              </span>
            </div>
          </div>
        </div>
        <p v-if="reviewData.description" class="variety-desc">{{ reviewData.description }}</p>
      </div>

      <div class="review-stats">
        <div class="stat-card">
          <div class="stat-icon">🌾</div>
          <div class="stat-content">
            <div class="stat-value">{{ reviewData.totalSowingBatches || 0 }}</div>
            <div class="stat-label">累计播种批次</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🫘</div>
          <div class="stat-content">
            <div class="stat-value">{{ reviewData.totalSowingSeeds || 0 }}</div>
            <div class="stat-label">累计播种粒数</div>
          </div>
        </div>
        <div class="stat-card highlight">
          <div class="stat-icon">⏱️</div>
          <div class="stat-content">
            <div class="stat-value">
              {{ reviewData.averageGerminationDays != null ? reviewData.averageGerminationDays + ' 天' : '--' }}
            </div>
            <div class="stat-label">平均发芽天数</div>
          </div>
        </div>
        <div class="stat-card highlight">
          <div class="stat-icon">📈</div>
          <div class="stat-content">
            <div class="stat-value">
              {{ reviewData.seedlingRate != null ? reviewData.seedlingRate + '%' : '--' }}
            </div>
            <div class="stat-label">综合成苗率</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🪴</div>
          <div class="stat-content">
            <div class="stat-value">{{ reviewData.totalTransplants || 0 }}</div>
            <div class="stat-label">移栽次数</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🌻</div>
          <div class="stat-content">
            <div class="stat-value">{{ reviewData.totalTransplantQuantity || 0 }}</div>
            <div class="stat-label">累计移栽株数</div>
          </div>
        </div>
      </div>

      <div class="card">
        <h4 class="section-title">📋 历次播种复盘</h4>

        <div v-if="!reviewData.sowingBatches || reviewData.sowingBatches.length === 0" class="empty-state">
          <div class="empty-icon">🌱</div>
          <div class="empty-text">暂无播种记录，去播种页面开始第一次培育吧</div>
        </div>

        <div v-else class="batch-list">
          <div
            v-for="(batch, index) in reviewData.sowingBatches"
            :key="batch.sowingId"
            class="batch-card"
          >
            <div class="batch-header">
              <div class="batch-title">
                <span class="batch-index">#{{ reviewData.sowingBatches.length - index }}</span>
                <span class="batch-time">{{ formatDateTime(batch.sowingTime) }}</span>
              </div>
              <div class="batch-status">
                <el-tag
                  v-if="batch.hasTransplant"
                  type="success"
                  size="small"
                  effect="light"
                >
                  已移栽
                </el-tag>
                <el-tag v-else type="warning" size="small" effect="light">
                  培育中
                </el-tag>
              </div>
            </div>

            <div class="batch-metrics">
              <div class="batch-metric">
                <span class="metric-label">播种</span>
                <span class="metric-value">{{ batch.sowingQuantity }} 粒</span>
              </div>
              <div class="batch-metric" v-if="batch.actualGerminationDays != null">
                <span class="metric-label">实际发芽</span>
                <span class="metric-value success">{{ batch.actualGerminationDays }} 天</span>
              </div>
              <div class="batch-metric" v-else>
                <span class="metric-label">发芽</span>
                <span class="metric-value muted">等待记录</span>
              </div>
              <div class="batch-metric" v-if="batch.estimatedSurvival != null && batch.estimatedSurvival > 0">
                <span class="metric-label">存活估算</span>
                <span class="metric-value">{{ batch.estimatedSurvival }} 株</span>
              </div>
              <div class="batch-metric" v-if="batch.seedlingRate != null">
                <span class="metric-label">成苗率</span>
                <span
                  class="metric-value"
                  :class="getRateClass(batch.seedlingRate)"
                >
                  {{ batch.seedlingRate }}%
                </span>
              </div>
              <div class="batch-metric" v-if="batch.transplantQuantity">
                <span class="metric-label">移栽</span>
                <span class="metric-value primary">{{ batch.transplantQuantity }} 株</span>
              </div>
            </div>

            <div v-if="batch.soilRatio || batch.containerType" class="batch-env">
              <span v-if="batch.soilRatio" class="env-item">🪴 {{ batch.soilRatio }}</span>
              <span v-if="batch.containerType" class="env-item">📦 {{ batch.containerType }}</span>
            </div>

            <div v-if="batch.stageRecords && batch.stageRecords.length > 0" class="batch-stages">
              <span
                v-for="(stage, i) in batch.stageRecords"
                :key="i"
                class="stage-chip"
              >
                {{ stage }}
              </span>
            </div>

            <div class="batch-actions">
              <el-button
                type="primary"
                link
                size="small"
                @click="goToGrowth(batch.sowingId)"
              >
                查看生长跟踪 →
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getVarietyReview } from '@/api/variety'
import { formatDateTime } from '@/utils/date'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const reviewData = ref(null)

const categoryIcons = {
  '一年生': '🌻',
  '二年生': '🌷',
  '多年生': '🌹',
  '球根': '🌷',
  '宿根': '🌺',
  '多肉': '🪴',
  '观叶': '🍃',
  '藤本': '🌿',
  '水生': '💧',
  '草本': '🌱'
}

const varietyIcon = computed(() => {
  if (!reviewData.value) return '🌸'
  return categoryIcons[reviewData.value.category] || '🌸'
})

const loadData = async () => {
  const id = route.query.id || route.params.id
  if (!id) return
  loading.value = true
  try {
    const data = await getVarietyReview(id)
    reviewData.value = data
  } catch (e) {
    console.error('加载品种详情失败', e)
  } finally {
    loading.value = false
  }
}

const getRateClass = (rate) => {
  if (rate >= 80) return 'success'
  if (rate >= 50) return 'warning'
  return 'danger'
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/card-wall')
  }
}

const goToGrowth = (sowingId) => {
  router.push({ path: '/growth', query: { sowingId } })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.variety-detail-page {
  min-height: 100%;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: #909399;
  gap: 12px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid #ebeef5;
  border-top-color: #67c23a;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.basic-info-card {
  margin-bottom: 20px;
}

.variety-name {
  margin: 0 0 8px 0;
  font-size: 22px;
  color: #303133;
}

.variety-alias {
  font-size: 16px;
  color: #909399;
  font-weight: normal;
}

.variety-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tag-item {
  font-size: 13px;
  color: #606266;
}

.variety-desc {
  margin: 16px 0 0 0;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.review-stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid #ebeef5;
  transition: all 0.25s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  }

  &.highlight {
    background: linear-gradient(135deg, #f0f9eb, #ecf5ff);
    border-color: #c2e7b0;
  }
}

.stat-icon {
  font-size: 28px;
  flex-shrink: 0;
}

.stat-content {
  min-width: 0;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
  gap: 10px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.6;
}

.empty-text {
  font-size: 14px;
}

.batch-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.batch-card {
  background: #fafbfc;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px 18px;
  transition: all 0.25s;

  &:hover {
    border-color: #c2e7b0;
    background: #fcfefb;
  }
}

.batch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.batch-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.batch-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 600;
}

.batch-time {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.batch-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 10px;
}

.batch-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.metric-label {
  font-size: 11px;
  color: #909399;
}

.metric-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;

  &.success { color: #67c23a; }
  &.warning { color: #e6a23c; }
  &.danger { color: #f56c6c; }
  &.primary { color: #409eff; }
  &.muted { color: #c0c4cc; font-weight: 400; }
}

.batch-env {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.env-item {
  font-size: 12px;
  color: #606266;
  padding: 3px 8px;
  background: white;
  border-radius: 4px;
}

.batch-stages {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.stage-chip {
  font-size: 11px;
  padding: 2px 8px;
  background: #f0f9eb;
  color: #67c23a;
  border-radius: 10px;
}

.batch-actions {
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
  text-align: right;
}

@media (max-width: 768px) {
  .review-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .batch-metrics {
    gap: 12px;
  }
}
</style>
