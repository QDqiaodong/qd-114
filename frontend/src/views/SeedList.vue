<template>
  <div class="seed-page">
    <div class="page-header">
      <h2 class="page-title">🌱 种子管理</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        登记种子
      </el-button>
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
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getSeedList, createSeed, updateSeed, deleteSeed } from '@/api/seed'
import { getVarietyList } from '@/api/variety'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const loading = ref(false)
const submitting = ref(false)
const seedList = ref([])
const varietyList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)

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
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadSeeds()
  loadVarieties()
})
</script>

<style scoped>
.low-stock {
  color: #f56c6c;
  font-weight: 600;
}
</style>
