<template>
  <div class="sowing-page">
    <div class="page-header">
      <h2 class="page-title">🌾 播种记录</h2>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        新增播种
      </el-button>
    </div>

    <div class="card">
      <el-table
        v-loading="loading"
        :data="sowingList"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="varietyName" label="花卉品种" min-width="120" />
        <el-table-column label="播种时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.sowingTime) }}</template>
        </el-table-column>
        <el-table-column prop="soilRatio" label="盆土配比" min-width="150" />
        <el-table-column prop="coveringThickness" label="覆土厚度" width="100">
          <template #default="{ row }">
            {{ row.coveringThickness ? row.coveringThickness + ' cm' : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sowingQuantity" label="播种数量" width="100">
          <template #default="{ row }">{{ row.sowingQuantity }} 粒</template>
        </el-table-column>
        <el-table-column prop="containerType" label="容器类型" width="100" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewGrowth(row)">
              生长跟踪
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

      <div v-if="sowingList.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">🌾</div>
        <div class="empty-text">暂无播种记录，点击上方按钮新增</div>
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑播种记录' : '新增播种'"
      width="650px"
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
              :label="`${seed.varietyName} (剩余${seed.remainingQuantity}粒)`"
              :value="seed.id"
              :disabled="seed.remainingQuantity <= 0"
            />
          </el-select>
        </el-form-item>

        <SeedDetailTip :seed-id="formData.seedId" />
        <el-form-item label="播种时间" prop="sowingTime">
          <el-date-picker
            v-model="formData.sowingTime"
            type="datetime"
            placeholder="选择播种时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="播种数量" prop="sowingQuantity">
          <el-input-number v-model="formData.sowingQuantity" :min="1" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            可用: {{ availableQuantity }} 粒
          </span>
        </el-form-item>
        <el-form-item label="盆土配比" prop="soilRatio">
          <el-input v-model="formData.soilRatio" placeholder="如：泥炭土:珍珠岩:蛭石=3:1:1" />
        </el-form-item>
        <el-form-item label="覆土厚度" prop="coveringThickness">
          <el-input-number v-model="formData.coveringThickness" :min="0" :step="0.1" :precision="2" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">cm</span>
        </el-form-item>
        <el-form-item label="浇水方案" prop="wateringPlan">
          <el-input v-model="formData.wateringPlan" type="textarea" :rows="2" placeholder="初期浇水频率、水量等" />
        </el-form-item>
        <el-form-item label="容器类型" prop="containerType">
          <el-input v-model="formData.containerType" placeholder="如：育苗盘、营养钵" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="环境温度" prop="environmentTemp">
              <el-input-number v-model="formData.environmentTemp" :step="0.1" :precision="1" />
              <span style="margin-left: 8px; font-size: 12px; color: #909399;">°C</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="环境湿度" prop="environmentHumidity">
              <el-input-number v-model="formData.environmentHumidity" :min="0" :max="100" :step="1" />
              <span style="margin-left: 8px; font-size: 12px; color: #909399;">%</span>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="notes">
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getSowingList, createSowing, updateSowing, deleteSowing } from '@/api/sowing'
import { getSeedList } from '@/api/seed'
import SeedDetailTip from '@/components/SeedDetailTip.vue'
import { formatDateTime, getCurrentLocalDateTime } from '@/utils/date'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const sowingList = ref([])
const seedList = ref([])
const dialogVisible = ref(false)
const editId = ref(null)
const formRef = ref(null)

const selectedSeed = computed(() => {
  return seedList.value.find(s => s.id === formData.seedId)
})

const availableQuantity = computed(() => {
  return selectedSeed.value ? selectedSeed.value.remainingQuantity : 0
})

const formData = reactive({
  seedId: null,
  varietyId: null,
  varietyName: '',
  sowingTime: '',
  soilRatio: '',
  coveringThickness: null,
  wateringPlan: '',
  sowingQuantity: 10,
  containerType: '',
  environmentTemp: null,
  environmentHumidity: null,
  notes: ''
})

const formRules = {
  seedId: [{ required: true, message: '请选择种子', trigger: 'change' }],
  sowingTime: [{ required: true, message: '请选择播种时间', trigger: 'change' }],
  sowingQuantity: [{ required: true, message: '请输入播种数量', trigger: 'blur' }],
  soilRatio: [{ required: true, message: '请输入盆土配比', trigger: 'blur' }],
  wateringPlan: [{ required: true, message: '请输入浇水方案', trigger: 'blur' }]
}

const handleSeedChange = (val) => {
  const seed = seedList.value.find(s => s.id === val)
  if (seed) {
    formData.varietyId = seed.varietyId
    formData.varietyName = seed.varietyName
    if (formData.sowingQuantity > seed.remainingQuantity) {
      formData.sowingQuantity = seed.remainingQuantity
    }
  }
}

const loadSowings = async () => {
  loading.value = true
  try {
    const data = await getSowingList()
    sowingList.value = data || []
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

const viewGrowth = (row) => {
  router.push({ path: '/growth', query: { sowingId: row.id } })
}

const handleAdd = () => {
  editId.value = null
  Object.assign(formData, {
    seedId: null,
    varietyId: null,
    varietyName: '',
    sowingTime: getCurrentLocalDateTime(),
    soilRatio: '',
    coveringThickness: null,
    wateringPlan: '',
    sowingQuantity: 10,
    containerType: '',
    environmentTemp: null,
    environmentHumidity: null,
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
    await ElMessageBox.confirm(`确定删除「${row.varietyName}」的播种记录吗？`, '提示', {
      type: 'warning'
    })
    await deleteSowing(row.id)
    ElMessage.success('删除成功')
    loadSowings()
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

    if (!editId.value && formData.sowingQuantity > availableQuantity.value) {
      ElMessage.error('播种数量不能超过剩余数量')
      return
    }

    submitting.value = true

    if (editId.value) {
      await updateSowing(editId.value, formData)
      ElMessage.success('更新成功')
    } else {
      await createSowing(formData)
      ElMessage.success('添加成功')
    }

    dialogVisible.value = false
    loadSowings()
    loadSeeds()
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadSowings()
  loadSeeds()
})
</script>

<style scoped>
</style>
