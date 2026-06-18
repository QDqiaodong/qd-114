<template>
  <div class="seedling-plan-page">
    <div class="page-header">
      <h2 class="page-title">📋 育苗计划表</h2>
      <div class="header-actions">
        <el-select
          v-model="filterStatus"
          placeholder="筛选状态"
          clearable
          style="width: 160px; margin-right: 12px;"
          @change="handleFilterChange"
        >
          <el-option label="待执行" value="PENDING" />
          <el-option label="已播种" value="SOWING" />
          <el-option label="已移栽" value="TRANSPLANTED" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增计划
        </el-button>
      </div>
    </div>

    <div v-if="statistics" class="stat-grid">
      <div class="stat-card stat-primary">
        <div class="stat-icon">📋</div>
        <div class="stat-value">{{ statistics.total }}</div>
        <div class="stat-label">总计划数</div>
      </div>
      <div class="stat-card stat-warning">
        <div class="stat-icon">⏳</div>
        <div class="stat-value">{{ statistics.pending }}</div>
        <div class="stat-label">待执行</div>
      </div>
      <div class="stat-card stat-success">
        <div class="stat-icon">🌱</div>
        <div class="stat-value">{{ statistics.sowing }}</div>
        <div class="stat-label">已播种</div>
      </div>
      <div class="stat-card stat-danger">
        <div class="stat-icon">🪴</div>
        <div class="stat-value">{{ statistics.transplanted + statistics.done }}</div>
        <div class="stat-label">已移栽/完成</div>
      </div>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="filteredPlans"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="varietyName" label="品种" min-width="100" />
        <el-table-column label="发芽天数" width="90">
          <template #default="{ row }">
            {{ row.germinationDays ? row.germinationDays + '天' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="育苗天数" width="90">
          <template #default="{ row }">
            {{ row.seedlingDays ? row.seedlingDays + '天' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="保质期" width="80">
          <template #default="{ row }">
            {{ row.shelfLife ? row.shelfLife + '月' : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="计划播种" width="110">
          <template #default="{ row }">
            <span :class="{ 'date-overdue': isOverdue(row.plannedSowDate, row.status) }">
              {{ row.plannedSowDate || '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="计划移栽" width="110">
          <template #default="{ row }">{{ row.plannedTransplantDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="过期日期" width="110">
          <template #default="{ row }">
            <el-tag
              v-if="row.expireDate"
              :type="getExpireTagType(row.expireDate)"
              size="small"
              effect="plain"
            >
              {{ row.expireDate }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="实际播种" width="110">
          <template #default="{ row }">{{ row.actualSowDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="success"
              link
              size="small"
              @click="handleStatusChange(row, 'SOWING')"
            >
              标记播种
            </el-button>
            <el-button
              v-if="row.status === 'SOWING'"
              type="primary"
              link
              size="small"
              @click="handleStatusChange(row, 'TRANSPLANTED')"
            >
              标记移栽
            </el-button>
            <el-button
              v-if="row.status === 'TRANSPLANTED'"
              type="success"
              link
              size="small"
              @click="handleStatusChange(row, 'DONE')"
            >
              标记完成
            </el-button>
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="filteredPlans.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无育苗计划，点击上方按钮新增</div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑育苗计划' : '新增育苗计划'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item label="选择种子" prop="seedId">
          <el-select v-model="formData.seedId" placeholder="请选择种子" style="width: 100%" @change="handleSeedChange" filterable>
            <el-option
              v-for="seed in seedList"
              :key="seed.id"
              :value="seed.id"
              :disabled="seed.remainingQuantity <= 0"
            >
              <div class="seed-option">
                <span class="seed-option-name">{{ seed.varietyName }}</span>
                <span class="seed-option-stock">剩余 {{ seed.remainingQuantity }} 粒</span>
                <span v-if="seed.shelfLife" class="seed-option-shelf">保质期 {{ seed.shelfLife }}月</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="计划播种日期" prop="plannedSowDate">
          <el-date-picker
            v-model="formData.plannedSowDate"
            type="date"
            placeholder="选择计划播种日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getSeedList } from '@/api/seed'
import {
  getSeedlingPlanList,
  createSeedlingPlan,
  updateSeedlingPlan,
  updateSeedlingPlanStatus,
  deleteSeedlingPlan
} from '@/api/seedlingPlan'

const loading = ref(false)
const submitting = ref(false)
const planList = ref([])
const seedList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const filterStatus = ref('')

const statistics = computed(() => {
  const total = planList.value.length
  const pending = planList.value.filter(p => p.status === 'PENDING').length
  const sowing = planList.value.filter(p => p.status === 'SOWING').length
  const transplanted = planList.value.filter(p => p.status === 'TRANSPLANTED').length
  const done = planList.value.filter(p => p.status === 'DONE').length
  return { total, pending, sowing, transplanted, done }
})

const filteredPlans = computed(() => {
  if (!filterStatus.value) return planList.value
  return planList.value.filter(p => p.status === filterStatus.value)
})

const formData = reactive({
  seedId: null,
  plannedSowDate: '',
  notes: ''
})

const formRules = {
  seedId: [{ required: true, message: '请选择种子', trigger: 'change' }],
  plannedSowDate: [{ required: true, message: '请选择计划播种日期', trigger: 'change' }]
}

const getStatusLabel = (status) => {
  const map = { PENDING: '待执行', SOWING: '已播种', TRANSPLANTED: '已移栽', DONE: '已完成', CANCELLED: '已取消' }
  return map[status] || status
}

const getStatusTagType = (status) => {
  const map = { PENDING: 'warning', SOWING: 'success', TRANSPLANTED: 'primary', DONE: 'info', CANCELLED: 'danger' }
  return map[status] || 'info'
}

const getExpireTagType = (expireDate) => {
  if (!expireDate) return 'info'
  const diff = new Date(expireDate) - new Date()
  if (diff < 0) return 'danger'
  if (diff < 30 * 24 * 60 * 60 * 1000) return 'warning'
  return 'success'
}

const isOverdue = (date, status) => {
  if (!date || status !== 'PENDING') return false
  return new Date(date) < new Date()
}

const handleFilterChange = () => {}

const handleSeedChange = (val) => {
  const seed = seedList.value.find(s => s.id === val)
  if (seed) {
    formData.seedId = seed.id
  }
}

const loadPlans = async () => {
  loading.value = true
  try {
    const data = await getSeedlingPlanList()
    planList.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const loadSeeds = async () => {
  try {
    const data = await getSeedList()
    seedList.value = data || []
  } catch (e) {
    console.error(e)
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(formData, {
    seedId: null,
    plannedSowDate: '',
    notes: ''
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(formData, {
    seedId: row.seedId,
    plannedSowDate: row.plannedSowDate,
    notes: row.notes
  })
  dialogVisible.value = true
}

const handleStatusChange = async (row, newStatus) => {
  try {
    const statusLabel = getStatusLabel(newStatus)
    await ElMessageBox.confirm(`确定将「${row.varietyName}」标记为${statusLabel}吗？`, '确认', {
      type: 'info'
    })
    const today = new Date().toISOString().split('T')[0]
    await updateSeedlingPlanStatus(row.id, newStatus, today)
    ElMessage.success(`已标记为${statusLabel}`)
    loadPlans()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.varietyName}」的育苗计划吗？`, '提示', {
      type: 'warning'
    })
    await deleteSeedlingPlan(row.id)
    ElMessage.success('删除成功')
    loadPlans()
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
      await updateSeedlingPlan(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createSeedlingPlan(formData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadPlans()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPlans()
  loadSeeds()
})
</script>

<style scoped>
.seed-option {
  display: flex;
  align-items: center;
  gap: 12px;
}

.seed-option-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.seed-option-stock {
  font-size: 12px;
  color: #67c23a;
}

.seed-option-shelf {
  font-size: 12px;
  color: #909399;
}

.date-overdue {
  color: #f56c6c;
  font-weight: 500;
}
</style>
