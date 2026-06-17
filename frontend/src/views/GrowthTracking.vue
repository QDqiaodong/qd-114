<template>
  <div class="growth-page">
    <div class="page-header">
      <h2 class="page-title">🌿 生长阶段跟踪</h2>
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
                <span v-if="getVarietyBySowing(s).seedlingDays" class="sowing-option-days">🌿 育苗{{ getVarietyBySowing(s).seedlingDays }}天</span>
              </div>
            </div>
          </el-option>
        </el-select>
        <el-button type="primary" @click="handleAdd" :disabled="!selectedSowingId">
          <el-icon><Plus /></el-icon>
          添加记录
        </el-button>
      </div>
    </div>

    <div v-if="selectedSowing" class="card sowing-info-card">
      <div class="sowing-basic">
        <h3>🌱 {{ selectedSowing.varietyName }}</h3>
        <div class="sowing-meta">
          <span>📅 播种: {{ formatDate(selectedSowing.sowingTime) }}</span>
          <span>🌾 {{ selectedSowing.sowingQuantity }} 粒</span>
          <span>🪴 {{ selectedSowing.soilRatio }}</span>
        </div>
      </div>
    </div>

    <div class="card">
      <h4 class="section-title">📊 阶段时间线</h4>
      <div class="stage-tags">
        <span
          v-for="stage in stages"
          :key="stage.id"
          class="stage-tag"
          :class="[getStageStatus(stage.stageCode), { 'is-active': selectedStageCode === stage.stageCode }]"
          @click="filterByStage(stage.stageCode)"
        >
          {{ stage.stageName }}
        </span>
      </div>
    </div>

    <div class="card">
      <h4 class="section-title">📝 生长记录</h4>
      <div v-if="filteredTrackings.length > 0" class="timeline">
        <div v-for="(item, index) in filteredTrackings" :key="item.id" class="timeline-item" :class="getTimelineClass(index)">
          <div class="timeline-time">{{ formatDateTime(item.recordTime) }}</div>
          <div class="timeline-title">
            <el-tag :type="getTagType(item.stageCode)" size="small">
              {{ item.stageName }}
            </el-tag>
          </div>
          <div class="timeline-content">
            <div class="tracking-metrics">
              <span v-if="item.plantHeight" class="metric">📏 {{ item.plantHeight }} cm</span>
              <span v-if="item.leafCount" class="metric">🍃 {{ item.leafCount }} 片叶</span>
              <span v-if="item.rootDevelopment" class="metric">🌳 {{ item.rootDevelopment }}</span>
              <span v-if="item.healthStatus" class="metric">💚 {{ item.healthStatus }}</span>
              <span v-if="item.estimatedSurvival" class="metric survival">🌿 存活约 {{ item.estimatedSurvival }} 株</span>
            </div>
            <div class="tracking-env" v-if="item.temperature || item.humidity || item.lightHours">
              <span v-if="item.temperature">🌡️ {{ item.temperature }}°C</span>
              <span v-if="item.humidity">💧 {{ item.humidity }}%</span>
              <span v-if="item.lightHours">☀️ {{ item.lightHours }}h</span>
              <span v-if="item.fertilization">🧪 {{ item.fertilization }}</span>
            </div>
            <div v-if="item.notes" class="tracking-notes">{{ item.notes }}</div>
            <div class="tracking-actions">
              <el-button type="primary" link size="small" @click="handleEdit(item)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(item)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="filteredTrackings.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🌿</div>
        <div class="empty-text">
          {{ selectedStageCode ? '该阶段暂无生长记录' : (selectedSowingId ? '暂无生长记录，点击上方按钮添加' : '请先选择播种记录') }}
        </div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑生长记录' : '添加生长记录'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="生长阶段" prop="stageCode">
          <el-select v-model="formData.stageCode" placeholder="请选择阶段" style="width: 100%" @change="handleStageChange">
            <el-option
              v-for="stage in stages"
              :key="stage.id"
              :label="stage.stageName"
              :value="stage.stageCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="记录时间" prop="recordTime">
          <el-date-picker
            v-model="formData.recordTime"
            type="datetime"
            placeholder="选择记录时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="株高">
              <el-input-number v-model="formData.plantHeight" :min="0" :step="0.1" :precision="2" />
              <span style="margin-left: 8px; font-size: 12px; color: #909399;">cm</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="叶片数">
              <el-input-number v-model="formData.leafCount" :min="0" :step="1" />
              <span style="margin-left: 8px; font-size: 12px; color: #909399;">片</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="根系发育">
          <el-input v-model="formData.rootDevelopment" placeholder="如：根系发达、须根茂密" />
        </el-form-item>
        <el-form-item label="健康状态">
          <el-input v-model="formData.healthStatus" placeholder="如：良好、徒长、有病虫害" />
        </el-form-item>
        <el-form-item label="存活估算">
          <el-input-number v-model="formData.estimatedSurvival" :min="0" :step="1" />
          <span style="margin-left: 8px; font-size: 12px; color: #909399;">株（当前观察到的存活苗数）</span>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="温度">
              <el-input-number v-model="formData.temperature" :step="0.1" :precision="1" />
              <span style="font-size: 12px; color: #909399;">°C</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="湿度">
              <el-input-number v-model="formData.humidity" :min="0" :max="100" :step="1" />
              <span style="font-size: 12px; color: #909399;">%</span>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="光照">
              <el-input-number v-model="formData.lightHours" :min="0" :step="0.5" :precision="1" />
              <span style="font-size: 12px; color: #909399;">h</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="施肥情况">
          <el-input v-model="formData.fertilization" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.notes" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getSowingList } from '@/api/sowing'
import { getStageList } from '@/api/stage'
import { getVarietyList } from '@/api/variety'
import {
  getGrowthTrackings,
  createGrowthTracking,
  updateGrowthTracking,
  deleteGrowthTracking
} from '@/api/growth'
import { formatDate, formatDateTime, getCurrentLocalDateTime } from '@/utils/date'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const sowingList = ref([])
const stages = ref([])
const varietyList = ref([])
const trackings = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const selectedSowingId = ref(null)
const selectedStageCode = ref('')

const selectedSowing = computed(() => {
  return sowingList.value.find(s => s.id === selectedSowingId.value)
})

const filteredTrackings = computed(() => {
  if (!selectedStageCode.value) return trackings.value
  return trackings.value.filter(t => t.stageCode === selectedStageCode.value)
})

const formData = reactive({
  sowingId: null,
  stageCode: '',
  stageName: '',
  recordTime: '',
  plantHeight: null,
  leafCount: null,
  rootDevelopment: '',
  healthStatus: '',
  estimatedSurvival: null,
  temperature: null,
  humidity: null,
  lightHours: null,
  fertilization: '',
  notes: ''
})

const formRules = {
  stageCode: [{ required: true, message: '请选择生长阶段', trigger: 'change' }],
  recordTime: [{ required: true, message: '请选择记录时间', trigger: 'change' }]
}



const handleStageChange = (code) => {
  const stage = stages.value.find(s => s.stageCode === code)
  if (stage) {
    formData.stageName = stage.stageName
  }
}

const getTagType = (stageCode) => {
  const stage = stages.value.find(s => s.stageCode === stageCode)
  if (!stage) return 'info'
  if (stage.stageOrder <= 2) return 'success'
  if (stage.stageOrder <= 5) return 'warning'
  return 'primary'
}

const getStageStatus = (stageCode) => {
  const hasStage = trackings.value.some(t => t.stageCode === stageCode)
  return hasStage ? 'is-done' : 'is-pending'
}

const getTimelineClass = (index) => {
  if (index === trackings.value.length - 1) return 'is-current'
  return 'is-done'
}

const filterByStage = (stageCode) => {
  if (selectedStageCode.value === stageCode) {
    selectedStageCode.value = ''
  } else {
    selectedStageCode.value = stageCode
  }
}

const loadSowings = async () => {
  try {
    const data = await getSowingList()
    sowingList.value = data || []
  } catch (e) {
    console.error(e)
  }
}

const loadStages = async () => {
  try {
    const data = await getStageList()
    stages.value = data || []
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

const getVarietyBySowing = (sowing) => {
  return varietyList.value.find(v => v.id === sowing.varietyId) || {}
}

const loadTrackings = async () => {
  if (!selectedSowingId.value) {
    trackings.value = []
    return
  }
  loading.value = true
  try {
    const data = await getGrowthTrackings(selectedSowingId.value)
    trackings.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSowingChange = () => {
  selectedStageCode.value = ''
  loadTrackings()
}

const handleAdd = () => {
  editId.value = null
  Object.assign(formData, {
    sowingId: selectedSowingId.value,
    stageCode: '',
    stageName: '',
    recordTime: getCurrentLocalDateTime(),
    plantHeight: null,
    leafCount: null,
    rootDevelopment: '',
    healthStatus: '',
    estimatedSurvival: null,
    temperature: null,
    humidity: null,
    lightHours: null,
    fertilization: '',
    notes: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除这条生长记录吗？`, '提示', {
      type: 'warning'
    })
    await deleteGrowthTracking(row.id)
    ElMessage.success('删除成功')
    loadTrackings()
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
    submitting.value = true

    if (editId.value) {
      await updateGrowthTracking(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      formData.sowingId = selectedSowingId.value
      await createGrowthTracking(formData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadTrackings()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadSowings(), loadStages(), loadVarieties()])
  if (route.query.sowingId) {
    selectedSowingId.value = Number(route.query.sowingId)
    loadTrackings()
  }
})
</script>

<style scoped>
.sowing-info-card {
  margin-bottom: 20px;
}

.sowing-basic h3 {
  font-size: 18px;
  color: #303133;
  margin-bottom: 8px;
}

.sowing-meta {
  display: flex;
  gap: 20px;
  color: #606266;
  font-size: 14px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.stage-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.stage-tag {
  padding: 8px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
  color: #909399;

  &:hover {
    transform: translateY(-1px);
  }

  &.is-done {
    background: #f0f9eb;
    border-color: #c2e7b0;
    color: #67c23a;
  }

  &.is-active {
    background: #409eff;
    border-color: #409eff;
    color: #fff;
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4);
  }
}

.tracking-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;

  .metric {
    padding: 2px 8px;
    background: #ecf5ff;
    border-radius: 4px;
    font-size: 12px;
    color: #409eff;

    &.survival {
      background: #f0f9eb;
      color: #67c23a;
    }
  }
}

.tracking-env {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.tracking-notes {
  color: #606266;
  font-size: 13px;
  margin-top: 6px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.tracking-actions {
  margin-top: 8px;
}

.sowing-option-content {
  line-height: 1.4;
}

.sowing-option-main {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
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
  flex-wrap: wrap;
}

.sowing-option-date,
.sowing-option-qty,
.sowing-option-days {
  font-size: 12px;
  color: #606266;
}
</style>
