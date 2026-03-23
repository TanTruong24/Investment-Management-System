<template>
  <div class="form-wrapper">
    <el-card shadow="never">
      <template #header>Cập nhật giá chứng khoán</template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-form-item label="Mã chứng khoán" prop="tickerSymbol">
          <el-select v-model="form.tickerSymbol" placeholder="Chọn mã chứng khoán" filterable clearable style="width:320px">
            <el-option
              v-for="t in store.tickers"
              :key="t.id"
              :label="`${t.symbol} - ${t.name}`"
              :value="t.symbol"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Ngày" prop="priceDate">
          <el-date-picker v-model="form.priceDate" type="date" value-format="YYYY-MM-DD" style="width:200px" />
        </el-form-item>
        <el-form-item label="Giá đóng cửa" prop="closePrice">
          <el-input-number v-model="form.closePrice" :min="0" :step="100" style="width:200px" />
        </el-form-item>
        <el-form-item label="Giá mở cửa">
          <el-input-number v-model="form.open" :min="0" :step="100" style="width:200px" />
        </el-form-item>
        <el-form-item label="Giá cao nhất">
          <el-input-number v-model="form.high" :min="0" :step="100" style="width:200px" />
        </el-form-item>
        <el-form-item label="Giá thấp nhất">
          <el-input-number v-model="form.low" :min="0" :step="100" style="width:200px" />
        </el-form-item>
        <el-form-item label="Khối lượng">
          <el-input-number v-model="form.volume" :min="0" style="width:200px" />
        </el-form-item>
        <el-form-item label="Nguồn">
          <el-select v-model="form.source" style="width:200px">
            <el-option value="MANUAL" label="Nhập tay" />
            <el-option value="CRAWL"  label="Crawl tự động" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <div class="action-row">
            <el-button class="action-btn" type="primary" :loading="loading" @click="submit">Cập nhật giá</el-button>
            <el-button class="action-btn" type="success" :loading="autoLoading" @click="refreshHeld">
            Cập nhật mã đang giữ (Vietstock)
            </el-button>
            <el-button class="action-btn" @click="resetForm">Reset</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useTransactionStore } from '@/stores/transaction'
import { priceApi } from '@/services/api'
import { ElMessage } from 'element-plus'

const store   = useTransactionStore()
const formRef = ref(null)
const loading = ref(false)
const autoLoading = ref(false)

const form = ref({
  tickerSymbol: '', priceDate: null, closePrice: 0,
  open: null, high: null, low: null, volume: null, source: 'MANUAL'
})

const rules = {
  tickerSymbol: [{ required: true, message: 'Chọn mã chứng khoán' }],
  priceDate:    [{ required: true, message: 'Chọn ngày' }],
  closePrice:   [{ required: true, type: 'number', min: 1, message: 'Giá đóng cửa > 0' }]
}

onMounted(() => store.loadRefData())

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await priceApi.update(form.value)
    ElMessage.success(`Đã cập nhật giá ${form.value.tickerSymbol}`)
    resetForm()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    loading.value = false
  }
}

async function refreshHeld() {
  autoLoading.value = true
  try {
    const res = await priceApi.refreshHeld()
    ElMessage.success(`Đã cập nhật ${res.updatedCount} mã đang giữ`)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Không cập nhật được giá tự động')
  } finally {
    autoLoading.value = false
  }
}

function resetForm() {
  form.value = { tickerSymbol: '', priceDate: null, closePrice: 0, open: null, high: null, low: null, volume: null, source: 'MANUAL' }
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.form-wrapper { max-width: 600px; }

.action-row {
  display: grid;
  grid-template-columns: 1fr 1.35fr 120px;
  gap: 10px;
  width: 100%;
  align-items: stretch;
}

.action-btn {
  min-width: 0;
  height: auto;
  line-height: 1.2;
  padding-top: 10px;
  padding-bottom: 10px;
}

:deep(.action-btn > span) {
  white-space: normal;
  word-break: break-word;
  text-align: center;
}

@media (max-width: 720px) {
  .action-row {
    grid-template-columns: 1fr;
  }
}
</style>
