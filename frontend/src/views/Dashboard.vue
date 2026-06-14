<template>
  <div class="dashboard">
    <div class="stat-grid">
      <div
        v-for="(item, key) in metricConfigs"
        :key="key"
        class="stat-card"
        :class="[item.themeClass, { active: activeMetric === key }]"
        @click="toggleMetric(key)"
      >
        <div class="stat-icon">{{ item.icon }}</div>
        <div class="stat-value">{{ formatNumber(getMetricValue(key)) }}</div>
        <div class="stat-label">{{ item.label }}</div>
        <div class="stat-hint" v-if="item.hint">{{ item.hint }}</div>
        <div class="stat-variety-tags" v-if="getTopVarieties(key).length > 0">
          <span class="mini-tag" v-for="v in getTopVarieties(key)" :key="v.varietyName">
            {{ v.varietyName }}
          </span>
        </div>
      </div>
    </div>

    <div class="metric-detail card" v-if="activeMetric && activeMetricData">
      <div class="card-header">
        <h3 class="card-title">
          {{ metricConfigs[activeMetric].icon }} {{ metricConfigs[activeMetric].label }} — 品种分布
        </h3>
        <span class="close-btn" @click="activeMetric = null">×</span>
      </div>

      <div class="detail-container">
        <div class="variety-section">
          <h4 class="section-title">📋 关联品种</h4>
          <div class="variety-grid">
            <div
              v-for="v in activeMetricData.varieties"
              :key="v.varietyName"
              class="variety-card"
              :class="{ active: activeVariety === v.varietyName }"
              @click="activeVariety = activeVariety === v.varietyName ? null : v.varietyName"
            >
              <div class="variety-name">{{ v.varietyName }}</div>
              <div class="variety-metrics">
                <span class="variety-metric" v-if="v.totalQty != null">
                  {{ v.totalQty }} 粒
                </span>
                <span class="variety-metric" v-if="v.seedCount != null">
                  {{ v.seedCount }} 条
                </span>
                <span class="variety-metric" v-if="v.batchCount != null">
                  {{ v.batchCount }} 批
                </span>
                <span class="variety-metric" v-if="v.issueCount != null">
                  {{ v.issueCount }} 项
                </span>
                <span class="variety-metric danger" v-if="v.daysLeft != null || v.minDaysLeft != null">
                  ⏰ {{ v.daysLeft != null ? v.daysLeft : v.minDaysLeft }}天
                </span>
                <span class="variety-metric warning" v-if="v.maxDaysSince != null">
                  ⏳ {{ v.maxDaysSince }}天
                </span>
              </div>
              <div class="health-tags" v-if="v.healthStatuses && v.healthStatuses.length > 0">
                <span class="health-tag" v-for="(h, i) in v.healthStatuses" :key="i">
                  {{ h }}
                </span>
              </div>
            </div>
            <div v-if="!activeMetricData.varieties || activeMetricData.varieties.length === 0" class="empty-state small">
              <div class="empty-text">暂无关联品种</div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4 class="section-title">📝 详细记录</h4>
          <div class="detail-list">
            <div
              v-for="d in filteredDetails"
              :key="d.id"
              class="detail-item"
            >
              <div class="detail-main">
                <div class="detail-name">{{ getDetailName(d) }}</div>
                <div class="detail-sub" v-if="getDetailSub(d)">
                  {{ getDetailSub(d) }}
                </div>
              </div>
              <div class="detail-right">
                <div class="detail-meta" v-if="getDetailMeta(d)">
                  {{ getDetailMeta(d) }}
                </div>
                <div class="detail-badge" :class="activeMetric">
                  {{ getDetailBadge(d) }}
                </div>
              </div>
            </div>
            <div v-if="filteredDetails.length === 0" class="empty-state small">
              <div class="empty-text">暂无记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <div class="card recent-seeds">
        <div class="card-header">
          <h3 class="card-title">📦 最近种子入库</h3>
          <router-link to="/seeds" class="more-link">查看全部 →</router-link>
        </div>
        <div v-if="recentSeeds.length > 0" class="seed-list">
          <div v-for="seed in recentSeeds" :key="seed.id" class="seed-item">
            <div class="seed-name">{{ seed.varietyName }}</div>
            <div class="seed-meta">
              <span class="seed-source">{{ sourceTypeMap[seed.sourceType] || seed.sourceType }}</span>
              <span class="seed-qty">剩余 {{ seed.remainingQuantity }} 粒</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">🌱</div>
          <div class="empty-text">暂无种子记录</div>
        </div>
      </div>

      <div class="card recent-sowings">
        <div class="card-header">
          <h3 class="card-title">🌾 最近播种</h3>
          <router-link to="/sowings" class="more-link">查看全部 →</router-link>
        </div>
        <div v-if="recentSowings.length > 0" class="sowing-list">
          <div v-for="sowing in recentSowings" :key="sowing.id" class="sowing-item">
            <div class="sowing-name">{{ sowing.varietyName }}</div>
            <div class="sowing-meta">
              <span class="sowing-date">{{ formatDate(sowing.sowingTime) }}</span>
              <span class="sowing-qty">{{ sowing.sowingQuantity }} 粒</span>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <div class="empty-icon">🌾</div>
          <div class="empty-text">暂无播种记录</div>
        </div>
      </div>

      <div class="card stages-card">
        <div class="card-header">
          <h3 class="card-title">📋 标准培育流程</h3>
        </div>
        <div class="timeline">
          <div v-for="stage in stages" :key="stage.id" class="timeline-item is-done">
            <div class="timeline-title">{{ stage.stageName }}</div>
            <div class="timeline-content">{{ stage.description }}</div>
          </div>
        </div>
      </div>

      <div class="card tips-card">
        <div class="card-header">
          <h3 class="card-title">💡 养护小贴士</h3>
        </div>
        <div class="tips-list">
          <div class="tip-item">
            <span class="tip-icon">💧</span>
            <span>播种初期保持土壤湿润，避免积水</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">☀️</span>
            <span>萌芽后逐渐增加光照，避免暴晒</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">🌡️</span>
            <span>最适发芽温度 20-25°C</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">🪴</span>
            <span>长出 2-3 片真叶后可进行移栽</span>
          </div>
          <div class="tip-item">
            <span class="tip-icon">🌿</span>
            <span>移栽后需缓苗一周，避免强光直射</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { getSeedList } from '@/api/seed'
import { getSowingList } from '@/api/sowing'
import { getStageList } from '@/api/stage'
import { getDashboardStats } from '@/api/dashboard'
import dayjs from 'dayjs'

const metricConfigs = {
  totalSeeds: {
    icon: '🌱',
    label: '种子总量',
    themeClass: 'stat-primary',
    hint: '粒',
    topVarietyKey: 'totalQty'
  },
  activeSowings: {
    icon: '🌾',
    label: '活跃播种批次',
    themeClass: 'stat-success',
    hint: '批',
    topVarietyKey: 'batchCount'
  },
  expiringSeeds: {
    icon: '⏰',
    label: '临期种子',
    themeClass: 'stat-warning',
    hint: '30天内到期',
    topVarietyKey: 'seedCount'
  },
  pendingTransplants: {
    icon: '🪴',
    label: '待移栽批次',
    themeClass: 'stat-info',
    hint: '可移栽',
    topVarietyKey: 'batchCount'
  },
  abnormalHealth: {
    icon: '⚠️',
    label: '异常健康状态',
    themeClass: 'stat-danger',
    hint: '需关注',
    topVarietyKey: 'issueCount'
  }
}

const stats = reactive({
  totalSeeds: { count: 0, varieties: [], details: [] },
  activeSowings: { count: 0, varieties: [], details: [] },
  expiringSeeds: { count: 0, varieties: [], details: [] },
  pendingTransplants: { count: 0, varieties: [], details: [] },
  abnormalHealth: { count: 0, varieties: [], details: [] }
})

const seedList = ref([])
const sowingList = ref([])
const stages = ref([])

const activeMetric = ref(null)
const activeVariety = ref(null)

const recentSeeds = computed(() => seedList.value.slice(0, 5))
const recentSowings = computed(() => sowingList.value.slice(0, 5))

const sourceTypeMap = {
  PURCHASE: '购入',
  HARVEST: '采收'
}

const activeMetricData = computed(() => {
  if (!activeMetric.value) return null
  return stats[activeMetric.value]
})

const filteredDetails = computed(() => {
  if (!activeMetricData.value || !activeMetricData.value.details) return []
  const details = activeMetricData.value.details
  if (!activeVariety.value) return details
  return details.filter(d => d.varietyName === activeVariety.value)
})

const formatNumber = (n) => {
  if (n == null) return 0
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toLocaleString()
}

const formatDate = (date) => {
  return dayjs(date).format('MM月DD日')
}

const getMetricValue = (key) => {
  return stats[key]?.count ?? 0
}

const getTopVarieties = (key) => {
  const varieties = stats[key]?.varieties || []
  return varieties.slice(0, 3)
}

const toggleMetric = (key) => {
  activeMetric.value = activeMetric.value === key ? null : key
  activeVariety.value = null
}

const getDetailName = (d) => d.varietyName

const getDetailSub = (d) => {
  const key = activeMetric.value
  if (key === 'totalSeeds') {
    return `${sourceTypeMap[d.sourceType] || d.sourceType || ''} · 入库 ${d.acquireTime || ''}`
  }
  if (key === 'activeSowings' || key === 'pendingTransplants') {
    return `播种 ${formatDate(d.sowingTime)} · 已${d.daysSince != null ? d.daysSince + '天' : ''}`
  }
  if (key === 'expiringSeeds') {
    return `到期 ${d.expireDate || ''}`
  }
  if (key === 'abnormalHealth') {
    return `${d.stageName || ''} · 记录 ${formatDate(d.recordTime)}`
  }
  return ''
}

const getDetailMeta = (d) => {
  const key = activeMetric.value
  if (key === 'totalSeeds') {
    return `剩余 ${d.remainingQuantity} 粒`
  }
  if (key === 'activeSowings' || key === 'pendingTransplants') {
    return `${d.sowingQuantity} 粒`
  }
  if (key === 'expiringSeeds') {
    return `剩余 ${d.remainingQuantity} 粒`
  }
  return ''
}

const getDetailBadge = (d) => {
  const key = activeMetric.value
  if (key === 'expiringSeeds') {
    const dl = d.daysLeft
    if (dl < 0) return `已过期${Math.abs(dl)}天`
    if (dl === 0) return '今天到期'
    return `剩${dl}天`
  }
  if (key === 'abnormalHealth') {
    return d.healthStatus || '异常'
  }
  if (key === 'pendingTransplants') {
    return '待移栽'
  }
  if (key === 'activeSowings') {
    return '培育中'
  }
  if (key === 'totalSeeds') {
    return '在库'
  }
  return ''
}

const loadData = async () => {
  try {
    const [seeds, sowings, stageList, dashboardStats] = await Promise.all([
      getSeedList(),
      getSowingList(),
      getStageList(),
      getDashboardStats()
    ])
    seedList.value = seeds || []
    sowingList.value = sowings || []
    stages.value = stageList || []
    if (dashboardStats) {
      Object.keys(metricConfigs).forEach(key => {
        if (dashboardStats[key]) {
          stats[key] = dashboardStats[key]
        }
      })
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

@media (max-width: 768px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

.more-link {
  font-size: 13px;
  color: #67c23a;
  text-decoration: none;
  cursor: pointer;
}

.seed-list, .sowing-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.seed-item, .sowing-item {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  transition: background 0.3s;

  &:hover {
    background: #ecf5ff;
  }
}

.seed-name, .sowing-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.seed-meta, .sowing-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.seed-source, .sowing-date {
  color: #67c23a;
}

.seed-qty, .sowing-qty {
  color: #e6a23c;
}

.tips-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  background: #fdf6ec;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;

  .tip-icon {
    font-size: 18px;
    flex-shrink: 0;
  }
}

.stat-card {
  cursor: pointer;
  position: relative;
  border: 2px solid transparent;

  &.active {
    border-color: currentColor;
    transform: translateY(-2px);
  }

  .stat-hint {
    font-size: 12px;
    color: #c0c4cc;
    margin-top: 4px;
  }

  .stat-variety-tags {
    margin-top: 10px;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
  }

  .mini-tag {
    display: inline-block;
    padding: 2px 8px;
    font-size: 11px;
    background: rgba(103, 194, 58, 0.1);
    color: #67c23a;
    border-radius: 10px;
    max-width: 80px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}

.stat-card.stat-primary .mini-tag {
  background: rgba(64, 158, 255, 0.1);
  color: #409eff;
}
.stat-card.stat-success .mini-tag {
  background: rgba(103, 194, 58, 0.1);
  color: #67c23a;
}
.stat-card.stat-warning .mini-tag {
  background: rgba(230, 162, 60, 0.1);
  color: #e6a23c;
}
.stat-card.stat-info .mini-tag {
  background: rgba(90, 200, 250, 0.1);
  color: #5ac8fa;
}
.stat-card.stat-danger .mini-tag {
  background: rgba(245, 108, 108, 0.1);
  color: #f56c6c;
}

.stat-info .stat-value {
  color: #5ac8fa;
}

.metric-detail {
  margin-bottom: 24px;

  .close-btn {
    cursor: pointer;
    font-size: 24px;
    color: #c0c4cc;
    line-height: 1;
    padding: 0 8px;
    transition: color 0.2s;

    &:hover {
      color: #f56c6c;
    }
  }
}

.detail-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed #ebeef5;
}

.variety-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 10px;
}

.variety-card {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &:hover {
    background: #ecf5ff;
    border-color: #409eff33;
  }

  &.active {
    background: #ecf5ff;
    border-color: #409eff;
  }

  .variety-name {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  .variety-metrics {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  .variety-metric {
    font-size: 11px;
    padding: 2px 6px;
    background: white;
    border-radius: 4px;
    color: #606266;

    &.danger {
      color: #f56c6c;
      background: #fef0f0;
    }

    &.warning {
      color: #e6a23c;
      background: #fdf6ec;
    }
  }

  .health-tags {
    margin-top: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .health-tag {
    font-size: 11px;
    padding: 2px 6px;
    background: #fef0f0;
    color: #f56c6c;
    border-radius: 4px;
  }
}

.detail-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 6px;
  transition: background 0.2s;

  &:hover {
    background: #f0f9eb;
  }

  .detail-main {
    flex: 1;
    min-width: 0;
  }

  .detail-name {
    font-size: 14px;
    font-weight: 500;
    color: #303133;
    margin-bottom: 4px;
  }

  .detail-sub {
    font-size: 12px;
    color: #909399;
  }

  .detail-right {
    text-align: right;
    flex-shrink: 0;
    margin-left: 12px;
  }

  .detail-meta {
    font-size: 13px;
    color: #67c23a;
    font-weight: 500;
    margin-bottom: 4px;
  }

  .detail-badge {
    display: inline-block;
    font-size: 11px;
    padding: 3px 8px;
    border-radius: 4px;
    background: #67c23a1a;
    color: #67c23a;

    &.expiringSeeds {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.abnormalHealth {
      background: #fef0f0;
      color: #f56c6c;
    }

    &.pendingTransplants {
      background: #fdf6ec;
      color: #e6a23c;
    }

    &.activeSowings {
      background: #f0f9eb;
      color: #67c23a;
    }

    &.totalSeeds {
      background: #ecf5ff;
      color: #409eff;
    }
  }
}

.empty-state {
  &.small {
    padding: 30px 20px;

    .empty-icon {
      font-size: 32px;
    }

    .empty-text {
      font-size: 13px;
    }
  }
}
</style>
