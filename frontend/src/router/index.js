import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/transactions'
    },
    {
      path: '/transactions',
      component: () => import('@/views/TransactionListView.vue'),
      meta: { title: 'Danh sách Giao dịch' }
    },
    {
      path: '/transactions/new',
      component: () => import('@/views/TransactionFormView.vue'),
      meta: { title: 'Tạo Giao dịch' }
    },
    {
      path: '/transactions/:id/edit',
      component: () => import('@/views/TransactionFormView.vue'),
      meta: { title: 'Sửa Giao dịch' }
    },
    {
      path: '/holdings',
      component: () => import('@/views/HoldingsView.vue'),
      meta: { title: 'Tài sản đang giữ' }
    },
    {
      path: '/pnl',
      component: () => import('@/views/PnLView.vue'),
      meta: { title: 'Lãi Lỗ (PnL)' }
    },
    {
      path: '/cash',
      component: () => import('@/views/CashView.vue'),
      meta: { title: 'Quản lý Tiền mặt' }
    },
    {
      path: '/prices',
      component: () => import('@/views/PriceUpdateView.vue'),
      meta: { title: 'Cập nhật Giá' }
    },
    {
      path: '/imports/stock-history',
      component: () => import('@/views/StockTransactionHistoryImportView.vue'),
      meta: { title: 'Import lịch sử giao dịch cổ phiếu' }
    },
    {
      path: '/imports/fund-history',
      component: () => import('@/views/FundTransactionHistoryImportView.vue'),
      meta: { title: 'Import lịch sử giao dịch quỹ' }
    },
    {
      path: '/imports/cash-statement',
      component: () => import('@/views/CashStatementImportView.vue'),
      meta: { title: 'Import sao kê tiền' }
    },
    {
      path: '/imports/stock-statement',
      component: () => import('@/views/StockStatementImportView.vue'),
      meta: { title: 'Import sao kê cổ phiếu' }
    },
    {
      path: '/ref/tickers',
      component: () => import('@/views/TickerManageView.vue'),
      meta: { title: 'Quản lý Mã CP' }
    },
    {
      path: '/ref/brokers',
      component: () => import('@/views/BrokerManageView.vue'),
      meta: { title: 'Quản lý Broker' }
    },
    {
      path: '/ref/accounts',
      component: () => import('@/views/AccountManageView.vue'),
      meta: { title: 'Quản lý Tài khoản' }
    }
  ]
})

export default router
