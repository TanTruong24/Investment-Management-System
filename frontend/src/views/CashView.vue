<template>
  <div>
    <!-- Account selector -->
    <el-card shadow="never" class="selector-card">
      <el-form inline>
        <el-form-item label="Tài khoản">
          <el-select v-model="selectedAccountId" placeholder="Chọn tài khoản" @change="loadData" style="width:220px">
            <el-option v-for="a in accounts" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="cashSummary">
      <!-- Summary cards -->
      <el-row :gutter="16" class="summary-row">
        <el-col :span="4">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Tổng nạp</div>
            <div class="stat-value profit">{{ formatVND(cashSummary.totalDeposit) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Tổng rút</div>
            <div class="stat-value loss">{{ formatVND(cashSummary.totalWithdraw) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Tiền vào ròng</div>
            <div class="stat-value" :class="cashSummary.netCashIn >= 0 ? 'profit' : 'loss'">
              {{ formatVND(cashSummary.netCashIn) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Số dư tiền mặt</div>
            <div class="stat-value highlight">{{ formatVND(cashSummary.cashBalance) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Sức mua</div>
            <div class="stat-value">{{ formatVND(cashSummary.purchasingPower) }}</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Khả dụng rút</div>
            <div class="stat-value">{{ formatVND(cashSummary.availableForWithdrawal) }}</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Add cash flow form -->
      <el-card shadow="never" class="form-card">
        <template #header>Nạp / Rút tiền</template>
        <el-form ref="cfFormRef" :model="cfForm" :rules="cfRules" inline>
          <el-form-item label="Loại" prop="type">
            <el-radio-group v-model="cfForm.type">
              <el-radio-button value="DEPOSIT">Nạp tiền</el-radio-button>
              <el-radio-button value="WITHDRAW">Rút tiền</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="Số tiền" prop="amount">
            <el-input-number v-model="cfForm.amount" :min="1" :step="1000000" />
          </el-form-item>
          <el-form-item label="Ngày" prop="flowDate">
            <el-date-picker v-model="cfForm.flowDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
          <el-form-item label="Ghi chú">
            <el-input v-model="cfForm.note" placeholder="..." style="width:200px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="submitCashFlow">Xác nhận</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- Cash flow history -->
      <el-card shadow="never">
        <template #header>Lịch sử giao dịch tiền</template>
        <el-table :data="pStore.cashFlows" stripe border>
          <el-table-column prop="flowDate" label="Ngày" width="120" />
          <el-table-column prop="type" label="Loại" width="100">
            <template #default="{ row }">
              <el-tag :type="row.type === 'DEPOSIT' ? 'success' : 'danger'">
                {{ row.type === 'DEPOSIT' ? 'Nạp' : 'Rút' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="Số tiền" align="right"
            :formatter="(r) => formatVND(r.amount)" />
          <el-table-column prop="note" label="Ghi chú" />
        </el-table>
      </el-card>
    </template>

    <el-empty v-else-if="!pStore.loading" description="Chọn tài khoản để xem dòng tiền" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePortfolioStore } from '@/stores/portfolio'
import { useTransactionStore } from '@/stores/transaction'
import { storeToRefs } from 'pinia'
import { ElMessage } from 'element-plus'

const pStore = usePortfolioStore()
const tStore = useTransactionStore()
const { cashSummary } = storeToRefs(pStore)

const accounts = ref([])
const selectedAccountId = ref(null)
const submitting = ref(false)
const cfFormRef = ref(null)

const cfForm = ref({ type: 'DEPOSIT', amount: 0, flowDate: null, note: '' })
const cfRules = {
  type:     [{ required: true }],
  amount:   [{ required: true, type: 'number', min: 1, message: 'Số tiền phải > 0' }],
  flowDate: [{ required: true, message: 'Chọn ngày' }]
}

onMounted(async () => {
  await tStore.loadRefData()
  accounts.value = tStore.accounts
})

async function loadData() {
  if (!selectedAccountId.value) return
  await pStore.fetchCashSummary(selectedAccountId.value)
  await pStore.fetchCashFlows(selectedAccountId.value)
}

async function submitCashFlow() {
  await cfFormRef.value.validate()
  submitting.value = true
  try {
    await pStore.addCashFlow({ ...cfForm.value, accountId: selectedAccountId.value })
    ElMessage.success('Ghi nhận thành công')
    cfForm.value = { type: 'DEPOSIT', amount: 0, flowDate: null, note: '' }
    await loadData()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || 'Có lỗi xảy ra')
  } finally {
    submitting.value = false
  }
}

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<style scoped>
.selector-card, .summary-row, .form-card { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 18px; font-weight: 700; }
.highlight { color: #409eff; }
.profit { color: #2ecc71; }
.loss   { color: #e74c3c; }
</style>
