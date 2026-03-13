<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> Thêm Broker
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="brokers" stripe border v-loading="loading">
        <el-table-column prop="code"           label="Mã"    width="90" />
        <el-table-column prop="name"           label="Tên"   min-width="160" />
        <el-table-column prop="defaultFeeRate" label="Phí mặc định (%)" width="160" align="right"
          :formatter="(r) => r.defaultFeeRate ? (r.defaultFeeRate * 100).toFixed(3) + '%' : '—'" />
        <el-table-column prop="website"        label="Website" min-width="160" />
        <el-table-column prop="active"         label="Kích hoạt" width="100">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? 'Có' : 'Không' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">Sửa</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingRow ? 'Sửa Broker' : 'Thêm Broker'" width="440px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="Mã broker" prop="code">
          <el-input v-model="form.code" :disabled="!!editingRow" />
        </el-form-item>
        <el-form-item label="Tên" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Phí mặc định (%)">
          <el-input-number v-model="form.defaultFeeRate" :min="0" :max="1" :step="0.0001" :precision="4" />
        </el-form-item>
        <el-form-item label="Website">
          <el-input v-model="form.website" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="Kích hoạt">
          <el-switch v-model="form.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Hủy</el-button>
        <el-button type="primary" :loading="saving" @click="save">Lưu</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { refApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const brokers = ref([])
const loading = ref(false)
const saving  = ref(false)
const dialogVisible = ref(false)
const editingRow = ref(null)
const formRef = ref(null)

const form = ref({ code: '', name: '', defaultFeeRate: 0.0015, website: '', active: true })
const rules = {
  code: [{ required: true, message: 'Nhập mã broker' }],
  name: [{ required: true, message: 'Nhập tên broker' }]
}

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try { brokers.value = await refApi.getBrokers() }
  finally { loading.value = false }
}

function openDialog(row = null) {
  editingRow.value = row
  form.value = row
    ? { ...row }
    : { code: '', name: '', defaultFeeRate: 0.0015, website: '', active: true }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingRow.value) {
      await refApi.updateBroker(form.value.id, form.value)
      ElMessage.success('Cập nhật thành công')
    } else {
      await refApi.createBroker(form.value)
      ElMessage.success('Thêm thành công')
    }
    dialogVisible.value = false
    await fetchData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
