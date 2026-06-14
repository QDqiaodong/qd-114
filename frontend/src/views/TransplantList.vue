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
        <el-table-column prop="transplantTime" label="移栽时间" width="160" />
        <el-table-column prop="potSpecification" label="花盆规格" width="120" />
        <el-table-column prop="soilRatio" label="盆土配比" min-width="150" />
        <el-table-column prop="transplantQuantity" label="移栽数量" width="100">
          <template #default="{ row }">{{ row.transplantQuantity }} 株</template>
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
            />
          </el-select>
        </el-form-item>
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
          <el-input-number v-model="formData.transplantQuantity" :min="1" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">株</span>
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

    <el-dialog v-model="detailVisible" title="移栽详情" width="600px">
      <div v-if="currentDetail" class="detail-content">
        <div class="detail-item">
          <span class="detail-label">花卉品种：</span>
          <span class="detail-value">{{ currentDetail.varietyName }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">移栽时间：</span>
          <span class="detail-value">{{ currentDetail.transplantTime }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">花盆规格：</span>
          <span class="detail-value">{{ currentDetail.potSpecification }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">移栽数量：</span>
          <span class="detail-value">{{ currentDetail.transplantQuantity }} 株</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">盆土配比：</span>
          <span class="detail-value">{{ currentDetail.soilRatio || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">光照要求：</span>
          <span class="detail-value">{{ currentDetail.lightRequirement || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">浇水频率：</span>
          <span class="detail-value">{{ currentDetail.wateringFrequency || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">施肥计划：</span>
          <span class="detail-value">{{ currentDetail.fertilizationPlan || '-' }}</span>
        </div>
        <div class="detail-item tips-item" v-if="currentDetail.recoveryTips">
          <span class="detail-label">💡 缓苗养护要点：</span>
          <div class="detail-value tips-content">{{ currentDetail.recoveryTips }}</div>
        </div>
        <div class="detail-item" v-if="currentDetail.notes">
          <span class="detail-label">备注：</span>
          <span class="detail-value">{{ currentDetail.notes }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getTransplantList,
  createTransplant,
  updateTransplant,
  deleteTransplant
} from '@/api/transplant'
import { getSowingList } from '@/api/sowing'
import dayjs from 'dayjs'

const loading = ref(false)
const submitting = ref(false)
const transplantList = ref([])
const sowingList = ref([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)
const currentDetail = ref(null)

const formatDate = (date) => dayjs(date).format('YYYY-MM-DD')

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

const handleSowingChange = (val) => {
  const sowing = sowingList.value.find(s => s.id === val)
  if (sowing) {
    formData.varietyId = sowing.varietyId
    formData.varietyName = sowing.varietyName
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
  } catch (e) {
    console.error(e)
  }
}

const handleAdd = () => {
  editId.value = null
  Object.assign(formData, {
    sowingId: null,
    varietyId: null,
    varietyName: '',
    transplantTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
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

const handleEdit = (row) => {
  editId.value = row.id
  Object.assign(formData, { ...row })
  dialogVisible.value = true
}

const handleDetail = (row) => {
  currentDetail.value = row
  detailVisible.value = true
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

onMounted(() => {
  loadTransplants()
  loadSowings()
})
</script>

<style scoped>
.detail-content {
  padding: 10px 0;
}

.detail-item {
  display: flex;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;

  &:last-child {
    border-bottom: none;
  }

  &.tips-item {
    flex-direction: column;
    background: #fdf6ec;
    padding: 12px;
    border-radius: 6px;
    border: none;
    margin-top: 10px;
  }
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

.tips-content {
  margin-top: 8px;
  line-height: 1.6;
}
</style>
