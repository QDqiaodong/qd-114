<template>
  <div class="readiness-page">
    <div class="page-header">
      <h2 class="page-title">🌱 移栽就绪评估</h2>
      <div class="header-actions">
        <el-select
          v-model="selectedSowingId"
          placeholder="选择播种记录"
          filterable
          style="width: 340px; margin-right: 12px;"
          @change="handleSowingChange"
        >
          <el-option
            v-for="s in sowingList"
            :key="s.id"
            :value="s.id"
          >
            <div class="sowing-option-content">
              <div class="sowing-option-main">
                <span class="sowing-option-name">{{ s.varietyName }}</span>
                <span v-if="getVarietyBySowing(s).alias" class="sowing-option-alias">（{{ getVarietyBySowing(s).alias }}）</span>
              </div>
              <div class="sowing-option-sub">
                <el-tag size="small" type="info" effect="plain">{{ getVarietyBySowing(s).category || '未分类' }}</el-tag>
                <span class="sowing-option-date">📅 {{ formatDate(s.sowingTime) }}</span>
                <span class="sowing-option-qty">🌾 {{ s.sowingQuantity }}粒</span>
              </div>
            </div>
          </el-option>
        </el-select>
        <el-button type="primary" @click="refreshAssessment" :disabled="!selectedSowingId" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新评估
        </el-button>
      </div>
    </div>

    <div v-if="readinessData" class="assessment-content">
      <div class="card overview-card">
        <div class="overview-header">
          <div class="overview-info">
            <h3 class="variety-name">🌱 {{ readinessData.varietyName }}</h3>
            <div class="sowing-meta">
              <span>📅 播种: {{ formatDateTime(readinessData.sowingTime) }}</span>
              <span>⏱️ 已生长: {{ readinessData.actualSeedlingDays }} 天</span>
              <span v-if="readinessData.seedlingDays">🎯 预期育苗期: {{ readinessData.seedlingDays }} 天</span>
            </div>
          </div>
          <div class="overview-result" :class="getResultClass(readinessData.overallResult)">
            <div class="result-icon">
              {{ getResultIcon(readinessData.overallResult) }}
            </div>
            <div class="result-info">
              <div class="result-title">{{ getResultDisplayName(readinessData.overallResult) }}</div>
              <div class="result-desc">{{ getResultDescription(readinessData.overallResult) }}</div>
            </div>
          </div>
        </div>

        <div class="score-section">
          <div class="score-ring">
            <svg viewBox="0 0 120 120" class="score-svg">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#f0f0f0" stroke-width="10" />
              <circle
                cx="60"
                cy="60"
                r="50"
                fill="none"
                :stroke="getScoreColor(readinessData.overallScore)"
                stroke-width="10"
                stroke-linecap="round"
                :stroke-dasharray="getScoreDashArray(readinessData.overallScore)"
                stroke-dashoffset="0"
                transform="rotate(-90 60 60)"
                class="score-circle"
              />
              <text x="60" y="65" text-anchor="middle" class="score-text">{{ readinessData.overallScore }}</text>
              <text x="60" y="82" text-anchor="middle" class="score-label">综合评分</text>
            </svg>
          </div>
          <div class="quick-metrics">
            <div class="metric-item" v-if="readinessData.currentPlantHeight != null">
              <span class="metric-icon">📏</span>
              <div class="metric-info">
                <div class="metric-value">{{ readinessData.currentPlantHeight }} cm</div>
                <div class="metric-label">当前株高</div>
              </div>
            </div>
            <div class="metric-item" v-if="readinessData.currentLeafCount != null">
              <span class="metric-icon">🍃</span>
              <div class="metric-info">
                <div class="metric-value">{{ readinessData.currentLeafCount }} 片</div>
                <div class="metric-label">当前叶片数</div>
              </div>
            </div>
            <div class="metric-item" v-if="readinessData.currentRootDevelopment">
              <span class="metric-icon">🌳</span>
              <div class="metric-info">
                <div class="metric-value">{{ readinessData.currentRootDevelopment }}</div>
                <div class="metric-label">根系发育</div>
              </div>
            </div>
            <div class="metric-item" v-if="readinessData.currentHealthStatus">
              <span class="metric-icon">💚</span>
              <div class="metric-info">
                <div class="metric-value">{{ readinessData.currentHealthStatus }}</div>
                <div class="metric-label">健康状态</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="card criteria-card">
        <h4 class="section-title">📊 详细评估指标</h4>
        <div class="criteria-list">
          <div
            v-for="criterion in readinessData.criteria"
            :key="criterion.criterionCode"
            class="criterion-item"
            :class="{ 'is-passed': criterion.passed, 'is-failed': !criterion.passed, 'is-missing': criterion.score == null }"
          >
            <div class="criterion-header">
              <div class="criterion-name">
                <span class="criterion-icon">{{ getCriterionIcon(criterion.criterionCode) }}</span>
                {{ criterion.criterionName }}
              </div>
              <div class="criterion-score" :class="getScoreClass(criterion.score)">
                <template v-if="criterion.score != null">
                  {{ criterion.score }} 分
                  <span class="weight">（权重 {{ (criterion.weight * 100).toFixed(0) }}%）</span>
                </template>
                <template v-else>
                  <span class="missing-text">数据缺失</span>
                </template>
              </div>
            </div>

            <el-progress
              v-if="criterion.score != null"
              :percentage="criterion.score"
              :color="getProgressColor(criterion.score)"
              :stroke-width="8"
              class="criterion-progress"
            />

            <div class="criterion-details">
              <div class="detail-row">
                <span class="detail-label">实际值：</span>
                <span class="detail-value">{{ criterion.actualValue }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">阈值标准：</span>
                <span class="detail-value">{{ criterion.threshold }}</span>
              </div>
              <div class="detail-row">
                <span class="detail-label">评估结果：</span>
                <span class="detail-value" :class="getAssessmentClass(criterion.passed)">
                  {{ criterion.assessment }}
                </span>
              </div>
              <div class="detail-row suggestion-row">
                <span class="detail-label">💡 建议：</span>
                <span class="detail-value">{{ criterion.suggestion }}</span>
              </div>
            </div>

            <div class="criterion-status-badge">
              <el-tag v-if="criterion.score == null" type="warning" size="small">
                数据不足
              </el-tag>
              <el-tag v-else-if="criterion.passed" type="success" size="small">
                ✓ 达标
              </el-tag>
              <el-tag v-else type="danger" size="small">
                ✗ 未达标
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <div class="card summary-card">
        <h4 class="section-title">📋 综合判断依据</h4>
        <div class="summary-content">
          <div class="passed-items" v-if="passedCriteria.length > 0">
            <div class="summary-title passed-title">✅ 已达标指标（{{ passedCriteria.length }} 项）</div>
            <ul>
              <li v-for="c in passedCriteria" :key="c.criterionCode">
                <b>{{ c.criterionName }}</b>：{{ c.actualValue }} - {{ c.assessment }}
              </li>
            </ul>
          </div>
          <div class="failed-items" v-if="failedCriteria.length > 0">
            <div class="summary-title failed-title">⚠️ 未达标指标（{{ failedCriteria.length }} 项）</div>
            <ul>
              <li v-for="c in failedCriteria" :key="c.criterionCode">
                <b>{{ c.criterionName }}</b>：{{ c.actualValue }} - {{ c.assessment }}
              </li>
            </ul>
          </div>
          <div class="missing-items" v-if="missingCriteria.length > 0">
            <div class="summary-title missing-title">❓ 数据缺失（{{ missingCriteria.length }} 项）</div>
            <ul>
              <li v-for="c in missingCriteria" :key="c.criterionCode">
                <b>{{ c.criterionName }}</b>：{{ c.suggestion }}
              </li>
            </ul>
          </div>
        </div>
        <div class="final-suggestion">
          <el-alert
            :title="getFinalSuggestion()"
            :type="getAlertType()"
            show-icon
            :closable="false"
          />
        </div>
      </div>
    </div>

    <div v-else-if="!loading && selectedSowingId" class="card empty-state-card">
      <div class="empty-icon">📊</div>
      <div class="empty-text">暂无评估数据，请点击"刷新评估"按钮</div>
    </div>

    <div v-else-if="!loading && !selectedSowingId" class="card empty-state-card">
      <div class="empty-icon">🌱</div>
      <div class="empty-text">请先选择一个播种记录开始评估</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getSowingList } from '@/api/sowing'
import { getVarietyList } from '@/api/variety'
import { getTransplantReadiness } from '@/api/growth'
import { formatDate, formatDateTime } from '@/utils/date'

const route = useRoute()
const loading = ref(false)
const sowingList = ref([])
const varietyList = ref([])
const selectedSowingId = ref(null)
const readinessData = ref(null)

const passedCriteria = computed(() => {
  if (!readinessData.value?.criteria) return []
  return readinessData.value.criteria.filter(c => c.passed && c.score != null)
})

const failedCriteria = computed(() => {
  if (!readinessData.value?.criteria) return []
  return readinessData.value.criteria.filter(c => !c.passed && c.score != null)
})

const missingCriteria = computed(() => {
  if (!readinessData.value?.criteria) return []
  return readinessData.value.criteria.filter(c => c.score == null)
})

const getVarietyBySowing = (sowing) => {
  return varietyList.value.find(v => v.id === sowing.varietyId) || {}
}

const getResultClass = (result) => {
  const map = {
    'READY': 'is-ready',
    'ALMOST_READY': 'is-almost',
    'NOT_READY': 'is-not-ready',
    'NEED_MORE_DATA': 'is-need-data'
  }
  return map[result] || ''
}

const getResultIcon = (result) => {
  const map = {
    'READY': '✅',
    'ALMOST_READY': '🌿',
    'NOT_READY': '⏳',
    'NEED_MORE_DATA': '📋'
  }
  return map[result] || '❓'
}

const getResultDisplayName = (result) => {
  const map = {
    'READY': '完全就绪',
    'ALMOST_READY': '基本就绪',
    'NOT_READY': '未就绪',
    'NEED_MORE_DATA': '数据不足'
  }
  return map[result] || '未知'
}

const getResultDescription = (result) => {
  const map = {
    'READY': '已满足所有移栽条件，可立即移栽',
    'ALMOST_READY': '基本满足移栽条件，建议观察1-3天后移栽',
    'NOT_READY': '暂不满足移栽条件，需要继续培育',
    'NEED_MORE_DATA': '缺少必要的生长记录，无法进行评估'
  }
  return map[result] || ''
}

const getScoreColor = (score) => {
  if (score == null) return '#909399'
  if (score >= 85) return '#67c23a'
  if (score >= 70) return '#e6a23c'
  return '#f56c6c'
}

const getScoreDashArray = (score) => {
  if (score == null) return '0 314'
  const circumference = 2 * Math.PI * 50
  const dashLength = (score / 100) * circumference
  return `${dashLength} ${circumference}`
}

const getScoreClass = (score) => {
  if (score == null) return 'score-missing'
  if (score >= 85) return 'score-high'
  if (score >= 70) return 'score-medium'
  return 'score-low'
}

const getProgressColor = (score) => {
  if (score >= 85) return '#67c23a'
  if (score >= 70) return '#e6a23c'
  return '#f56c6c'
}

const getCriterionIcon = (code) => {
  const map = {
    'PLANT_HEIGHT': '📏',
    'LEAF_COUNT': '🍃',
    'ROOT_DEVELOPMENT': '🌳',
    'HEALTH_STATUS': '💚',
    'SEEDLING_AGE': '⏱️'
  }
  return map[code] || '📊'
}

const getAssessmentClass = (passed) => {
  return passed ? 'assessment-passed' : 'assessment-failed'
}

const getFinalSuggestion = () => {
  if (!readinessData.value) return ''
  const result = readinessData.value.overallResult
  if (result === 'READY') {
    return '🎉 恭喜！该批次幼苗已完全满足移栽条件，可以立即进行移栽分盆操作。'
  } else if (result === 'ALMOST_READY') {
    return '🌿 该批次幼苗基本满足移栽条件，建议再观察1-3天，待各项指标更理想后进行移栽。'
  } else if (result === 'NOT_READY') {
    return '⏳ 该批次幼苗暂不满足移栽条件，请根据上述未达标项的建议继续培育。'
  } else {
    return '📋 请先完善生长记录数据，填写株高、叶片数、根系发育和健康状态等信息后再进行评估。'
  }
}

const getAlertType = () => {
  if (!readinessData.value) return 'info'
  const result = readinessData.value.overallResult
  const map = {
    'READY': 'success',
    'ALMOST_READY': 'warning',
    'NOT_READY': 'error',
    'NEED_MORE_DATA': 'info'
  }
  return map[result] || 'info'
}

const loadSowings = async () => {
  try {
    const data = await getSowingList()
    sowingList.value = data || []
  } catch (e) {
    console.error(e)
  }
}

const loadVarieties = async () => {
  try {
    const data = await getVarietyList()
    varietyList.value = data || []
  } catch (e) {
    console.error(e)
  }
}

const loadReadiness = async () => {
  if (!selectedSowingId.value) return
  loading.value = true
  try {
    const data = await getTransplantReadiness(selectedSowingId.value)
    readinessData.value = data
  } catch (e) {
    console.error(e)
    ElMessage.error('加载评估数据失败')
  } finally {
    loading.value = false
  }
}

const handleSowingChange = () => {
  readinessData.value = null
  loadReadiness()
}

const refreshAssessment = () => {
  loadReadiness()
  ElMessage.success('评估数据已刷新')
}

onMounted(async () => {
  await Promise.all([loadSowings(), loadVarieties()])
  if (route.query.sowingId) {
    selectedSowingId.value = Number(route.query.sowingId)
    loadReadiness()
  }
})
</script>

<style scoped>
.readiness-page {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
}

.assessment-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.overview-card {
  background: linear-gradient(135deg, #f5f7fa 0%, #e8f4f8 100%);
}

.overview-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.variety-name {
  font-size: 20px;
  color: #303133;
  margin: 0 0 8px 0;
}

.sowing-meta {
  display: flex;
  gap: 20px;
  color: #606266;
  font-size: 14px;
}

.overview-result {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.overview-result.is-ready {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%);
}

.overview-result.is-almost {
  background: linear-gradient(135deg, #fdf6ec 0%, #faecd8 100%);
}

.overview-result.is-not-ready {
  background: linear-gradient(135deg, #fef0f0 0%, #fde2e2 100%);
}

.overview-result.is-need-data {
  background: linear-gradient(135deg, #f4f4f5 0%, #e9e9eb 100%);
}

.result-icon {
  font-size: 36px;
}

.result-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.result-desc {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
}

.score-section {
  display: flex;
  align-items: center;
  gap: 40px;
}

.score-ring {
  width: 140px;
  height: 140px;
}

.score-svg {
  width: 100%;
  height: 100%;
}

.score-circle {
  transition: stroke-dasharray 1s ease-in-out;
}

.score-text {
  font-size: 28px;
  font-weight: bold;
  fill: #303133;
}

.score-label {
  font-size: 11px;
  fill: #909399;
}

.quick-metrics {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.metric-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.metric-icon {
  font-size: 24px;
}

.metric-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.metric-label {
  font-size: 12px;
  color: #909399;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px 0;
}

.criteria-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.criterion-item {
  position: relative;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid #ebeef5;
  background: #fff;
  transition: all 0.3s;
}

.criterion-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-1px);
}

.criterion-item.is-passed {
  border-color: #c2e7b0;
}

.criterion-item.is-failed {
  border-color: #fbc4c4;
}

.criterion-item.is-missing {
  border-color: #f5dab1;
  background: #fdf6ec;
}

.criterion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.criterion-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}

.criterion-icon {
  font-size: 20px;
}

.criterion-score {
  font-size: 18px;
  font-weight: 600;
}

.criterion-score.score-high {
  color: #67c23a;
}

.criterion-score.score-medium {
  color: #e6a23c;
}

.criterion-score.score-low {
  color: #f56c6c;
}

.criterion-score .weight {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
  margin-left: 4px;
}

.criterion-score .missing-text {
  font-size: 14px;
  color: #e6a23c;
  font-weight: normal;
}

.criterion-progress {
  margin-bottom: 16px;
}

.criterion-details {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-row {
  display: flex;
  font-size: 13px;
}

.detail-label {
  min-width: 80px;
  color: #909399;
}

.detail-value {
  color: #606266;
  flex: 1;
}

.detail-value.assessment-passed {
  color: #67c23a;
}

.detail-value.assessment-failed {
  color: #f56c6c;
}

.suggestion-row {
  padding-top: 8px;
  margin-top: 4px;
  border-top: 1px dashed #ebeef5;
}

.suggestion-row .detail-value {
  color: #409eff;
}

.criterion-status-badge {
  position: absolute;
  top: 20px;
  right: 20px;
}

.summary-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.passed-title {
  color: #67c23a;
}

.failed-title {
  color: #e6a23c;
}

.missing-title {
  color: #909399;
}

.summary-content ul {
  margin: 0;
  padding-left: 20px;
}

.summary-content li {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

.summary-content b {
  color: #303133;
}

.final-suggestion {
  margin-top: 20px;
}

.empty-state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  gap: 16px;
}

.empty-icon {
  font-size: 64px;
}

.empty-text {
  font-size: 14px;
  color: #909399;
}

.sowing-option-content {
  line-height: 1.4;
}

.sowing-option-main {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sowing-option-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.sowing-option-alias {
  font-size: 12px;
  color: #909399;
}

.sowing-option-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.sowing-option-date,
.sowing-option-qty {
  font-size: 12px;
  color: #606266;
}

.card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
</style>
