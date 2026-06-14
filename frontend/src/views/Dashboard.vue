<template>
  <div class="dashboard">
    <div class="stat-grid">
      <div class="stat-card stat-primary">
        <div class="stat-icon">🌱</div>
        <div class="stat-value">{{ seedCount }}</div>
        <div class="stat-label">种子品种数</div>
      </div>
      <div class="stat-card stat-success">
        <div class="stat-icon">🌾</div>
        <div class="stat-value">{{ sowingCount }}</div>
        <div class="stat-label">播种记录</div>
      </div>
      <div class="stat-card stat-warning">
        <div class="stat-icon">🪴</div>
        <div class="stat-value">{{ growingCount }}</div>
        <div class="stat-label">培育中</div>
      </div>
      <div class="stat-card stat-danger">
        <div class="stat-icon">🌸</div>
        <div class="stat-value">{{ varietyCount }}</div>
        <div class="stat-label">花卉品种</div>
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
import { ref, onMounted, computed } from 'vue'
import { getSeedList } from '@/api/seed'
import { getSowingList } from '@/api/sowing'
import { getVarietyList } from '@/api/variety'
import { getStageList } from '@/api/stage'
import dayjs from 'dayjs'

const seedList = ref([])
const sowingList = ref([])
const varietyList = ref([])
const stages = ref([])

const seedCount = computed(() => seedList.value.length)
const sowingCount = computed(() => sowingList.value.length)
const varietyCount = computed(() => varietyList.value.length)
const growingCount = computed(() => sowingList.value.length)

const recentSeeds = computed(() => seedList.value.slice(0, 5))
const recentSowings = computed(() => sowingList.value.slice(0, 5))

const sourceTypeMap = {
  PURCHASE: '购入',
  HARVEST: '采收'
}

const formatDate = (date) => {
  return dayjs(date).format('MM月DD日')
}

const loadData = async () => {
  try {
    const [seeds, sowings, varieties, stageList] = await Promise.all([
      getSeedList(),
      getSowingList(),
      getVarietyList(),
      getStageList()
    ])
    seedList.value = seeds || []
    sowingList.value = sowings || []
    varietyList.value = varieties || []
    stages.value = stageList || []
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
</style>
