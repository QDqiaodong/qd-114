<template>
  <div class="card-wall-page">
    <div class="page-header">
      <h2 class="page-title">🏷️ 品种培育卡片墙</h2>
      <div class="header-stats">
        <span class="stat-chip primary">
          🌱 品种 {{ totalVarieties }}
        </span>
        <span class="stat-chip success">
          🌾 活跃批次 {{ totalActiveBatches }}
        </span>
        <span class="stat-chip warning">
          🫘 种子库存 {{ totalSeedQuantity }}
        </span>
      </div>
    </div>

    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <template v-else>
      <div v-if="Object.keys(cardWallData).length === 0" class="empty-state">
        <div class="empty-icon">🌺</div>
        <div class="empty-text">暂无品种数据，请先添加花卉品种</div>
      </div>

      <div v-else class="category-sections">
        <div
          v-for="(varieties, category) in cardWallData"
          :key="category"
          class="category-section"
        >
          <div class="category-header">
            <span class="category-icon">{{ getCategoryIcon(category) }}</span>
            <span class="category-name">{{ category }}</span>
            <span class="category-count">{{ varieties.length }} 个品种</span>
          </div>
          <div class="variety-cards">
            <div
              v-for="v in varieties"
              :key="v.varietyId"
              class="variety-card"
              :class="{ 'has-active': v.activeSowingBatches > 0 }"
              @click="goToDetail(v.varietyId)"
            >
              <div class="card-top">
                <div class="card-name-row">
                  <span class="card-name">{{ v.name }}</span>
                  <span v-if="v.activeSowingBatches > 0" class="active-badge">培育中</span>
                </div>
                <div class="card-meta-row">
                  <el-tag v-if="v.category" size="small" type="info" effect="plain" class="category-tag">{{ v.category }}</el-tag>
                  <span v-if="v.alias" class="card-alias">{{ v.alias }}</span>
                </div>
              </div>

              <div class="card-metrics">
                <div class="metric-item">
                  <span class="metric-icon">🌱</span>
                  <div class="metric-content">
                    <span class="metric-label">发芽天数</span>
                    <span class="metric-value" :class="{ 'no-data': v.germinationDays == null }">
                      {{ v.germinationDays != null ? v.germinationDays + ' 天' : '--' }}
                    </span>
                  </div>
                </div>
                <div class="metric-item">
                  <span class="metric-icon">🌿</span>
                  <div class="metric-content">
                    <span class="metric-label">育苗天数</span>
                    <span class="metric-value" :class="{ 'no-data': v.seedlingDays == null }">
                      {{ v.seedlingDays != null ? v.seedlingDays + ' 天' : '--' }}
                    </span>
                  </div>
                </div>
                <div class="metric-item">
                  <span class="metric-icon">🫘</span>
                  <div class="metric-content">
                    <span class="metric-label">种子库存</span>
                    <span class="metric-value" :class="{ 'low-stock': v.seedQuantity > 0 && v.seedQuantity < 10, 'no-stock': v.seedQuantity === 0 }">
                      {{ v.seedQuantity }} 粒
                    </span>
                  </div>
                </div>
                <div class="metric-item">
                  <span class="metric-icon">🌾</span>
                  <div class="metric-content">
                    <span class="metric-label">活跃批次</span>
                    <span class="metric-value" :class="{ 'has-batch': v.activeSowingBatches > 0, 'no-batch': v.activeSowingBatches === 0 }">
                      {{ v.activeSowingBatches }} 批
                    </span>
                  </div>
                </div>
              </div>

              <div class="card-footer-bar">
                <div class="footer-indicator seed-indicator" :style="{ width: seedBarWidth(v) + '%' }"></div>
                <div class="footer-indicator batch-indicator" :style="{ width: batchBarWidth(v) + '%' }"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getVarietyCardWall } from '@/api/variety'

const router = useRouter()

const loading = ref(false)
const cardWallData = ref({})

const totalVarieties = computed(() => {
  return Object.values(cardWallData.value).reduce((sum, list) => sum + list.length, 0)
})

const totalActiveBatches = computed(() => {
  return Object.values(cardWallData.value)
    .flat()
    .reduce((sum, v) => sum + (v.activeSowingBatches || 0), 0)
})

const totalSeedQuantity = computed(() => {
  return Object.values(cardWallData.value)
    .flat()
    .reduce((sum, v) => sum + (v.seedQuantity || 0), 0)
})

const maxSeedInCategory = (varieties) => {
  return Math.max(...varieties.map(v => v.seedQuantity || 0), 1)
}

const seedBarWidth = (v) => {
  const categoryList = cardWallData.value[v.category || '未分类'] || []
  const max = maxSeedInCategory(categoryList)
  return Math.min(((v.seedQuantity || 0) / max) * 100, 100)
}

const batchBarWidth = (v) => {
  const categoryList = cardWallData.value[v.category || '未分类'] || []
  const maxBatches = Math.max(...categoryList.map(c => c.activeSowingBatches || 0), 1)
  return Math.min(((v.activeSowingBatches || 0) / maxBatches) * 100, 100)
}

const categoryIcons = {
  '一年生': '🌻',
  '二年生': '🌷',
  '多年生': '🌹',
  '球根': '🌷',
  '宿根': '🌺',
  '多肉': '🪴',
  '观叶': '🍃',
  '藤本': '🌿',
  '水生': '💧'
}

const getCategoryIcon = (category) => {
  return categoryIcons[category] || '🌸'
}

const goToDetail = (varietyId) => {
  router.push({ path: '/variety-detail', query: { id: varietyId } })
}

const loadData = async () => {
  loading.value = true
  try {
    const data = await getVarietyCardWall()
    cardWallData.value = data || {}
  } catch (e) {
    console.error('加载卡片墙数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.card-wall-page {
  min-height: 100%;
}

.header-stats {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.stat-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.stat-chip.primary {
  background: #ecf5ff;
  color: #409eff;
}

.stat-chip.success {
  background: #f0f9eb;
  color: #67c23a;
}

.stat-chip.warning {
  background: #fdf6ec;
  color: #e6a23c;
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

.category-sections {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.category-section {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px 24px;
  background: linear-gradient(135deg, #f0f9eb, #ecf5ff);
  border-bottom: 1px solid #ebeef5;
}

.category-icon {
  font-size: 22px;
}

.category-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.category-count {
  font-size: 12px;
  color: #909399;
  background: white;
  padding: 2px 10px;
  border-radius: 10px;
  margin-left: 4px;
}

.variety-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 16px;
  padding: 20px;
}

.variety-card {
  background: #fafbfc;
  border-radius: 10px;
  padding: 18px;
  border: 1px solid #ebeef5;
  transition: all 0.25s;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.variety-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
  border-color: #c2e7b0;
}

.variety-card.has-active {
  border-left: 3px solid #67c23a;
}

.card-top {
  margin-bottom: 14px;
}

.card-name-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.card-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.active-badge {
  flex-shrink: 0;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: #f0f9eb;
  color: #67c23a;
  font-weight: 500;
  animation: breathe 2s ease-in-out infinite;
}

@keyframes breathe {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.card-meta-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.category-tag {
  flex-shrink: 0;
}

.card-alias {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: white;
  border-radius: 6px;
}

.metric-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.metric-content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.metric-label {
  font-size: 11px;
  color: #c0c4cc;
  line-height: 1;
}

.metric-value {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-top: 3px;
  line-height: 1.2;
}

.metric-value.no-data {
  color: #dcdfe6;
}

.metric-value.low-stock {
  color: #e6a23c;
}

.metric-value.no-stock {
  color: #f56c6c;
}

.metric-value.has-batch {
  color: #67c23a;
}

.metric-value.no-batch {
  color: #c0c4cc;
}

.card-footer-bar {
  display: flex;
  gap: 4px;
  margin-top: 14px;
  height: 4px;
  background: #f0f2f5;
  border-radius: 2px;
  overflow: hidden;
}

.footer-indicator {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.seed-indicator {
  background: linear-gradient(90deg, #e6a23c, #f5c96a);
}

.batch-indicator {
  background: linear-gradient(90deg, #67c23a, #95d475);
}

@media (max-width: 768px) {
  .variety-cards {
    grid-template-columns: 1fr;
  }

  .header-stats {
    flex-direction: column;
  }

  .stat-chip {
    width: fit-content;
  }
}
</style>
