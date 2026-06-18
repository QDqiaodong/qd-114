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
            <el-button type="success" size="small" @click="openRecoveryRecord(item)">
              记录恢复
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="goToGrowth(row.sowingId)">
              生长跟踪
            </el-button>
            <el-button type="success" link size="small" @click="openRecoveryRecord(row)">
              记录恢复
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

    <el-dialog
      v-model="recoveryDialogVisible"
      title="移栽恢复记录"
      width="720px"
      destroy-on-close
      @close="handleRecoveryDialogClose"
    >
      <div v-if="currentTransplant" class="recovery-dialog">
        <div class="recovery-header">
          <div class="recovery-header-info">
            <h3 class="variety-name">{{ currentTransplant.varietyName }}</h3>
            <div class="recovery-meta">
              <span>🪴 {{ currentTransplant.potSpecification || '-' }}</span>
              <span>📅 移栽: {{ formatDate(currentTransplant.transplantTime) }}</span>
              <span>🌱 已移栽 {{ currentTransplant.daysSinceTransplant }} 天</span>
            </div>
          </div>
          <div class="recovery-stage-tag" :class="'stage-' + recoveryDetail?.recoveryStage">
            {{ recoveryDetail?.recoveryStageText || '加载中...' }}
          </div>
        </div>

        <div class="recovery-progress-section">
          <h4 class="section-title">📊 恢复进度</h4>
          <div class="progress-bar-wrapper">
            <div class="progress-bar">
              <div
                class="progress-fill"
                :style="{ width: Math.min(recoveryProgressPercent, 100) + '%' }"
              ></div>
            </div>
            <div class="progress-labels">
              <span>缓苗期</span>
              <span>恢复中</span>
              <span>已恢复</span>
            </div>
          </div>
          <div class="recovery-stats">
            <div class="stat-item">
              <span class="stat-label">记录天数</span>
              <span class="stat-value">{{ recoveryDetail?.records?.length || 0 }} 天</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">浇水次数</span>
              <span class="stat-value">{{ recoveryDetail?.wateringCount || 0 }} 次</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最新叶片</span>
              <span class="stat-value">{{ recoveryDetail?.latestLeafStatus || '-' }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">健康状态</span>
              <span class="stat-value" :class="{ danger: recoveryDetail?.hasAbnormal }">
                {{ recoveryDetail?.latestHealthStatus || '-' }}
              </span>
            </div>
          </div>
        </div>

        <div class="recovery-records-section">
          <div class="section-header">
            <h4 class="section-title">📝 恢复记录</h4>
            <el-button type="primary" size="small" @click="openAddRecord">
              <el-icon><Plus /></el-icon>
              新增记录
            </el-button>
          </div>

          <div v-if="recoveryDetail?.records?.length > 0" class="recovery-records-list">
            <div
              v-for="(record, index) in reversedRecords"
              :key="record.id"
              class="recovery-record-item"
            >
              <div class="record-date-badge">
                <span class="date-text">第 {{ record.recoveryDays }} 天</span>
                <span class="date-sub">{{ record.recordDate }}</span>
              </div>
              <div class="record-content">
                <div class="record-info-grid">
                  <div class="record-info-item">
                    <span class="record-info-label">叶片状态</span>
                    <span class="record-info-value">{{ record.leafStatus || '-' }}</span>
                  </div>
                  <div class="record-info-item">
                    <span class="record-info-label">叶片数量</span>
                    <span class="record-info-value">{{ record.leafCount != null ? record.leafCount + ' 片' : '-' }}</span>
                  </div>
                  <div class="record-info-item">
                    <span class="record-info-label">是否浇水</span>
                    <el-tag :type="record.wateringDone ? 'success' : 'info'" size="small">
                      {{ record.wateringDone ? '已浇水' : '未浇水' }}
                    </el-tag>
                  </div>
                  <div v-if="record.wateringAmount" class="record-info-item">
                    <span class="record-info-label">浇水量</span>
                    <span class="record-info-value">{{ record.wateringAmount }} ml</span>
                  </div>
                </div>
                <div class="record-health-row">
                  <span v-if="record.healthStatus" class="record-health">
                    💚 {{ record.healthStatus }}
                  </span>
                  <span v-if="record.temperature" class="record-env">
                    🌡️ {{ record.temperature }}℃
                  </span>
                  <span v-if="record.humidity" class="record-env">
                    💧 {{ record.humidity }}%
                  </span>
                  <span v-if="record.lightHours" class="record-env">
                    ☀️ {{ record.lightHours }}h
                  </span>
                </div>
                <div v-if="record.notes" class="record-notes">
                  📋 {{ record.notes }}
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-state small">
            <div class="empty-icon">📝</div>
            <div class="empty-text">暂无恢复记录，点击上方按钮添加</div>
          </div>
        </div>
      </div>
    </el-dialog>

    <el-dialog
      v-model="addRecordDialogVisible"
      :title="editRecordId ? '编辑恢复记录' : '新增恢复记录'"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="recordFormRef"
        :model="recordFormData"
        :rules="recordFormRules"
        label-width="100px"
      >
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker
            v-model="recordFormData.recordDate"
            type="date"
            placeholder="选择记录日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="恢复天数" prop="recoveryDays">
          <el-input-number v-model="recordFormData.recoveryDays" :min="0" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="叶片状态" prop="leafStatus">
              <el-select v-model="recordFormData.leafStatus" placeholder="请选择" style="width: 100%">
                <el-option label="正常" value="正常" />
                <el-option label="萎蔫" value="萎蔫" />
                <el-option label="发黄" value="发黄" />
                <el-option label="脱落" value="脱落" />
                <el-option label="卷曲" value="卷曲" />
                <el-option label="斑点" value="斑点" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="叶片数量" prop="leafCount">
              <el-input-number v-model="recordFormData.leafCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="是否浇水" prop="wateringDone">
          <el-switch v-model="recordFormData.wateringDone" />
        </el-form-item>
        <el-form-item v-if="recordFormData.wateringDone" label="浇水量(ml)" prop="wateringAmount">
          <el-input-number v-model="recordFormData.wateringAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="健康状态" prop="healthStatus">
          <el-select v-model="recordFormData.healthStatus" placeholder="请选择" style="width: 100%">
            <el-option label="健康" value="健康" />
            <el-option label="良好" value="良好" />
            <el-option label="一般" value="一般" />
            <el-option label="较弱" value="较弱" />
            <el-option label="病弱" value="病弱" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="温度(℃)" prop="temperature">
              <el-input-number v-model="recordFormData.temperature" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="湿度(%)" prop="humidity">
              <el-input-number v-model="recordFormData.humidity" :min="0" :max="100" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="光照(h)" prop="lightHours">
              <el-input-number v-model="recordFormData.lightHours" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="notes">
          <el-input v-model="recordFormData.notes" type="textarea" :rows="3" placeholder="记录其他观察情况" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addRecordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRecord" :loading="recordSubmitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getRecoveryBoard,
  getRecoveryDetail,
  createRecoveryRecord,
  updateRecoveryRecord,
  deleteRecoveryRecord
} from '@/api/transplant'
import { formatDate, formatDateTime } from '@/utils/date'

const router = useRouter()
const loading = ref(false)
const boardData = ref(null)

const recoveryDialogVisible = ref(false)
const currentTransplant = ref(null)
const recoveryDetail = ref(null)
const recoveryDetailLoading = ref(false)

const addRecordDialogVisible = ref(false)
const recordFormRef = ref(null)
const recordSubmitting = ref(false)
const editRecordId = ref(null)

const recordFormData = ref({
  transplantId: null,
  recordDate: '',
  recoveryDays: 0,
  leafStatus: '',
  leafCount: null,
  wateringDone: false,
  wateringAmount: null,
  healthStatus: '',
  temperature: null,
  humidity: null,
  lightHours: null,
  notes: ''
})

const recordFormRules = {
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
  recoveryDays: [{ required: true, message: '请输入恢复天数', trigger: 'blur' }]
}

const reversedRecords = computed(() => {
  if (!recoveryDetail.value?.records) return []
  return [...recoveryDetail.value.records].reverse()
})

const recoveryProgressPercent = computed(() => {
  if (!currentTransplant.value) return 0
  const days = currentTransplant.value.daysSinceTransplant || 0
  if (days <= 7) {
    return (days / 7) * 33
  } else if (days <= 14) {
    return 33 + ((days - 7) / 7) * 34
  } else {
    return 100
  }
})

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

const openRecoveryRecord = async (item) => {
  currentTransplant.value = item
  recoveryDialogVisible.value = true
  await loadRecoveryDetail(item.id)
}

const loadRecoveryDetail = async (transplantId) => {
  recoveryDetailLoading.value = true
  try {
    const data = await getRecoveryDetail(transplantId)
    recoveryDetail.value = data
  } catch (e) {
    console.error('加载恢复详情失败', e)
  } finally {
    recoveryDetailLoading.value = false
  }
}

const handleRecoveryDialogClose = () => {
  recoveryDetail.value = null
  currentTransplant.value = null
}

const openAddRecord = () => {
  editRecordId.value = null
  const today = new Date().toISOString().split('T')[0]
  const daysSince = currentTransplant.value?.daysSinceTransplant || 0
  recordFormData.value = {
    transplantId: currentTransplant.value?.id,
    recordDate: today,
    recoveryDays: daysSince,
    leafStatus: '',
    leafCount: null,
    wateringDone: false,
    wateringAmount: null,
    healthStatus: '',
    temperature: null,
    humidity: null,
    lightHours: null,
    notes: ''
  }
  addRecordDialogVisible.value = true
}

const handleSubmitRecord = async () => {
  if (!recordFormRef.value) return
  try {
    await recordFormRef.value.validate()
    recordSubmitting.value = true

    const payload = { ...recordFormData.value }
    payload.transplantId = currentTransplant.value?.id

    if (editRecordId.value) {
      await updateRecoveryRecord(editRecordId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createRecoveryRecord(payload)
      ElMessage.success('添加成功')
    }

    addRecordDialogVisible.value = false
    await loadRecoveryDetail(currentTransplant.value?.id)
    loadData()
  } catch (e) {
    console.error(e)
  } finally {
    recordSubmitting.value = false
  }
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

.recovery-dialog {
  padding: 0 10px;
}

.recovery-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.variety-name {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 8px 0;
}

.recovery-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #606266;
  flex-wrap: wrap;
}

.recovery-stage-tag {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.recovery-stage-tag.stage-ACCLIMATING {
  background: #fdf6ec;
  color: #e6a23c;
}

.recovery-stage-tag.stage-RECOVERING {
  background: #ecf5ff;
  color: #409eff;
}

.recovery-stage-tag.stage-RECOVERED {
  background: #f0f9eb;
  color: #67c23a;
}

.recovery-progress-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 16px 0;
}

.progress-bar-wrapper {
  margin-bottom: 20px;
}

.progress-bar {
  height: 10px;
  background: #e4e7ed;
  border-radius: 5px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #e6a23c 0%, #67c23a 100%);
  border-radius: 5px;
  transition: width 0.5s ease;
}

.progress-labels {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.recovery-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  background: #fafbfc;
  border-radius: 8px;
  text-align: center;
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.stat-value.danger {
  color: #f56c6c;
}

.recovery-records-section {
  margin-top: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header .section-title {
  margin: 0;
}

.recovery-records-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
  padding-right: 4px;
}

.recovery-record-item {
  display: flex;
  gap: 16px;
  padding: 14px;
  background: #fafbfc;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.record-date-badge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 70px;
  padding: 8px 10px;
  background: linear-gradient(135deg, #67c23a 0%, #95d475 100%);
  border-radius: 8px;
  color: white;
}

.date-text {
  font-size: 15px;
  font-weight: 700;
}

.date-sub {
  font-size: 11px;
  opacity: 0.9;
}

.record-content {
  flex: 1;
  min-width: 0;
}

.record-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 10px;
}

.record-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.record-info-label {
  font-size: 11px;
  color: #909399;
}

.record-info-value {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
}

.record-health-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #606266;
}

.record-health {
  color: #67c23a;
  font-weight: 500;
}

.record-env {
  color: #909399;
}

.record-notes {
  font-size: 12px;
  color: #606266;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
}
</style>
