<template>
  <div>
    <el-card shadow="never" class="import-card">
      <template #header>Import sao kê tiền (Cash Statement)</template>
      <el-form inline>
        <el-form-item label="Tài khoản">
          <el-select v-model="accountId" placeholder="Chọn tài khoản" style="width:260px" clearable>
            <el-option v-for="a in tStore.accounts" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-change="handleFileChange"
          >
            <el-button type="primary" :loading="importing">
              <el-icon><Upload /></el-icon> Import Cash Statement
            </el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div class="hint">Mẫu hỗ trợ: SAO KÊ TIỀN / CASH STATEMENT.</div>
    </el-card>

    <el-card shadow="never" v-if="rows.length">
      <template #header>Kết quả import ({{ rows.length }} dòng tiền)</template>
      <el-table :data="rows" stripe border>
        <el-table-column prop="flowDate" label="Ngày GD" width="120" />
        <el-table-column prop="type" label="Loại" width="110">
          <template #default="{ row }">
            <el-tag :type="row.type === 'DEPOSIT' ? 'success' : 'danger'">
              {{ row.type === 'DEPOSIT' ? 'Nạp' : 'Rút' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="Số tiền" width="160" align="right" :formatter="(r) => formatVND(r.amount)" />
        <el-table-column prop="note" label="Mô tả" min-width="260" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTransactionStore } from '@/stores/transaction'
import { usePortfolioStore } from '@/stores/portfolio'

const tStore = useTransactionStore()
const pStore = usePortfolioStore()

const accountId = ref(null)
const importing = ref(false)
const rows = ref([])

onMounted(async () => {
  await tStore.loadRefData()
})

async function handleFileChange(file) {
  if (!accountId.value) {
    ElMessage.warning('Vui lòng chọn tài khoản trước khi import')
    return
  }

  importing.value = true
  try {
    rows.value = await pStore.importCashStatement(accountId.value, file.raw)
    ElMessage.success(`Import thành công ${rows.value.length} dòng tiền`)
  } catch (e) {
    ElMessage.error('Import thất bại: ' + (e.response?.data?.message || e.message))
  } finally {
    importing.value = false
  }
}

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<style scoped>
.import-card { margin-bottom: 12px; }
.hint { color: #909399; font-size: 12px; margin-top: 8px; }
</style>
