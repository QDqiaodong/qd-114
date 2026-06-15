<template>
  <div class="seed-page">
    <div class="page-header">
      <h2 class="page-title">🌱 种子管理</h2>
      <div class="header-actions">
        <el-button :type="showRiskPanel ? 'warning' : 'default'" @click="toggleRiskPanel">
          ⚠️ 保质期风险
          <el-badge v-if="riskTotal > 0" :value="riskTotal" class="risk-badge" />
        </el-button>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          登记种子
        </el-button>
      </div>
    </div>

    <div v-if="showRiskPanel" class="risk-panel card">
      <div class="card-header">
        <h3 class="card-title">⚠️ 种子保质期风险报告</h3>
        <span class="close-btn" @click="showRiskPanel = false">×</span>
      </div>

      <div class="risk-summary">
        <div class="risk-stat expired" @click="activeRiskTab = 'expired'">
          <div class="risk-stat-value">{{ riskData?.expiredCount || 0 }}</div>
          <div class="risk-stat-label">❌ 已过期</div>
        </div>
        <div class="risk-stat expiring" @click="activeRiskTab = 'expiring'">
          <div class="risk-stat-value">{{ riskData?.expiringCount || 0 }}</div>
          <div class="risk-stat-label">⏰ 临期</div>
        </div>
        <div class="risk-stat overstocked" @click="activeRiskTab = 'overstocked'">
          <div class="risk-stat-value">{{ riskData?.overstockedCount || 0 }}</div>
          <div class="risk-stat-label">📦 库存偏高</div>
        </div>
      </div>

      <el-tabs v-model="activeRiskTab" class="risk-tabs">
        <el-tab-pane label="已过期" name="expired">
          <div v-if="riskData?.expiredList?.length" class="risk-list">
            <div v-for="item in riskData.expiredList" :key="item.seedId" class="risk-item expired">
              <div class="risk-item-main">
                <div class="risk-item-name">{{ item.varietyName }}</div>
                <div class="risk-item-reason">{{ item.riskReason }}</div>
              </div>
              <div class="risk-item-meta">
                <span class="risk-meta-tag danger">剩余 {{ item.remainingQuantity }} 粒</span>
                <span class="risk-meta-tag">到期 {{ item.expireDate }}</span>
                <span class="risk-meta-tag">位置 {{ item.storageLocation }}</span>
              </div>
            </div>
          </div>
          <div v-else class="risk-empty">✅ 暂无过期种子</div>
        </el-tab-pane>

        <el-tab-pane label="临期（30天内）" name="expiring">
          <div v-if="riskData?.expiringList?.length" class="risk-list">
            <div v-for="item in riskData.expiringList" :key="item.seedId" class="risk-item expiring">
              <div class="risk-item-main">
                <div class="risk-item-name">{{ item.varietyName }}</div>
                <div class="risk-item-reason">{{ item.riskReason }}</div>
              </div>
              <div class="risk-item-meta">
                <span class="risk-meta-tag warning">剩余 {{ item.remainingQuantity }} 粒</span>
                <span class="risk-meta-tag">到期 {{ item.expireDate }}</span>
                <span class="risk-meta-tag">位置 {{ item.storageLocation }}</span>
              </div>
            </div>
          </div>
          <div v-else class="risk-empty">✅ 暂无临期种子</div>
        </el-tab-pane>

        <el-tab-pane label="库存偏高" name="overstocked">
          <div v-if="riskData?.overstockedList?.length" class="risk-list">
            <div v-for="item in riskData.overstockedList" :key="item.seedId" class="risk-item overstocked">
              <div class="risk-item-main">
                <div class="risk-item-name">{{ item.varietyName }}</div>
                <div class="risk-item-reason">{{ item.riskReason }}</div>
              </div>
              <div class="risk-item-meta">
                <span class="risk-meta-tag info">剩余 {{ item.remainingQuantity }} / 初始 {{ item.initialQuantity }}</span>
                <span class="risk-meta-tag">到期 {{ item.expireDate }}</span>
                <span class="risk-meta-tag">位置 {{ item.storageLocation }}</span>
              </div>
            </div>
          </div>
          <div v-else class="risk-empty">✅ 暂无库存偏高品种</div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="seedList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="varietyName" label="花卉品种" min-width="120" />
        <el-table-column prop="sourceType" label="来源" width="80">
          <template #default="{ row }">
            <el-tag :type="row.sourceType === 'PURCHASE' ? 'primary' : 'success'">
              {{ sourceTypeMap[row.sourceType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="acquireTime" label="购入/采收时间" width="130" />
        <el-table-column prop="storageLocation" label="存放位置" min-width="120" />
        <el-table-column prop="initialQuantity" label="初始数量" width="100" />
        <el-table-column label="剩余数量" width="100">
          <template #default="{ row }">
            <span :class="row.remainingQuantity < 10 ? 'low-stock' : ''">
              {{ row.remainingQuantity }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="supplier" label="供应商/批次" min-width="120" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="seedList.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🌱</div>
        <div class="empty-text">暂无种子记录，点击上方按钮登记</div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑种子信息' : '登记种子'"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="110px"
      >
        <el-form-item label="花卉品种" prop="varietyId">
          <el-select v-model="formData.varietyId" placeholder="请选择品种" style="width: 100%" @change="handleVarietyChange">
            <el-option
              v-for="v in varietyList"
              :key="v.id"
              :label="v.name"
              :value="v.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="来源类型" prop="sourceType">
          <el-radio-group v-model="formData.sourceType">
            <el-radio value="PURCHASE">购入</el-radio>
            <el-radio value="HARVEST">采收</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="购入/采收时间" prop="acquireTime">
          <el-date-picker
            v-model="formData.acquireTime"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="存放位置" prop="storageLocation">
          <el-input v-model="formData.storageLocation" placeholder="如：冰箱保鲜层、干燥柜" />
        </el-form-item>
        <el-form-item label="初始数量" prop="initialQuantity">
          <el-input-number v-model="formData.initialQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="供应商/批次" prop="supplier">
          <el-input v-model="formData.supplier" placeholder="选填" />
        </el-form-item>
        <el-form-item label="保质期(月)" prop="shelfLife">
          <el-input-number v-model="formData.shelfLife" :min="1" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注" prop="notes">
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
import { getSeedList, createSeed, updateSeed, deleteSeed, getSeedShelfLifeRisk } from '@/api/seed'
import { getVarietyList } from '@/api/variety'

const loading = ref(false)
const submitting = ref(false)
const seedList = ref([])
const varietyList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const showRiskPanel = ref(false)
const riskData = ref(null)
const activeRiskTab = ref('expired')

const riskTotal = computed(() => {
  if (!riskData.value) return 0
  return (riskData.value.expiredCount || 0) + (riskData.value.expiringCount || 0) + (riskData.value.overstockedCount || 0)
})

const sourceTypeMap = {
  PURCHASE: '购入',
  HARVEST: '采收'
}

const formData = reactive({
  varietyId: null,
  varietyName: '',
  sourceType: 'PURCHASE',
  acquireTime: '',
  storageLocation: '',
  initialQuantity: 10,
  remainingQuantity: null,
  supplier: '',
  shelfLife: null,
  notes: ''
})

const formRules = {
  varietyId: [{ required: true, message: '请选择品种', trigger: 'change' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }],
  acquireTime: [{ required: true, message: '请选择日期', trigger: 'change' }],
  storageLocation: [{ required: true, message: '请输入存放位置', trigger: 'blur' }],
  initialQuantity: [{ required: true, message: '请输入初始数量', trigger: 'blur' }]
}

const toggleRiskPanel = async () => {
  showRiskPanel.value = !showRiskPanel.value
  if (showRiskPanel.value && !riskData.value) {
    await loadRiskData()
  }
}

const loadRiskData = async () => {
  try {
    const data = await getSeedShelfLifeRisk()
    riskData.value = data || null
  } catch (e) {
    console.error(e)
  }
}

const handleVarietyChange = (val) => {
  const variety = varietyList.value.find(v => v.id === val)
  if (variety) {
    formData.varietyName = variety.name
  }
}

const loadSeeds = async () => {
  loading.value = true
  try {
    const data = await getSeedList()
    seedList.value = data || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
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

const handleAdd = () => {
  editId.value = null
  Object.assign(formData, {
    varietyId: null,
    varietyName: '',
    sourceType: 'PURCHASE',
    acquireTime: '',
    storageLocation: '',
    initialQuantity: 10,
    remainingQuantity: null,
    supplier: '',
    shelfLife: null,
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
    await ElMessageBox.confirm(`确定删除「${row.varietyName}」的种子记录吗？`, '提示', {
      type: 'warning'
    })
    await deleteSeed(row.id)
    ElMessage.success('删除成功')
    loadSeeds()
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
      await updateSeed(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      formData.remainingQuantity = formData.initialQuantity
      await createSeed(formData)
      ElMessage.success('登记成功')
    }

    dialogVisible.value = false
    loadSeeds()
    if (showRiskPanel.value) {
      loadRiskData()
    }
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadSeeds()
  loadVarieties()
  loadRiskData()
})
</script>

<style scoped>
.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.risk-badge {
  margin-left: 6px;
}

.low-stock {
  color: #f56c6c;
  font-weight: 600;
}

.risk-panel {
  margin-bottom: 20px;
}

.risk-panel .card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.risk-panel .close-btn {
  cursor: pointer;
  font-size: 24px;
  color: #c0c4cc;
  line-height: 1;
  padding: 0 8px;
  transition: color 0.2s;
}

.risk-panel .close-btn:hover {
  color: #f56c6c;
}

.risk-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.risk-stat {
  text-align: center;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.risk-stat:hover {
  transform: translateY(-2px);
}

.risk-stat.expired {
  background: #fef0f0;
  border: 1px solid #fab6b6;
}

.risk-stat.expiring {
  background: #fdf6ec;
  border: 1px solid #f3d19e;
}

.risk-stat.overstocked {
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
}

.risk-stat-value {
  font-size: 28px;
  font-weight: 700;
}

.risk-stat.expired .risk-stat-value {
  color: #f56c6c;
}

.risk-stat.expiring .risk-stat-value {
  color: #e6a23c;
}

.risk-stat.overstocked .risk-stat-value {
  color: #409eff;
}

.risk-stat-label {
  font-size: 13px;
  color: #606266;
  margin-top: 4px;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-radius: 6px;
  transition: background 0.2s;
}

.risk-item:hover {
  background: #f5f7fa;
}

.risk-item.expired {
  border-left: 4px solid #f56c6c;
  background: #fef0f0;
}

.risk-item.expiring {
  border-left: 4px solid #e6a23c;
  background: #fdf6ec;
}

.risk-item.overstocked {
  border-left: 4px solid #409eff;
  background: #ecf5ff;
}

.risk-item-main {
  flex: 1;
  min-width: 0;
}

.risk-item-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.risk-item-reason {
  font-size: 12px;
  color: #909399;
}

.risk-item-meta {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.risk-meta-tag {
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
  background: white;
  color: #606266;
  white-space: nowrap;
}

.risk-meta-tag.danger {
  color: #f56c6c;
  background: #fff;
  font-weight: 600;
}

.risk-meta-tag.warning {
  color: #e6a23c;
  background: #fff;
  font-weight: 600;
}

.risk-meta-tag.info {
  color: #409eff;
  background: #fff;
  font-weight: 600;
}

.risk-empty {
  text-align: center;
  padding: 40px 20px;
  color: #67c23a;
  font-size: 14px;
}

@media (max-width: 768px) {
  .risk-summary {
    grid-template-columns: 1fr;
  }

  .risk-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .risk-item-meta {
    margin-left: 0;
    margin-top: 8px;
  }
}
</style>
