<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> Thêm mã CK
      </el-button>
    </div>

    <el-card shadow="never">
      <el-table :data="tickers" stripe border v-loading="loading">
        <el-table-column prop="symbol"   label="Mã"   width="90" />
        <el-table-column prop="name"     label="Tên"  min-width="160" />
        <el-table-column prop="type"     label="Loại" width="120" />
        <el-table-column prop="exchange" label="Sàn"  width="90" />
        <el-table-column prop="industry" label="Ngành" min-width="140" />
        <el-table-column prop="active"   label="Kích hoạt" width="100">
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

    <!-- Dialog form -->
    <el-dialog v-model="dialogVisible" :title="editingRow ? 'Sửa mã CK' : 'Thêm mã CK'" width="480px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="Mã CK" prop="symbol">
          <el-input v-model="form.symbol" :disabled="!!editingRow" style="width:200px" />
        </el-form-item>
        <el-form-item label="Tên" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="Loại tài sản">
          <el-select v-model="form.type" style="width:200px">
            <el-option value="STOCK"            label="Cổ phiếu" />
            <el-option value="GOLD"             label="Vàng" />
            <el-option value="SILVER"           label="Bạc" />
            <el-option value="STOCK_FUND"       label="Quỹ cổ phiếu" />
            <el-option value="BOND_FUND"        label="Quỹ trái phiếu" />
            <el-option value="FUND_CERTIFICATE" label="CCQ" />
          </el-select>
        </el-form-item>
        <el-form-item label="Sàn GD">
          <el-select v-model="form.exchange" style="width:160px">
            <el-option value="HOSE" /><el-option value="HNX" /><el-option value="UPCOM" />
          </el-select>
        </el-form-item>
        <el-form-item label="Ngành">
          <el-input v-model="form.industry" />
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

const tickers = ref([])
const loading = ref(false)
const saving  = ref(false)
const dialogVisible = ref(false)
const editingRow = ref(null)
const formRef = ref(null)

const form = ref({ symbol: '', name: '', type: 'STOCK', exchange: 'HOSE', industry: '', active: true })
const rules = {
  symbol: [{ required: true, message: 'Nhập mã CK' }],
  name:   [{ required: true, message: 'Nhập tên' }]
}

onMounted(fetchData)

async function fetchData() {
  loading.value = true
  try { tickers.value = await refApi.getTickers() }
  finally { loading.value = false }
}

function openDialog(row = null) {
  editingRow.value = row
  if (row) {
    form.value = { ...row }
  } else {
    form.value = { symbol: '', name: '', type: 'STOCK', exchange: 'HOSE', industry: '', active: true }
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()

  const payload = {
    ...form.value,
    symbol: (form.value.symbol || '').trim().toUpperCase(),
    name: (form.value.name || '').trim()
  }

  if (!editingRow.value) {
    const exists = tickers.value.some(t => t.symbol?.toUpperCase() === payload.symbol)
    if (exists) {
      ElMessage.warning(`Mã ${payload.symbol} đã tồn tại`)
      return
    }
  }

  saving.value = true
  try {
    if (editingRow.value) {
      await refApi.updateTicker(form.value.id, payload)
      ElMessage.success('Cập nhật thành công')
    } else {
      await refApi.createTicker(payload)
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
