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
        <el-form-item>
          <el-button type="primary" :disabled="!selectedAccountId" @click="loadData">
            <el-icon><Refresh /></el-icon> Làm mới
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <template v-if="summary">
      <!-- Summary cards -->
      <el-row :gutter="16" class="summary-row">
        <el-col :span="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Lãi thực hiện</div>
            <div class="stat-value" :class="summary.realizedPnL >= 0 ? 'profit' : 'loss'">
              {{ formatVND(summary.realizedPnL) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Lãi chưa thực hiện</div>
            <div class="stat-value" :class="summary.unrealizedPnL >= 0 ? 'profit' : 'loss'">
              {{ formatVND(summary.unrealizedPnL) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Tổng lãi/lỗ</div>
            <div class="stat-value" :class="summary.totalPnL >= 0 ? 'profit' : 'loss'">
              {{ formatVND(summary.totalPnL) }}
            </div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Tỉ suất sinh lời</div>
            <div class="stat-value" :class="summary.returnRate >= 0 ? 'profit' : 'loss'">
              {{ formatPct(summary.returnRate) }}
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Charts row -->
      <el-row :gutter="16" class="chart-row">
        <!-- Allocation pie chart -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>Cơ cấu danh mục</template>
            <div class="chart-container">
              <Pie v-if="pieData" :data="pieData" :options="pieOptions" />
            </div>
          </el-card>
        </el-col>

        <!-- PnL bar chart per ticker -->
        <el-col :span="12">
          <el-card shadow="never">
            <template #header>Lãi/Lỗ theo mã CK</template>
            <div class="chart-container">
              <Bar v-if="barData" :data="barData" :options="barOptions" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-else-if="!pStore.loading" description="Chọn tài khoản để xem báo cáo" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Pie, Bar } from 'vue-chartjs'
import {
  Chart as ChartJS, ArcElement, Tooltip, Legend,
  CategoryScale, LinearScale, BarElement, Title
} from 'chart.js'
import { usePortfolioStore } from '@/stores/portfolio'
import { useTransactionStore } from '@/stores/transaction'
import { storeToRefs } from 'pinia'

ChartJS.register(ArcElement, Tooltip, Legend, CategoryScale, LinearScale, BarElement, Title)

const pStore = usePortfolioStore()
const tStore = useTransactionStore()
const { summary } = storeToRefs(pStore)
const accounts = ref([])
const selectedAccountId = ref(null)

onMounted(async () => {
  await tStore.loadRefData()
  accounts.value = tStore.accounts
})

async function loadData() {
  if (!selectedAccountId.value) return
  await pStore.fetchPortfolio(selectedAccountId.value)
}

const COLORS = ['#409eff','#67c23a','#e6a23c','#f56c6c','#909399','#9b59b6','#1abc9c','#e74c3c']

const pieData = computed(() => {
  if (!summary.value?.allocation?.length) return null
  return {
    labels: summary.value.allocation.map(a => a.label),
    datasets: [{
      data: summary.value.allocation.map(a => a.value),
      backgroundColor: COLORS
    }]
  }
})

const pieOptions = { responsive: true, plugins: { legend: { position: 'right' } } }

const barData = computed(() => {
  if (!summary.value?.holdings?.length) return null
  const holdings = summary.value.holdings
  return {
    labels: holdings.map(h => h.tickerSymbol),
    datasets: [{
      label: 'Lãi/Lỗ chưa TH (VND)',
      data: holdings.map(h => h.unrealizedPnL),
      backgroundColor: holdings.map(h => h.unrealizedPnL >= 0 ? '#2ecc71' : '#e74c3c')
    }]
  }
})

const barOptions = {
  responsive: true,
  plugins: { legend: { display: false } },
  scales: { y: { ticks: { callback: v => (v / 1e6).toFixed(1) + 'M' } } }
}

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
function formatPct(val) {
  if (val == null) return '—'
  return (val >= 0 ? '+' : '') + val.toFixed(2) + '%'
}
</script>

<style scoped>
.selector-card, .summary-row, .chart-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 700; }
.profit { color: #2ecc71; }
.loss   { color: #e74c3c; }
.chart-container { height: 280px; display: flex; align-items: center; justify-content: center; }
</style>
