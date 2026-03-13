<template>
  <div>
    <el-card shadow="never" class="import-card">
      <template #header>Import lịch sử giao dịch quỹ (Fund Statement)</template>
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
              <el-icon><Upload /></el-icon> Import Fund Statement
            </el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <div class="hint">Mẫu hỗ trợ: LỊCH SỬ GIAO DỊCH QUỸ / FUND STATEMENT.</div>
    </el-card>

    <el-card shadow="never" v-if="rows.length">
      <template #header>Kết quả import ({{ rows.length }} giao dịch)</template>
      <el-table :data="rows" stripe border>
        <el-table-column prop="tradingDate" label="Ngày GD" width="120" />
        <el-table-column prop="tickerSymbol" label="Mã quỹ" width="100" />
        <el-table-column prop="trade" label="Loại" width="90" />
        <el-table-column prop="matchedVolume" label="Số lượng" width="120" align="right" />
        <el-table-column prop="matchedValue" label="Giá/đơn vị" width="140" align="right" :formatter="(r) => formatVND(r.matchedValue)" />
        <el-table-column prop="cost" label="Giá vốn" width="120" align="right" :formatter="(r) => formatVND(r.cost)" />
        <el-table-column prop="orderNo" label="Mã import" min-width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useTransactionStore } from '@/stores/transaction'

const tStore = useTransactionStore()
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
    rows.value = await tStore.importFundHistory(accountId.value, file.raw)
    ElMessage.success(`Import thành công ${rows.value.length} giao dịch quỹ`)
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
