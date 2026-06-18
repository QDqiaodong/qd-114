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
  deleteGerminationObservation
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
</style>
