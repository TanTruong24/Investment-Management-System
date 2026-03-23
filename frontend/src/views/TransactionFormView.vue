<template>
  <div class="form-wrapper">
    <el-card shadow="never">
      <template #header>
        <span>{{ isEdit ? 'Cập nhật giao dịch' : 'Tạo giao dịch mới' }}</span>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="150px">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="Tài khoản" prop="accountId">
              <el-select v-model="form.accountId" placeholder="Chọn tài khoản" style="width:100%">
                <el-option v-for="a in store.accounts" :key="a.id" :label="a.name" :value="a.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Mã chứng khoán" prop="tickerSymbol">
              <el-select v-model="form.tickerSymbol" placeholder="Chọn mã chứng khoán" filterable clearable style="width:100%">
                <el-option
                  v-for="t in store.tickers"
                  :key="t.id"
                  :label="`${t.symbol} - ${t.name}`"
                  :value="t.symbol"
                />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Ngày giao dịch" prop="tradingDate">
              <el-date-picker v-model="form.tradingDate" type="date"
                value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Loại giao dịch" prop="trade">
              <el-radio-group v-model="form.trade">
                <el-radio-button value="BUY">Mua</el-radio-button>
                <el-radio-button value="SELL">Bán</el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Khối lượng đặt" prop="volume">
              <el-input-number v-model="form.volume" :min="0" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Giá đặt" prop="orderPrice">
              <el-input-number v-model="form.orderPrice" :min="0" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="KL khớp lệnh" prop="matchedVolume">
              <el-input-number v-model="form.matchedVolume" :min="0" :step="100" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="Giá khớp" prop="matchedPrice">
              <el-input-number v-model="form.matchedPrice" :min="0" style="width:100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Giá trị khớp">
              <el-input :model-value="formatVND(matchedValuePreview)" readonly />
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="Sàn giao dịch">
              <el-select v-model="form.stockExchange" style="width:100%">
                <el-option value="HOSE" /><el-option value="HNX" /><el-option value="UPCOM" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Loại lệnh">
              <el-select v-model="form.orderType" style="width:100%">
                <el-option label="Thường" value="NORMAL" />
                <el-option label="Phái sinh" value="DERIVATIVE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="Kênh đặt lệnh">
              <el-input v-model="form.channel" placeholder="Online, Broker..." />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="Số hiệu lệnh">
              <el-input v-model="form.orderNo" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Phí GD (tự tính nếu để trống)">
              <el-input-number v-model="form.fee" :min="0" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Thuế TNCN">
              <el-input-number v-model="form.tax" :min="0" style="width:100%" controls-position="right" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="Ghi chú">
              <el-input v-model="form.note" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button @click="$router.back()">Hủy</el-button>
          <el-button type="primary" :loading="loading" @click="submit">
            {{ isEdit ? 'Lưu thay đổi' : 'Tạo giao dịch' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTransactionStore } from '@/stores/transaction'
import { ElMessage } from 'element-plus'

const route  = useRoute()
const router = useRouter()
const store  = useTransactionStore()

const isEdit  = computed(() => !!route.params.id)
const loading = ref(false)
const formRef = ref(null)

const form = ref({
  accountId: null, tickerSymbol: '', tradingDate: null, trade: 'BUY',
  volume: 0, orderPrice: 0, matchedVolume: 0, matchedPrice: 0, matchedValue: 0,
  stockExchange: 'HOSE', orderType: 'NORMAL', channel: 'Online',
  orderNo: '', note: '', fee: null, tax: null
})

const matchedValuePreview = computed(() => {
  const volume = Number(form.value.matchedVolume || 0)
  const price = Number(form.value.matchedPrice || 0)
  return volume > 0 && price > 0 ? volume * price : 0
})

const rules = {
  accountId:    [{ required: true, message: 'Chọn tài khoản' }],
  tickerSymbol: [{ required: true, message: 'Chọn mã chứng khoán' }],
  tradingDate:  [{ required: true, message: 'Chọn ngày giao dịch' }],
  trade:        [{ required: true }],
  matchedVolume:[{ required: true, type: 'number', min: 1, message: 'KL khớp > 0' }],
  matchedPrice: [{ required: true, type: 'number', min: 1, message: 'Giá khớp > 0' }]
}

onMounted(async () => {
  await store.loadRefData()
  if (isEdit.value) {
    // Load existing transaction to prefill form
    const found = store.transactions.find(t => t.id == route.params.id)
    if (found) Object.assign(form.value, found)
  }
})

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const payload = {
      ...form.value,
      matchedValue: null
    }

    if (isEdit.value) {
      await store.updateTransaction(route.params.id, payload)
      ElMessage.success('Cập nhật thành công')
    } else {
      await store.createTransaction(payload)
      ElMessage.success('Tạo giao dịch thành công')
    }
    router.push('/transactions')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    loading.value = false
  }
}

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<style scoped>
.form-wrapper { max-width: 1000px; }
.form-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; }
</style>
