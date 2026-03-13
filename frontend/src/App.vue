<template>
  <el-container class="app-layout">
    <el-aside width="220px" class="sidebar">
      <div class="logo">📈 Go Stock</div>
      <el-menu router :default-active="$route.path" background-color="#1a2035" text-color="#a0aec0" active-text-color="#7eb3ff">
        <el-menu-item index="/transactions">
          <el-icon><List /></el-icon>
          <span>Giao dịch</span>
        </el-menu-item>
        <el-menu-item index="/holdings">
          <el-icon><Wallet /></el-icon>
          <span>Tài sản đang giữ</span>
        </el-menu-item>
        <el-menu-item index="/pnl">
          <el-icon><TrendCharts /></el-icon>
          <span>Lãi lỗ (PnL)</span>
        </el-menu-item>
        <el-menu-item index="/cash">
          <el-icon><Money /></el-icon>
          <span>Tiền mặt</span>
        </el-menu-item>
        <el-menu-item index="/prices">
          <el-icon><DataLine /></el-icon>
          <span>Cập nhật giá</span>
        </el-menu-item>
        <el-sub-menu index="imports">
          <template #title>
            <el-icon><Upload /></el-icon>
            <span>Import sao kê</span>
          </template>
          <el-menu-item index="/imports/stock-history">Lịch sử GD cổ phiếu</el-menu-item>
          <el-menu-item index="/imports/fund-history">Lịch sử GD quỹ</el-menu-item>
          <el-menu-item index="/imports/cash-statement">Sao kê tiền</el-menu-item>
          <el-menu-item index="/imports/stock-statement">Sao kê cổ phiếu</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="ref">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>Danh mục</span>
          </template>
          <el-menu-item index="/ref/tickers">Mã chứng khoán</el-menu-item>
          <el-menu-item index="/ref/brokers">Broker</el-menu-item>
          <el-menu-item index="/ref/accounts">Tài khoản</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span class="page-title">{{ $route.meta.title || 'Go Stock' }}</span>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style>
html, body { margin: 0; font-family: 'Inter', sans-serif; background: #f0f2f5; }
.app-layout { height: 100vh; }
.sidebar { background: #1a2035; overflow: hidden; }
.logo { color: #fff; font-size: 20px; font-weight: bold; padding: 24px 20px; }
.header { background: #fff; display: flex; align-items: center; border-bottom: 1px solid #e8e8e8; }
.page-title { font-size: 18px; font-weight: 600; }
.el-main { padding: 20px; }
</style>
