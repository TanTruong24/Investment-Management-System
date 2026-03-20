<template>
  <div>
    <!-- Bộ lọc -->
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="Mã CP">
          <el-select v-model="filters.tickerSymbol" placeholder="Tất cả" clearable filterable style="width:220px">
            <el-option
              v-for="t in store.tickers"
              :key="t.id"
              :label="`${t.symbol} - ${t.name}`"
              :value="t.symbol"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="Loại GD">
          <el-select v-model="filters.trade" placeholder="Tất cả" clearable style="width:120px">
            <el-option label="Mua" value="BUY" />
            <el-option label="Bán" value="SELL" />
          </el-select>
        </el-form-item>
        <el-form-item label="Từ ngày">
          <el-date-picker v-model="filters.fromDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="Đến ngày">
          <el-date-picker v-model="filters.toDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="Tài khoản">
          <el-select v-model="filters.accountId" placeholder="Tất cả" clearable style="width:160px">
            <el-option v-for="a in store.accounts" :key="a.id" :label="a.name" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search"><el-icon><Search /></el-icon> Tìm</el-button>
          <el-button @click="resetFilters">Reset</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Toolbar -->
    <div class="toolbar">
      <el-button type="primary" @click="$router.push('/transactions/new')">
        <el-icon><Plus /></el-icon> Tạo GD
      </el-button>
    </div>

    <!-- Bảng dữ liệu -->
    <el-card shadow="never">
      <el-table :data="store.transactions" v-loading="store.loading"
                @sort-change="onSortChange" stripe border>
        <el-table-column prop="tradingDate" label="Ngày GD" sortable="custom" width="110" />
        <el-table-column prop="tickerSymbol" label="Mã CP" width="80">
          <template #default="{ row }">
            <el-tag>{{ row.tickerSymbol }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="trade" label="Loại" width="70">
          <template #default="{ row }">
            <el-tag :type="row.trade === 'BUY' ? 'success' : 'danger'">
              {{ row.trade === 'BUY' ? 'Mua' : 'Bán' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="matchedVolume" label="KL khớp" width="100" align="right"
                         :formatter="(r) => r.matchedVolume?.toLocaleString('vi-VN')" />
        <el-table-column prop="matchedValue" label="Giá khớp" width="120" align="right"
                         :formatter="(r) => formatVND(r.matchedValue)" />
        <el-table-column prop="fee" label="Phí" width="100" align="right"
                         :formatter="(r) => formatVND(r.fee)" />
        <el-table-column prop="tax" label="Thuế" width="100" align="right"
                         :formatter="(r) => formatVND(r.tax)" />
        <el-table-column prop="cost" label="Giá vốn" width="130" align="right"
                         :formatter="(r) => formatVND(r.cost)" />
        <el-table-column prop="returnAmount" label="Lãi lỗ" width="130" align="right">
          <template #default="{ row }">
            <span :class="row.returnAmount >= 0 ? 'profit' : 'loss'">
              {{ formatVND(row.returnAmount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="brokerCode" label="Broker" width="80" />
        <el-table-column prop="status" label="Trạng thái" width="100">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Thao tác" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/transactions/${row.id}/edit`)">
              Sửa
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Phân trang -->
      <el-pagination class="pagination"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="store.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="search"
        @current-change="search" />
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useTransactionStore } from '@/stores/transaction'

const store = useTransactionStore()

const filters = reactive({ tickerSymbol: null, trade: null, fromDate: null, toDate: null, accountId: null })
const currentPage = ref(1)
const pageSize = ref(20)
const sortField = ref('tradingDate')
const sortOrder = ref('desc')

onMounted(async () => {
  await store.loadRefData()
  await search()
})

async function search() {
  await store.fetchTransactions({
    ...filters,
    page: currentPage.value - 1,
    size: pageSize.value,
    sort: `${sortField.value},${sortOrder.value}`
  })
}

function resetFilters() {
  Object.assign(filters, { tickerSymbol: null, trade: null, fromDate: null, toDate: null, accountId: null })
  search()
}

function onSortChange({ prop, order }) {
  sortField.value = prop || 'tradingDate'
  sortOrder.value = order === 'ascending' ? 'asc' : 'desc'
  search()
}

function formatVND(val) {
  if (val == null) return '—'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<style scoped>
.filter-card { margin-bottom: 12px; }
.toolbar { display: flex; gap: 8px; margin-bottom: 12px; }
.pagination { margin-top: 16px; justify-content: flex-end; }
.profit { color: #2ecc71; font-weight: 600; }
.loss   { color: #e74c3c; font-weight: 600; }
</style>
