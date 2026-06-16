<template>
  <div class="recovery-board">
    <div class="page-header">
      <h2 class="page-title">🌿 移栽恢复观察板</h2>
      <p class="page-subtitle">集中展示移栽后各批次的恢复状态，重点关注需要持续观察的批次</p>
    </div>

    <div class="stat-grid">
      <div class="stat-card total">
        <div class="stat-icon">🪴</div>
        <div class="stat-value">{{ boardData?.totalCount || 0 }}</div>
        <div class="stat-label">移栽总批次</div>
      </div>
      <div class="stat-card observing">
        <div class="stat-icon">👀</div>
        <div class="stat-value">{{ boardData?.observingCount || 0 }}</div>
        <div class="stat-label">观察中</div>
        <div class="stat-hint">缓苗期+恢复中</div>
      </div>
      <div class="stat-card abnormal">
        <div class="stat-icon">⚠️</div>
        <div class="stat-value">{{ boardData?.abnormalCount || 0 }}</div>
        <div class="stat-label">需关注</div>
        <div class="stat-hint">存在健康异常</div>
      </div>
      <div class="stat-card recovered">
        <div class="stat-icon">✅</div>
        <div class="stat-value">{{ boardData?.recoveredCount || 0 }}</div>
        <div class="stat-label">已恢复</div>
      </div>
    </div>

    <div class="card section-card highlight-section">
      <div class="card-header">
        <h3 class="card-title">
          <span class="highlight-icon">🔔</span>
          需要继续观察的批次
          <span class="badge danger">{{ boardData?.needObservationItems?.length || 0 }}</span>
        </h3>
      </div>

      <div v-if="boardData?.needObservationItems?.length > 0" class="observation-cards">
        <div
          v-for="item in boardData.needObservationItems"
          :key="item.id"
          class="obs-card"
          :class="'level-' + item.recoveryStatusLevel"
        >
          <div class="obs-card-header">
            <div class="obs-variety">{{ item.varietyName }}</div>
            <div class="obs-status-tag" :class="'status-' + item.recoveryStatusLevel">
              {{ item.recoveryStatusText }}
            </div>
          </div>

          <div class="obs-card-body">
            <div class="obs-info-grid">
              <div class="obs-info-item">
                <span class="obs-info-label">花盆规格</span>
                <span class="obs-info-value">{{ item.potSpecification || '-' }}</span>
              </div>
              <div class="obs-info-item">
                <span class="obs-info-label">移栽数量</span>
                <span class="obs-info-value">{{ item.transplantQuantity || 0 }} 株</span>
              </div>
              <div class="obs-info-item">
                <span class="obs-info-label">光照需求</span>
                <span class="obs-info-value">{{ item.lightRequirement || '-' }}</span>
              </div>
              <div class="obs-info-item">
                <span class="obs-info-label">浇水频率</span>
                <span class="obs-info-value">{{ item.wateringFrequency || '-' }}</span>
              </div>
            </div>

            <div class="obs-meta-row">
              <span class="obs-meta">
                📅 移栽 {{ formatDate(item.transplantTime) }}
                <em>（{{ item.daysSinceTransplant }} 天前）</em>
              </span>
            </div>

            <div v-if="item.observationReason" class="obs-reason">
              <span class="reason-icon">💡</span>
              <span class="reason-text">{{ item.observationReason }}</span>
            </div>

            <div v-if="item.recoveryTips" class="obs-tips">
              <div class="tips-title">📋 恢复提示</div>
              <div class="tips-content">{{ item.recoveryTips }}</div>
            </div>

            <div v-if="item.latestHealthStatus || item.currentStageName" class="obs-latest">
              <div class="latest-row">
                <span v-if="item.currentStageName" class="latest-stage">
                  🌱 {{ item.currentStageName }}
                </span>
                <span v-if="item.latestHealthStatus" class="latest-health">
                  💚 {{ item.latestHealthStatus }}
                </span>
              </div>
              <div v-if="item.latestTrackingTime" class="latest-time">
                最近记录：{{ formatDateTime(item.latestTrackingTime) }}
              </div>
            </div>
          </div>

          <div class="obs-card-footer">
            <el-button type="primary" size="small" @click="goToGrowth(item.sowingId)">
              查看生长跟踪
            </el-button>
            <el-button type="default" size="small" @click="goToTransplantDetail(item.id)">
              移栽详情
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">🎉</div>
        <div class="empty-text">太棒了！当前没有需要特别观察的批次</div>
      </div>
    </div>

    <div class="card section-card">
      <div class="card-header">
        <h3 class="card-title">
          📊 按品种统计
        </h3>
      </div>

      <div v-if="boardData?.byVariety?.length > 0" class="variety-stats">
        <div
          v-for="v in boardData.byVariety"
          :key="v.varietyId"
          class="variety-stat-item"
        >
          <div class="vs-header">
            <span class="vs-name">{{ v.varietyName }}</span>
            <span class="vs-count">{{ v.totalCount }} 批</span>
          </div>
          <div class="vs-bar">
            <div
              class="vs-bar-fill observing"
              :style="{ width: (v.totalCount > 0 ? (v.observingCount / v.totalCount * 100) : 0) + '%' }"
            ></div>
            <div
              class="vs-bar-fill abnormal"
              :style="{
                width: (v.totalCount > 0 ? (v.abnormalCount / v.totalCount * 100) : 0) + '%',
                left: (v.totalCount > 0 ? (v.observingCount / v.totalCount * 100) : 0) + '%'
              }"
            ></div>
          </div>
          <div class="vs-footer">
            <span class="vs-tag observing">
              👀 观察中 {{ v.observingCount }}
            </span>
            <span class="vs-tag abnormal">
              ⚠️ 异常 {{ v.abnormalCount }}
            </span>
            <span class="vs-tag recovered">
              ✅ 已恢复 {{ v.totalCount - v.observingCount - v.abnormalCount }}
            </span>
          </div>
        </div>
      </div>

      <div v-else class="empty-state small">
        <div class="empty-text">暂无品种统计数据</div>
      </div>
    </div>

    <div class="card section-card">
      <div class="card-header">
        <h3 class="card-title">📋 全部移栽恢复记录</h3>
      </div>

      <el-table
        v-loading="loading"
        :data="boardData?.allItems || []"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="varietyName" label="花卉品种" min-width="120" />
        <el-table-column label="恢复状态" width="100">
          <template #default="{ row }">
            <span class="table-status-tag" :class="'status-' + row.recoveryStatusLevel">
              {{ row.recoveryStatusText }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="potSpecification" label="盆规格" width="120" />
        <el-table-column prop="lightRequirement" label="光照需求" width="120" />
        <el-table-column prop="wateringFrequency" label="浇水频率" width="120" />
        <el-table-column label="移栽时间" width="160">
          <template #default="{ row }">{{ formatDate(row.transplantTime) }}</template>
        </el-table-column>
        <el-table-column label="已移栽" width="90">
          <template #default="{ row }">{{ row.daysSinceTransplant }} 天</template>
        </el-table-column>
        <el-table-column label="最新健康" min-width="120">
          <template #default="{ row }">
            <span v-if="row.latestHealthStatus">{{ row.latestHealthStatus }}</span>
            <span v-else class="muted">暂无记录</span>
          </template>
        </el-table-column>
        <el-table-column label="观察提示" min-width="160">
          <template #default="{ row }">
            <span v-if="row.needObservation" class="obs-hint">
              {{ row.observationReason }}
            </span>
            <span v-else class="normal-hint">恢复良好</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goToGrowth(row.sowingId)">
              生长跟踪
            </el-button>
            <el-button type="primary" link size="small" @click="goToTransplantDetail(row.id)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!boardData?.allItems?.length && !loading" class="empty-state">
        <div class="empty-icon">🪴</div>
        <div class="empty-text">暂无移栽记录</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecoveryBoard } from '@/api/transplant'
import { formatDate, formatDateTime } from '@/utils/date'

const router = useRouter()
const loading = ref(false)
const boardData = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const data = await getRecoveryBoard()
    boardData.value = data
  } catch (e) {
    console.error('加载恢复观察板数据失败', e)
  } finally {
    loading.value = false
  }
}

const goToGrowth = (sowingId) => {
  router.push({ path: '/growth', query: { sowingId } })
}

const goToTransplantDetail = (id) => {
  router.push({ path: '/transplants', query: { id, tab: 'detail' } })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.recovery-board {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 6px 0;
}

.page-subtitle {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 2px solid transparent;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.stat-card.total {
  border-color: #409eff33;
}
.stat-card.total .stat-value { color: #409eff; }

.stat-card.observing {
  border-color: #e6a23c33;
}
.stat-card.observing .stat-value { color: #e6a23c; }

.stat-card.abnormal {
  border-color: #f56c6c33;
}
.stat-card.abnormal .stat-value { color: #f56c6c; }

.stat-card.recovered {
  border-color: #67c23a33;
}
.stat-card.recovered .stat-value { color: #67c23a; }

.stat-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}

.stat-hint {
  font-size: 12px;
  color: #c0c4cc;
}

.section-card {
  margin-bottom: 24px;
}

.highlight-section {
  border: 2px solid #f56c6c33;
  background: linear-gradient(135deg, #fef0f0 0%, #ffffff 60%);
}

.highlight-icon {
  margin-right: 4px;
}

.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
  margin-left: 8px;
}

.badge.danger {
  background: #fef0f0;
  color: #f56c6c;
}

.observation-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.obs-card {
  background: white;
  border-radius: 10px;
  border: 1px solid #ebeef5;
  overflow: hidden;
  transition: all 0.3s;
  display: flex;
  flex-direction: column;
}

.obs-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.obs-card.level-DANGER {
  border-color: #f56c6c;
  border-top: 3px solid #f56c6c;
}

.obs-card.level-WARNING {
  border-color: #e6a23c;
  border-top: 3px solid #e6a23c;
}

.obs-card.level-INFO {
  border-color: #5ac8fa;
  border-top: 3px solid #5ac8fa;
}

.obs-card.level-SUCCESS {
  border-color: #67c23a;
  border-top: 3px solid #67c23a;
}

.obs-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
}

.obs-variety {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.obs-status-tag {
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 10px;
  font-weight: 500;
}

.obs-status-tag.status-DANGER {
  background: #fef0f0;
  color: #f56c6c;
}

.obs-status-tag.status-WARNING {
  background: #fdf6ec;
  color: #e6a23c;
}

.obs-status-tag.status-INFO {
  background: #ecf5ff;
  color: #409eff;
}

.obs-status-tag.status-SUCCESS {
  background: #f0f9eb;
  color: #67c23a;
}

.obs-card-body {
  padding: 14px 16px;
  flex: 1;
}

.obs-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-bottom: 12px;
}

.obs-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.obs-info-label {
  font-size: 12px;
  color: #909399;
}

.obs-info-value {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
}

.obs-meta-row {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #ebeef5;
}

.obs-meta em {
  font-style: normal;
  color: #606266;
}

.obs-reason {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 10px;
  background: #fdf6ec;
  border-radius: 6px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #b88230;
}

.level-DANGER .obs-reason {
  background: #fef0f0;
  color: #f56c6c;
}

.reason-icon {
  flex-shrink: 0;
}

.reason-text {
  line-height: 1.5;
}

.obs-tips {
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 10px;
  border-left: 3px solid #5ac8fa;
}

.tips-title {
  font-size: 12px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}

.tips-content {
  font-size: 12px;
  color: #606266;
  line-height: 1.6;
}

.obs-latest {
  padding: 10px 12px;
  background: #f0f9eb;
  border-radius: 6px;
}

.latest-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 4px;
}

.latest-stage,
.latest-health {
  font-size: 12px;
  color: #67c23a;
}

.latest-health {
  color: #67c23a;
}

.latest-time {
  font-size: 11px;
  color: #909399;
}

.obs-card-footer {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  border-top: 1px solid #ebeef5;
  background: #fafbfc;
}

.variety-stats {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.variety-stat-item {
  padding: 14px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.vs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.vs-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.vs-count {
  font-size: 12px;
  color: #909399;
}

.vs-bar {
  position: relative;
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  margin-bottom: 10px;
  overflow: hidden;
}

.vs-bar-fill {
  position: absolute;
  top: 0;
  height: 100%;
  transition: width 0.3s;
}

.vs-bar-fill.observing {
  left: 0;
  background: #e6a23c;
}

.vs-bar-fill.abnormal {
  background: #f56c6c;
}

.vs-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.vs-tag {
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 4px;
  background: white;
}

.vs-tag.observing {
  color: #e6a23c;
  background: #fdf6ec;
}

.vs-tag.abnormal {
  color: #f56c6c;
  background: #fef0f0;
}

.vs-tag.recovered {
  color: #67c23a;
  background: #f0f9eb;
}

.table-status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.table-status-tag.status-DANGER {
  background: #fef0f0;
  color: #f56c6c;
}

.table-status-tag.status-WARNING {
  background: #fdf6ec;
  color: #e6a23c;
}

.table-status-tag.status-INFO {
  background: #ecf5ff;
  color: #409eff;
}

.table-status-tag.status-SUCCESS {
  background: #f0f9eb;
  color: #67c23a;
}

.obs-hint {
  font-size: 12px;
  color: #e6a23c;
}

.normal-hint {
  font-size: 12px;
  color: #67c23a;
}

.muted {
  color: #c0c4cc;
  font-size: 12px;
}

.empty-state {
  padding: 40px 20px;
  text-align: center;

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

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
}
</style>
