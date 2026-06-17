<template>
  <div class="transplant-page">
    <div class="page-header">
      <h2 class="page-title">🪴 移栽分盆记录</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增移栽
      </el-button>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="transplantList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="varietyName" label="花卉品种" min-width="120" />
        <el-table-column label="移栽时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.transplantTime) }}</template>
        </el-table-column>
        <el-table-column prop="potSpecification" label="花盆规格" width="120" />
        <el-table-column prop="soilRatio" label="盆土配比" min-width="150" />
        <el-table-column prop="transplantQuantity" label="移栽数量" width="100">
          <template #default="{ row }">{{ row.transplantQuantity }} 株</template>
        </el-table-column>
        <el-table-column prop="cumulativeQuantity" label="累计分盆" width="100">
          <template #default="{ row }">
            <span class="cumulative-badge">{{ row.cumulativeQuantity ?? '-' }} 株</span>
          </template>
        </el-table-column>
        <el-table-column prop="lightRequirement" label="光照要求" width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">
              详情
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="transplantList.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🪴</div>
        <div class="empty-text">暂无移栽记录，点击上方按钮新增</div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑移栽记录' : '新增移栽'"
      width="650px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item label="选择播种" prop="sowingId">
          <el-select v-model="formData.sowingId" placeholder="请选择播种记录" style="width: 100%" @change="handleSowingChange" filterable>
            <el-option
              v-for="s in sowingList"
              :key="s.id"
              :label="`${s.varietyName} - ${formatDate(s.sowingTime)}`"
              :value="s.id"
              :disabled="!isSowingEligible(s.id)"
            >
              <span>{{ s.varietyName }} - {{ formatDate(s.sowingTime) }}</span>
              <span v-if="sowingEligibilityMap[s.id]" style="float: right; font-size: 12px;"
                :class="sowingEligibilityMap[s.id].eligible ? 'eligible-tag' : 'ineligible-tag'">
                {{ sowingEligibilityMap[s.id].eligible ? '✅可移栽' : `❌${sowingEligibilityMap[s.id].stageName || '未跟踪'}` }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <div v-if="formData.sowingId && !isSowingEligible(formData.sowingId)" class="ineligible-warning">
          ⚠️ 该播种批次当前处于【{{ sowingEligibilityMap[formData.sowingId]?.stageName || '未跟踪' }}】阶段，尚未达到移栽条件，需至少进入【成苗期】
        </div>
        <div v-if="formData.sowingId && selectedSowingInfo" class="sowing-quantity-hint">
          <span>🌾 播种数量：<strong>{{ selectedSowingInfo.sowingQuantity }}</strong> 粒</span>
          <span v-if="currentEstimatedSurvival != null" class="survival-hint">
            🌿 存活估算：<strong>{{ currentEstimatedSurvival }}</strong> 株
          </span>
          <span class="transplanted-hint">
            🪴 已移栽：<strong>{{ currentTransplantedCount }}</strong> 株
          </span>
          <span class="remaining-hint">
            ✅ 可移栽：<strong>{{ maxAllowedQuantity - currentTransplantedCount }}</strong> 株
          </span>
        </div>
        <el-form-item label="移栽时间" prop="transplantTime">
          <el-date-picker
            v-model="formData.transplantTime"
            type="datetime"
            placeholder="选择移栽时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="花盆规格" prop="potSpecification">
          <el-input v-model="formData.potSpecification" placeholder="如：12cm 红陶盆、加仑盆" />
        </el-form-item>
        <el-form-item label="移栽数量" prop="transplantQuantity">
          <el-input-number v-model="formData.transplantQuantity" :min="1" :max="maxAllowedQuantity - currentTransplantedCount" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">株</span>
          <span v-if="formData.sowingId" style="margin-left: 10px; color: #e6a23c; font-size: 12px;">
            （上限 {{ maxAllowedQuantity - currentTransplantedCount }} 株）
          </span>
        </el-form-item>
        <el-form-item label="盆土配比" prop="soilRatio">
          <el-input v-model="formData.soilRatio" placeholder="如：营养土:珍珠岩:蛭石=4:1:1" />
        </el-form-item>
        <el-form-item label="缓苗养护要点" prop="recoveryTips">
          <el-input v-model="formData.recoveryTips" type="textarea" :rows="3" placeholder="缓苗期间的养护注意事项" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="光照要求">
              <el-input v-model="formData.lightRequirement" placeholder="如：散射光" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="浇水频率">
              <el-input v-model="formData.wateringFrequency" placeholder="如：见干见湿" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="施肥计划">
              <el-input v-model="formData.fertilizationPlan" placeholder="如：一周后施薄肥" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="formData.notes" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="移栽详情" width="700px">
      <div v-if="detailData" class="detail-content">
        <div class="detail-section">
          <h4 class="detail-section-title">📋 移栽基本信息</h4>
          <div class="detail-item">
            <span class="detail-label">花卉品种：</span>
            <span class="detail-value">{{ detailData.varietyName }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">移栽时间：</span>
            <span class="detail-value">{{ formatDateTime(detailData.transplantTime) }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">花盆规格：</span>
            <span class="detail-value">{{ detailData.potSpecification }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">移栽数量：</span>
            <span class="detail-value highlight-quantity">{{ detailData.transplantQuantity }} 株</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">累计分盆：</span>
            <span class="detail-value">
              <span class="cumulative-info">{{ detailData.previousCumulativeQuantity ?? 0 }}</span>
              <span class="cumulative-arrow"> → </span>
              <span class="cumulative-current">{{ detailData.cumulativeQuantity ?? detailData.transplantQuantity }} 株</span>
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">播种数量：</span>
            <span class="detail-value">{{ detailData.sowingQuantity ?? '-' }} 粒</span>
            <span v-if="detailData.estimatedSurvival != null" class="detail-survival">
              （存活估算：{{ detailData.estimatedSurvival }} 株）
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">盆土配比：</span>
            <span class="detail-value">{{ detailData.soilRatio || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">光照要求：</span>
            <span class="detail-value">{{ detailData.lightRequirement || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">浇水频率：</span>
            <span class="detail-value">{{ detailData.wateringFrequency || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">施肥计划：</span>
            <span class="detail-value">{{ detailData.fertilizationPlan || '-' }}</span>
          </div>
        </div>

        <div v-if="detailData.lastGrowthBeforeTransplant" class="detail-section contrast-section">
          <h4 class="detail-section-title">🌱 移栽前幼苗状态（对照）</h4>
          <div class="contrast-card growth-before">
            <div class="contrast-header">
              <span class="contrast-badge before">移栽前</span>
              <span class="contrast-time">{{ formatDateTime(detailData.lastGrowthBeforeTransplant.recordTime) }}</span>
            </div>
            <div class="contrast-body">
              <div class="contrast-metrics">
                <span v-if="detailData.lastGrowthBeforeTransplant.stageName" class="contrast-metric">
                  🏷️ {{ detailData.lastGrowthBeforeTransplant.stageName }}
                </span>
                <span v-if="detailData.lastGrowthBeforeTransplant.plantHeight" class="contrast-metric">
                  📏 {{ detailData.lastGrowthBeforeTransplant.plantHeight }} cm
                </span>
                <span v-if="detailData.lastGrowthBeforeTransplant.leafCount" class="contrast-metric">
                  🍃 {{ detailData.lastGrowthBeforeTransplant.leafCount }} 片叶
                </span>
                <span v-if="detailData.lastGrowthBeforeTransplant.rootDevelopment" class="contrast-metric">
                  🌳 {{ detailData.lastGrowthBeforeTransplant.rootDevelopment }}
                </span>
                <span v-if="detailData.lastGrowthBeforeTransplant.healthStatus" class="contrast-metric health">
                  💚 {{ detailData.lastGrowthBeforeTransplant.healthStatus }}
                </span>
                <span v-if="detailData.lastGrowthBeforeTransplant.estimatedSurvival" class="contrast-metric survival">
                  🌿 存活约 {{ detailData.lastGrowthBeforeTransplant.estimatedSurvival }} 株
                </span>
              </div>
              <div class="contrast-env" v-if="detailData.lastGrowthBeforeTransplant.temperature || detailData.lastGrowthBeforeTransplant.humidity || detailData.lastGrowthBeforeTransplant.lightHours">
                <span v-if="detailData.lastGrowthBeforeTransplant.temperature">🌡️ {{ detailData.lastGrowthBeforeTransplant.temperature }}°C</span>
                <span v-if="detailData.lastGrowthBeforeTransplant.humidity">💧 {{ detailData.lastGrowthBeforeTransplant.humidity }}%</span>
                <span v-if="detailData.lastGrowthBeforeTransplant.lightHours">☀️ {{ detailData.lastGrowthBeforeTransplant.lightHours }}h</span>
              </div>
              <div v-if="detailData.lastGrowthBeforeTransplant.notes" class="contrast-notes">
                {{ detailData.lastGrowthBeforeTransplant.notes }}
              </div>
            </div>
          </div>

          <div class="contrast-card transplant-result">
            <div class="contrast-header">
              <span class="contrast-badge after">分盆结果</span>
              <span class="contrast-time">{{ formatDateTime(detailData.transplantTime) }}</span>
            </div>
            <div class="contrast-body">
              <div class="contrast-metrics">
                <span class="contrast-metric quantity">🪴 移栽 {{ detailData.transplantQuantity }} 株</span>
                <span class="contrast-metric cumulative">📊 累计 {{ detailData.cumulativeQuantity ?? detailData.transplantQuantity }} 株</span>
                <span v-if="detailData.sowingQuantity" class="contrast-metric">
                  🌾 播种 {{ detailData.sowingQuantity }} 粒
                </span>
              </div>
              <div class="contrast-progress" v-if="detailData.sowingQuantity">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: Math.min(100, ((detailData.cumulativeQuantity ?? detailData.transplantQuantity) / detailData.sowingQuantity * 100)) + '%' }"></div>
                </div>
                <span class="progress-text">{{ Math.min(100, Math.round((detailData.cumulativeQuantity ?? detailData.transplantQuantity) / detailData.sowingQuantity * 100)) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <div v-if="detailData.recoveryTips" class="detail-section recovery-section">
          <h4 class="detail-section-title">💡 缓苗恢复提示</h4>
          <div class="recovery-card">
            <div class="recovery-content">{{ detailData.recoveryTips }}</div>
          </div>
        </div>

        <div v-if="detailData.notes" class="detail-section">
          <div class="detail-item">
            <span class="detail-label">备注：</span>
            <span class="detail-value">{{ detailData.notes }}</span>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTransplantList,
  getTransplantDetail,
  getTransplantsBySowing,
  createTransplant,
  updateTransplant,
  deleteTransplant
} from '@/api/transplant'
import { getSowingList } from '@/api/sowing'
import { getGrowthTrackings } from '@/api/growth'
import { getStageList } from '@/api/stage'
import { formatDate, formatDateTime, getCurrentLocalDateTime } from '@/utils/date'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const transplantList = ref([])
const sowingList = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const detailData = ref(null)
const sowingTransplantCounts = ref({})
const sowingSurvivalMap = ref({})
const sowingEligibilityMap = ref({})
const stageList = ref([])



const formData = reactive({
  sowingId: null,
  varietyId: null,
  varietyName: '',
  transplantTime: '',
  potSpecification: '',
  soilRatio: '',
  transplantQuantity: 1,
  recoveryTips: '',
  lightRequirement: '',
  wateringFrequency: '',
  fertilizationPlan: '',
  notes: ''
})

const formRules = {
  sowingId: [{ required: true, message: '请选择播种记录', trigger: 'change' }],
  transplantTime: [{ required: true, message: '请选择移栽时间', trigger: 'change' }],
  potSpecification: [{ required: true, message: '请输入花盆规格', trigger: 'blur' }],
  transplantQuantity: [{ required: true, message: '请输入移栽数量', trigger: 'blur' }]
}

const selectedSowingInfo = computed(() => {
  if (!formData.sowingId) return null
  return sowingList.value.find(s => s.id === formData.sowingId)
})

const currentTransplantedCount = computed(() => {
  if (!formData.sowingId) return 0
  let count = sowingTransplantCounts.value[formData.sowingId] || 0
  if (editId.value) {
    const existingRecord = transplantList.value.find(r => r.id === editId.value)
    if (existingRecord && existingRecord.sowingId === formData.sowingId) {
      count -= existingRecord.transplantQuantity
    }
  }
  return Math.max(0, count)
})

const currentEstimatedSurvival = computed(() => {
  if (!formData.sowingId) return null
  return sowingSurvivalMap.value[formData.sowingId] ?? null
})

const maxAllowedQuantity = computed(() => {
  if (!selectedSowingInfo.value) return 9999
  let max = selectedSowingInfo.value.sowingQuantity
  if (currentEstimatedSurvival.value != null && currentEstimatedSurvival.value > 0) {
    max = Math.min(max, currentEstimatedSurvival.value)
  }
  return max
})

const isSowingEligible = (sowingId) => {
  if (!sowingId) return false
  const info = sowingEligibilityMap.value[sowingId]
  return info?.eligible === true
}

const loadSowingEligibility = async () => {
  const SEEDLING_CODE = 'SEEDLING'
  const seedlingOrder = stageList.value.find(s => s.stageCode === SEEDLING_CODE)?.stageOrder ?? 5

  for (const sowing of sowingList.value) {
    try {
      const trackings = await getGrowthTrackings(sowing.id)
      if (!trackings || trackings.length === 0) {
        sowingEligibilityMap.value[sowing.id] = { eligible: false, stageName: '未跟踪' }
        continue
      }
      const latest = trackings[trackings.length - 1]
      const latestOrder = stageList.value.find(s => s.stageCode === latest.stageCode)?.stageOrder ?? 0
      sowingEligibilityMap.value[sowing.id] = {
        eligible: latestOrder >= seedlingOrder,
        stageName: latest.stageName || '未知'
      }
      if (latest.estimatedSurvival != null && latest.estimatedSurvival > 0) {
        sowingSurvivalMap.value[sowing.id] = latest.estimatedSurvival
      }
    } catch (e) {
      sowingEligibilityMap.value[sowing.id] = { eligible: false, stageName: '加载失败' }
    }
  }
}

const handleSowingChange = async (val) => {
  const sowing = sowingList.value.find(s => s.id === val)
  if (sowing) {
    formData.varietyId = sowing.varietyId
    formData.varietyName = sowing.varietyName
  }
  await loadSowingTransplantInfo(val)
}

const loadSowingTransplantInfo = async (sowingId) => {
  try {
    const transplants = await getTransplantsBySowing(sowingId)
    sowingTransplantCounts.value[sowingId] = (transplants || []).reduce(
      (sum, t) => sum + (t.transplantQuantity || 0), 0
    )
  } catch (e) {
    console.error(e)
  }

  try {
    const trackings = await getGrowthTrackings(sowingId)
    if (trackings && trackings.length > 0) {
      const latest = trackings[trackings.length - 1]
      if (latest.estimatedSurvival != null && latest.estimatedSurvival > 0) {
        sowingSurvivalMap.value[sowingId] = latest.estimatedSurvival
      } else {
        sowingSurvivalMap.value[sowingId] = null
      }
    }
  } catch (e) {
    console.error(e)
  }
}

const loadTransplants = async () => {
  loading.value = true
  try {
    const data = await getTransplantList()
    transplantList.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadSowings = async () => {
  try {
    const data = await getSowingList()
    sowingList.value = data || []
    await loadSowingEligibility()
  } catch (e) {
    console.error(e)
  }
}

const handleAdd = () => {
  editId.value = null
  sowingTransplantCounts.value = {}
  sowingSurvivalMap.value = {}
  Object.assign(formData, {
    sowingId: null,
    varietyId: null,
    varietyName: '',
    transplantTime: getCurrentLocalDateTime(),
    potSpecification: '',
    soilRatio: '',
    transplantQuantity: 1,
    recoveryTips: '',
    lightRequirement: '',
    wateringFrequency: '',
    fertilizationPlan: '',
    notes: ''
  })
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  editId.value = row.id
  Object.assign(formData, { ...row })
  if (row.sowingId) {
    await loadSowingTransplantInfo(row.sowingId)
  }
  dialogVisible.value = true
}

const handleDetail = async (row) => {
  try {
    const data = await getTransplantDetail(row.id)
    detailData.value = data
    detailVisible.value = true
  } catch (e) {
    console.error(e)
    detailData.value = row
    detailVisible.value = true
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.varietyName}」的移栽记录吗？`, '提示', {
      type: 'warning'
    })
    await deleteTransplant(row.id)
    ElMessage.success('删除成功')
    loadTransplants()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()

    if (formData.sowingId && !isSowingEligible(formData.sowingId)) {
      const stageName = sowingEligibilityMap.value[formData.sowingId]?.stageName || '未跟踪'
      ElMessage.warning(`该播种批次当前处于【${stageName}】阶段，尚未达到移栽条件，需至少进入【成苗期】`)
      return
    }

    if (formData.transplantQuantity + currentTransplantedCount.value > maxAllowedQuantity.value) {
      ElMessage.warning(`移栽数量超出上限，当前最多可移栽 ${maxAllowedQuantity.value - currentTransplantedCount.value} 株`)
      return
    }

    submitting.value = true

    if (editId.value) {
      await updateTransplant(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createTransplant(formData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadTransplants()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadTransplants()
  try {
    stageList.value = await getStageList()
  } catch (e) {
    console.error(e)
  }
  loadSowings()

  if (route.query.id) {
    const targetId = Number(route.query.id)
    const row = transplantList.value.find(r => r.id === targetId)
    if (row) {
      handleDetail(row)
    } else {
      try {
        const data = await getTransplantDetail(targetId)
        detailData.value = data
        detailVisible.value = true
      } catch (e) {
        console.error(e)
      }
    }
  }
})
</script>

<style scoped>
.ineligible-warning {
  padding: 10px 14px;
  margin: 0 0 12px 110px;
  background: #fef0f0;
  border: 1px solid #fab6b6;
  border-radius: 6px;
  color: #f56c6c;
  font-size: 13px;
  line-height: 1.6;
}

.eligible-tag {
  color: #67c23a;
  font-weight: 600;
}

.ineligible-tag {
  color: #f56c6c;
  font-weight: 600;
}

.cumulative-badge {
  font-size: 12px;
  padding: 2px 8px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
}

.sowing-quantity-hint {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  padding: 10px 14px;
  margin: 0 0 16px 110px;
  background: #f5f7fa;
  border-radius: 6px;
  font-size: 13px;
  color: #606266;
}

.sowing-quantity-hint strong {
  color: #303133;
}

.survival-hint strong {
  color: #67c23a;
}

.transplanted-hint strong {
  color: #e6a23c;
}

.remaining-hint strong {
  color: #409eff;
}

.detail-content {
  padding: 10px 0;
}

.detail-section {
  margin-bottom: 16px;
}

.detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.detail-item {
  display: flex;
  align-items: baseline;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f0f0;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  width: 100px;
  color: #909399;
  flex-shrink: 0;
}

.detail-value {
  color: #303133;
  flex: 1;
}

.highlight-quantity {
  font-weight: 600;
  color: #e6a23c;
  font-size: 16px;
}

.detail-survival {
  margin-left: 8px;
  font-size: 12px;
  color: #67c23a;
}

.cumulative-info {
  color: #909399;
  font-size: 13px;
}

.cumulative-arrow {
  color: #c0c4cc;
  margin: 0 4px;
}

.cumulative-current {
  font-weight: 600;
  color: #409eff;
}

.contrast-section {
  background: #fafbfc;
  border-radius: 8px;
  padding: 14px;
}

.contrast-card {
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
}

.contrast-card:last-child {
  margin-bottom: 0;
}

.growth-before {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
}

.transplant-result {
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.contrast-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.contrast-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 10px;
  border-radius: 10px;
}

.contrast-badge.before {
  background: #67c23a;
  color: white;
}

.contrast-badge.after {
  background: #409eff;
  color: white;
}

.contrast-time {
  font-size: 12px;
  color: #909399;
}

.contrast-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.contrast-metric {
  font-size: 12px;
  padding: 3px 8px;
  background: white;
  border-radius: 4px;
  color: #606266;
}

.contrast-metric.health {
  color: #67c23a;
  background: white;
}

.contrast-metric.survival {
  color: #e6a23c;
  background: #fef9e7;
}

.contrast-metric.quantity {
  color: #409eff;
  font-weight: 600;
}

.contrast-metric.cumulative {
  color: #5ac8fa;
}

.contrast-env {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.contrast-notes {
  font-size: 12px;
  color: #606266;
  padding: 6px 10px;
  background: white;
  border-radius: 4px;
  margin-top: 6px;
}

.contrast-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: #e4e7ed;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #409eff, #67c23a);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
  flex-shrink: 0;
}

.recovery-section {
  background: #fdf6ec;
  border-radius: 8px;
  padding: 14px;
}

.recovery-section .detail-section-title {
  border-bottom-color: #f5dab1;
}

.recovery-card {
  background: white;
  border-radius: 6px;
  padding: 12px;
  border-left: 3px solid #e6a23c;
}

.recovery-content {
  color: #606266;
  line-height: 1.7;
  font-size: 13px;
}
</style>
