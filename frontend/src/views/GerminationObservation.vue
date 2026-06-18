<template>
  <div class="germination-page">
    <div class="page-header">
      <h2 class="page-title">🌱 批次发芽观察</h2>
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
                <span class="sowing-option-qty">🌾 {{ s.sowingQuantity }}粒</span>
                <span class="sowing-option-date">📅 {{ formatDate(s.sowingTime) }}</span>
              </div>
            </div>
          </el-option>
        </el-select>
        <el-button type="primary" @click="handleAdd" :disabled="!selectedSowingId">
          <el-icon><Plus /></el-icon>
          新增观察
        </el-button>
      </div>
    </div>

    <div v-if="selectedSowing" class="card sowing-info-card">
      <div class="sowing-basic">
        <h3>🌱 {{ selectedSowing.varietyName }}</h3>
        <div class="sowing-meta">
          <span>📅 播种: {{ formatDate(selectedSowing.sowingTime) }}</span>
          <span>🌾 播种数量: {{ selectedSowing.sowingQuantity }} 粒</span>
          <span v-if="latestObservation" class="germination-rate-display">
            📊 当前发芽率:
            <strong :class="getRateClass(latestObservation.germinationRate)">
              {{ latestObservation.germinationRate }}%
            </strong>
          </span>
          <span v-if="selectedSowing.batchStatus" class="batch-status">
            <el-tag :type="getBatchStatusTagType(selectedSowing.batchStatus)" size="small">
              {{ getBatchStatusText(selectedSowing.batchStatus) }}
            </el-tag>
          </span>
        </div>
      </div>
    </div>

    <div v-if="observations.length >= 2" class="card">
      <h4 class="section-title">📈 发芽趋势曲线</h4>
      <div ref="chartRef" class="trend-chart"></div>
    </div>

    <div class="card">
      <h4 class="section-title">📝 观察明细</h4>
      <el-table
        v-loading="loading"
        :data="observations"
        stripe
        style="width: 100%"
      >
        <el-table-column label="观察日期" width="120">
          <template #default="{ row }">{{ row.observationDate }}</template>
        </el-table-column>
        <el-table-column label="发芽数量" width="110">
          <template #default="{ row }">
            <span class="count-germinated">{{ row.germinatedCount }} 粒</span>
          </template>
        </el-table-column>
        <el-table-column label="未发芽数量" width="110">
          <template #default="{ row }">
            <span class="count-not-germinated">{{ row.notGerminatedCount }} 粒</span>
          </template>
        </el-table-column>
        <el-table-column label="发芽率" width="100">
          <template #default="{ row }">
            <el-tag :type="getRateTagType(row.germinationRate)" size="small">
              {{ row.germinationRate }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="异常说明" min-width="200">
          <template #default="{ row }">
            <span v-if="row.anomalyNote">{{ row.anomalyNote }}</span>
            <span v-else style="color: #c0c4cc;">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="observations.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🌱</div>
        <div class="empty-text">
          {{ selectedSowingId ? '暂无观察记录，点击上方按钮新增' : '请先选择播种记录' }}
        </div>
      </div>
    </div>

    <div v-if="selectedSowingId" class="card">
      <div class="section-header">
        <h4 class="section-title">⚠️ 发芽异常处理</h4>
        <el-tag v-if="anomalies.length > 0" type="danger" size="small">
          {{ anomalies.length }} 条异常
        </el-tag>
      </div>

      <div v-if="anomalies.length > 0" class="anomaly-list">
        <div
          v-for="anomaly in anomalies"
          :key="anomaly.id"
          class="anomaly-item"
          :class="'level-' + anomaly.anomalyLevel"
        >
          <div class="anomaly-header">
            <div class="anomaly-title">
              <span class="anomaly-level-tag" :class="'tag-' + anomaly.anomalyLevel">
                {{ getAnomalyLevelText(anomaly.anomalyLevel) }}
              </span>
              <span class="anomaly-status-tag" :class="'status-' + anomaly.status">
                {{ getAnomalyStatusText(anomaly.status) }}
              </span>
            </div>
            <span class="anomaly-date">{{ formatDateTime(anomaly.createTime) }}</span>
          </div>
          <div class="anomaly-body">
            <div class="anomaly-info-grid">
              <div class="anomaly-info-item">
                <span class="anomaly-info-label">基准发芽率</span>
                <span class="anomaly-info-value">{{ anomaly.baselineGerminationRate }}%</span>
              </div>
              <div class="anomaly-info-item">
                <span class="anomaly-info-label">实际发芽率</span>
                <span class="anomaly-info-value danger">{{ anomaly.actualGerminationRate }}%</span>
              </div>
              <div class="anomaly-info-item">
                <span class="anomaly-info-label">差值</span>
                <span class="anomaly-info-value danger">-{{ anomaly.rateDiff }}%</span>
              </div>
            </div>

            <div v-if="anomaly.actionTaken" class="anomaly-section">
              <div class="anomaly-section-title">📋 处理措施</div>
              <div class="anomaly-section-content">{{ anomaly.actionTaken }}</div>
            </div>

            <div v-if="anomaly.resultNote" class="anomaly-section">
              <div class="anomaly-section-title">✅ 处理结果</div>
              <div class="anomaly-section-content">{{ anomaly.resultNote }}</div>
            </div>

            <div v-if="anomaly.handler || anomaly.handleTime" class="anomaly-footer">
              <span v-if="anomaly.handler">处理人: {{ anomaly.handler }}</span>
              <span v-if="anomaly.handleTime">处理时间: {{ formatDateTime(anomaly.handleTime) }}</span>
            </div>
          </div>
          <div class="anomaly-actions">
            <el-button
              v-if="anomaly.status === 'PENDING'"
              type="primary"
              size="small"
              @click="handleProcessAnomaly(anomaly)"
            >
              开始处理
            </el-button>
            <el-button
              v-if="anomaly.status === 'PROCESSING'"
              type="success"
              size="small"
              @click="handleResolveAnomaly(anomaly)"
            >
              标记解决
            </el-button>
            <el-button
              v-if="anomaly.status !== 'CLOSED'"
              type="info"
              size="small"
              @click="handleCloseAnomaly(anomaly)"
            >
              关闭
            </el-button>
          </div>
        </div>
      </div>

      <div v-else class="empty-state small">
        <div class="empty-icon">✅</div>
        <div class="empty-text">暂无发芽异常记录</div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑观察记录' : '新增发芽观察'"
      width="560px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item label="观察日期" prop="observationDate">
          <el-date-picker
            v-model="formData.observationDate"
            type="date"
            placeholder="选择观察日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发芽数量" prop="germinatedCount">
              <el-input-number v-model="formData.germinatedCount" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="未发芽数量" prop="notGerminatedCount">
              <el-input-number v-model="formData.notGerminatedCount" :min="0" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="异常说明">
          <el-input v-model="formData.anomalyNote" type="textarea" :rows="3" placeholder="记录异常情况，如发霉、虫害等（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">
          确定
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="anomalyDialogVisible"
      :title="anomalyDialogType === 'process' ? '处理发芽异常' : '标记异常解决'"
      width="520px"
      destroy-on-close
    >
      <el-form
        ref="anomalyFormRef"
        :model="anomalyFormData"
        :rules="anomalyFormRules"
        label-width="90px"
      >
        <el-form-item label="处理人" prop="handler">
          <el-input v-model="anomalyFormData.handler" placeholder="请输入处理人姓名" />
        </el-form-item>
        <el-form-item
          v-if="anomalyDialogType === 'process'"
          label="处理措施"
          prop="actionTaken"
        >
          <el-input
            v-model="anomalyFormData.actionTaken"
            type="textarea"
            :rows="4"
            placeholder="请输入具体的处理措施，如调整温度、增加湿度、更换基质等"
          />
        </el-form-item>
        <el-form-item
          v-if="anomalyDialogType === 'resolve'"
          label="处理结果"
          prop="resultNote"
        >
          <el-input
            v-model="anomalyFormData.resultNote"
            type="textarea"
            :rows="4"
            placeholder="请输入处理结果说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="anomalyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAnomalySubmit" :loading="anomalySubmitting">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getSowingList } from '@/api/sowing'
import {
  getGerminationObservations,
  createGerminationObservation,
  updateGerminationObservation,
  deleteGerminationObservation,
  getAnomaliesBySowing,
  processAnomaly,
  resolveAnomaly,
  closeAnomaly
} from '@/api/germination'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const submitting = ref(false)
const sowingList = ref([])
const observations = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const selectedSowingId = ref(null)

const anomalies = ref([])
const anomalyDialogVisible = ref(false)
const anomalyDialogType = ref('process')
const anomalyFormRef = ref(null)
const anomalySubmitting = ref(false)
const currentAnomalyId = ref(null)

const anomalyFormData = reactive({
  handler: '',
  actionTaken: '',
  resultNote: ''
})

const anomalyFormRules = {
  handler: [{ required: true, message: '请输入处理人', trigger: 'blur' }]
}

const selectedSowing = computed(() => {
  return sowingList.value.find(s => s.id === selectedSowingId.value)
})

const latestObservation = computed(() => {
  if (observations.value.length === 0) return null
  return observations.value[observations.value.length - 1]
})

const chartRef = ref(null)
let chartInstance = null

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) {
    chartInstance.dispose()
  }
  chartInstance = echarts.init(chartRef.value)
}

const buildChartOption = () => {
  const data = [...observations.value]
  const dates = data.map(item => item.observationDate)
  const germinatedData = data.map(item => item.germinatedCount)
  const notGerminatedData = data.map(item => item.notGerminatedCount)
  const rateData = data.map(item => item.germinationRate != null ? Number(item.germinationRate) : null)

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter(params) {
        let html = `<div style="font-weight:600;margin-bottom:4px">${params[0].axisValue}</div>`
        params.forEach(p => {
          if (p.value != null) {
            const unit = p.seriesName === '发芽率' ? '%' : '粒'
            html += `<div style="display:flex;align-items:center;gap:4px">${p.marker}${p.seriesName}：<b>${p.value}</b> ${unit}</div>`
          }
        })
        return html
      }
    },
    legend: {
      data: ['发芽数量', '未发芽数量', '发芽率'],
      top: 0
    },
    grid: {
      left: 60,
      right: 60,
      bottom: 30,
      top: 40
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: {
        fontSize: 11,
        rotate: dates.length > 6 ? 30 : 0,
        formatter(val) {
          if (val && val.length > 10) return val.substring(5)
          return val
        }
      },
      boundaryGap: false
    },
    yAxis: [
      {
        type: 'value',
        name: '数量（粒）',
        nameTextStyle: { fontSize: 11, color: '#909399' },
        axisLabel: { fontSize: 11 },
        splitLine: { lineStyle: { type: 'dashed', color: '#ebeef5' } }
      },
      {
        type: 'value',
        name: '发芽率（%）',
        nameTextStyle: { fontSize: 11, color: '#909399' },
        axisLabel: { fontSize: 11 },
        splitLine: { show: false },
        min: 0,
        max: 100
      }
    ],
    series: [
      {
        name: '发芽数量',
        type: 'bar',
        yAxisIndex: 0,
        data: germinatedData,
        itemStyle: { color: '#67C23A' },
        barWidth: '30%'
      },
      {
        name: '未发芽数量',
        type: 'bar',
        yAxisIndex: 0,
        data: notGerminatedData,
        itemStyle: { color: '#F56C6C' },
        barWidth: '30%'
      },
      {
        name: '发芽率',
        type: 'line',
        yAxisIndex: 1,
        data: rateData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        lineStyle: { width: 3, color: '#409EFF' },
        itemStyle: { color: '#409EFF' },
        connectNulls: true
      }
    ]
  }
}

const updateChart = async () => {
  await nextTick()
  if (observations.value.length < 2) {
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
    return
  }
  if (!chartInstance) {
    initChart()
  }
  if (chartInstance) {
    chartInstance.setOption(buildChartOption(), true)
  }
}

const handleResize = () => {
  chartInstance && chartInstance.resize()
}

watch(observations, () => {
  updateChart()
})

window.addEventListener('resize', handleResize)

const formData = reactive({
  sowingId: null,
  observationDate: '',
  germinatedCount: 0,
  notGerminatedCount: 0,
  anomalyNote: ''
})

const formRules = {
  observationDate: [{ required: true, message: '请选择观察日期', trigger: 'change' }],
  germinatedCount: [{ required: true, message: '请输入发芽数量', trigger: 'blur' }],
  notGerminatedCount: [{ required: true, message: '请输入未发芽数量', trigger: 'blur' }]
}

const getRateTagType = (rate) => {
  if (rate == null) return 'info'
  const r = Number(rate)
  if (r >= 80) return 'success'
  if (r >= 50) return 'warning'
  return 'danger'
}

const getRateClass = (rate) => {
  if (rate == null) return ''
  const r = Number(rate)
  if (r >= 80) return 'rate-high'
  if (r >= 50) return 'rate-medium'
  return 'rate-low'
}

const getBatchStatusTagType = (status) => {
  switch (status) {
    case 'NORMAL': return 'success'
    case 'ABNORMAL': return 'danger'
    case 'RESOLVED': return 'warning'
    default: return 'info'
  }
}

const getBatchStatusText = (status) => {
  switch (status) {
    case 'NORMAL': return '正常'
    case 'ABNORMAL': return '异常'
    case 'RESOLVED': return '已恢复'
    default: return status
  }
}

const getAnomalyLevelText = (level) => {
  switch (level) {
    case 'MILD': return '轻度'
    case 'MODERATE': return '中度'
    case 'SEVERE': return '严重'
    default: return level
  }
}

const getAnomalyStatusText = (status) => {
  switch (status) {
    case 'PENDING': return '待处理'
    case 'PROCESSING': return '处理中'
    case 'RESOLVED': return '已解决'
    case 'CLOSED': return '已关闭'
    default: return status
  }
}

const formatDateTime = (datetime) => {
  if (!datetime) return ''
  const d = new Date(datetime)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const loadSowings = async () => {
  try {
    const data = await getSowingList()
    sowingList.value = data || []
  } catch (e) {
    console.error(e)
  }
}

const loadObservations = async () => {
  if (!selectedSowingId.value) {
    observations.value = []
    return
  }
  loading.value = true
  try {
    const data = await getGerminationObservations(selectedSowingId.value)
    observations.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const handleSowingChange = () => {
  loadObservations()
  loadAnomalies()
}

const loadAnomalies = async () => {
  if (!selectedSowingId.value) {
    anomalies.value = []
    return
  }
  try {
    const data = await getAnomaliesBySowing(selectedSowingId.value)
    anomalies.value = data || []
  } catch (e) {
    console.error(e)
  }
}

const handleProcessAnomaly = (anomaly) => {
  currentAnomalyId.value = anomaly.id
  anomalyDialogType.value = 'process'
  Object.assign(anomalyFormData, {
    handler: '',
    actionTaken: '',
    resultNote: ''
  })
  anomalyDialogVisible.value = true
}

const handleResolveAnomaly = (anomaly) => {
  currentAnomalyId.value = anomaly.id
  anomalyDialogType.value = 'resolve'
  Object.assign(anomalyFormData, {
    handler: '',
    actionTaken: '',
    resultNote: ''
  })
  anomalyDialogVisible.value = true
}

const handleCloseAnomaly = async (anomaly) => {
  try {
    await ElMessageBox.confirm('确定关闭这条异常记录吗？', '提示', {
      type: 'warning'
    })
    await closeAnomaly(anomaly.id)
    ElMessage.success('关闭成功')
    loadAnomalies()
    loadSowings()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleAnomalySubmit = async () => {
  if (!anomalyFormRef.value) return
  try {
    await anomalyFormRef.value.validate()
    anomalySubmitting.value = true

    if (anomalyDialogType.value === 'process') {
      await processAnomaly(currentAnomalyId.value, anomalyFormData)
      ElMessage.success('已开始处理')
    } else {
      await resolveAnomaly(currentAnomalyId.value, anomalyFormData)
      ElMessage.success('已标记解决')
    }

    anomalyDialogVisible.value = false
    loadAnomalies()
    loadSowings()
  } catch (e) {
    console.error(e)
  } finally {
    anomalySubmitting.value = false
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(formData, {
    sowingId: selectedSowingId.value,
    observationDate: '',
    germinatedCount: 0,
    notGerminatedCount: 0,
    anomalyNote: ''
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
    await ElMessageBox.confirm('确定删除这条观察记录吗？', '提示', {
      type: 'warning'
    })
    await deleteGerminationObservation(row.id)
    ElMessage.success('删除成功')
    loadObservations()
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
      await updateGerminationObservation(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      formData.sowingId = selectedSowingId.value
      await createGerminationObservation(formData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadObservations()
    loadAnomalies()
    loadSowings()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadSowings()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
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
  flex-wrap: wrap;
  align-items: center;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.trend-chart {
  width: 100%;
  height: 380px;
}

.count-germinated {
  color: #67c23a;
  font-weight: 500;
}

.count-not-germinated {
  color: #f56c6c;
  font-weight: 500;
}

.rate-high {
  color: #67c23a;
}

.rate-medium {
  color: #e6a23c;
}

.rate-low {
  color: #f56c6c;
}

.sowing-option-content {
  line-height: 1.4;
}

.sowing-option-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.sowing-option-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.sowing-option-qty,
.sowing-option-date {
  font-size: 12px;
  color: #606266;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.section-header .section-title {
  margin: 0;
}

.anomaly-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.anomaly-item {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  background: white;
  transition: all 0.3s;
}

.anomaly-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.anomaly-item.level-MILD {
  border-left: 4px solid #e6a23c;
}

.anomaly-item.level-MODERATE {
  border-left: 4px solid #f56c6c;
}

.anomaly-item.level-SEVERE {
  border-left: 4px solid #c0392b;
}

.anomaly-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
}

.anomaly-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.anomaly-level-tag,
.anomaly-status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.anomaly-level-tag.tag-MILD {
  background: #fdf6ec;
  color: #e6a23c;
}

.anomaly-level-tag.tag-MODERATE {
  background: #fef0f0;
  color: #f56c6c;
}

.anomaly-level-tag.tag-SEVERE {
  background: #fbe9e7;
  color: #c0392b;
}

.anomaly-status-tag.status-PENDING {
  background: #fdf6ec;
  color: #e6a23c;
}

.anomaly-status-tag.status-PROCESSING {
  background: #ecf5ff;
  color: #409eff;
}

.anomaly-status-tag.status-RESOLVED {
  background: #f0f9eb;
  color: #67c23a;
}

.anomaly-status-tag.status-CLOSED {
  background: #f4f4f5;
  color: #909399;
}

.anomaly-date {
  font-size: 12px;
  color: #909399;
}

.anomaly-body {
  padding: 14px 16px;
}

.anomaly-info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.anomaly-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.anomaly-info-label {
  font-size: 12px;
  color: #909399;
}

.anomaly-info-value {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.anomaly-info-value.danger {
  color: #f56c6c;
}

.anomaly-section {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.anomaly-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 6px;
}

.anomaly-section-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.6;
}

.anomaly-footer {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
}

.anomaly-actions {
  display: flex;
  gap: 8px;
  padding: 12px 16px;
  background: #fafbfc;
  border-top: 1px solid #ebeef5;
}

.batch-status {
  display: inline-flex;
}
</style>
