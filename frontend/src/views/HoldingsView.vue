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
        <el-col :span="8">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Giá trị đầu tư</div>
            <div class="stat-value">{{ formatVND(summary.totalInvested) }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Giá trị thị trường</div>
            <div class="stat-value highlight">{{ formatVND(summary.currentValue) }}</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" class="stat-card">
            <div class="stat-label">Lãi/Lỗ chưa thực hiện</div>
            <div class="stat-value" :class="summary.unrealizedPnL >= 0 ? 'profit' : 'loss'">
              {{ formatVND(summary.unrealizedPnL) }}
              <span class="pct">({{ formatPct(summary.returnRate) }})</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- Holdings table -->
      <el-card shadow="never">
        <template #header>Danh mục đang nắm giữ</template>
        <el-table :data="summary.holdings" stripe border v-loading="pStore.loading">
          <el-table-column prop="tickerSymbol" label="Mã CP" width="90">
            <template #default="{ row }">
              <el-tag>{{ row.tickerSymbol }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="tickerName" label="Tên" min-width="160" />
          <el-table-column prop="exchange" label="Sàn" width="80" />
          <el-table-column prop="holdingVolume" label="KL nắm" width="100" align="right"
            :formatter="(r) => r.holdingVolume?.toLocaleString('vi-VN')" />
          <el-table-column prop="avgCost" label="Giá vốn TB" width="130" align="right"
            :formatter="(r) => formatVND(r.avgCost)" />
          <el-table-column prop="currentPrice" label="Giá hiện tại" width="130" align="right"
            :formatter="(r) => formatVND(r.currentPrice)" />
          <el-table-column prop="marketValue" label="Giá trị TT" width="140" align="right"
            :formatter="(r) => formatVND(r.marketValue)" />
          <el-table-column label="Lãi/Lỗ" width="160" align="right">
            <template #default="{ row }">
              <div :class="row.unrealizedPnL >= 0 ? 'profit' : 'loss'">
                {{ formatVND(row.unrealizedPnL) }}
              </div>
              <div class="pct" :class="row.unrealizedPnLPct >= 0 ? 'profit' : 'loss'">
                {{ formatPct(row.unrealizedPnLPct) }}
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>

    <el-empty v-else-if="!pStore.loading" description="Chọn tài khoản để xem danh mục" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { usePortfolioStore } from '@/stores/portfolio'
import { useTransactionStore } from '@/stores/transaction'
import { storeToRefs } from 'pinia'

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

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
function formatPct(val) {
  if (val == null) return ''
  return (val >= 0 ? '+' : '') + val.toFixed(2) + '%'
}
</script>

<style scoped>
.selector-card, .summary-row { margin-bottom: 16px; }
.stat-card { text-align: center; padding: 8px 0; }
.stat-label { font-size: 13px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 700; }
.highlight { color: #409eff; }
.profit { color: #2ecc71; }
.loss   { color: #e74c3c; }
.pct    { font-size: 12px; }
</style>
