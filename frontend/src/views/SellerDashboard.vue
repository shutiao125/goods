<template>
  <div class="seller-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="logo">🛍️ 卖家中心</h1>
        <p class="user-info">{{ userStore.userInfo.username || '卖家' }}</p>
      </div>
      <ul class="nav-menu">
        <li class="nav-item">
          <router-link to="/seller/dashboard" class="nav-link" :class="{ active: $route.path === '/seller/dashboard' }">
            <span class="nav-icon">📊</span>
            <span>数据概览</span>
          </router-link>
        </li>
        <li class="nav-item">
          <router-link to="/seller/goods" class="nav-link" :class="{ active: $route.path.startsWith('/seller/goods') }">
            <span class="nav-icon">📦</span>
            <span>商品管理</span>
          </router-link>
        </li>
        <li class="nav-item">
          <router-link to="/seller/orders" class="nav-link" :class="{ active: $route.path === '/seller/orders' }">
            <span class="nav-icon">🛒</span>
            <span>订单管理</span>
          </router-link>
        </li>
      </ul>
    </aside>

    <main class="main-content">
      <div class="page-header">
        <h1 class="page-title">📊 数据概览</h1>
        <div class="header-actions">
          <router-link to="/goods/publish" class="btn btn-primary">➕ 发布商品</router-link>
          <button @click="handleLogout" class="btn btn-logout">🚪 退出登录</button>
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-header">
            <div>
              <div class="stat-label">我的商品数</div>
              <div class="stat-value">{{ stats.totalGoods || 0 }}</div>
            </div>
            <div class="stat-icon icon-purple">📦</div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend trend-up">↑ 12%</span>
            <span class="stat-desc">较上月增长</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div>
              <div class="stat-label">总销量</div>
              <div class="stat-value">{{ stats.totalSales || 0 }}</div>
            </div>
            <div class="stat-icon icon-green">💰</div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend trend-up">↑ 25%</span>
            <span class="stat-desc">较上月增长</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div>
              <div class="stat-label">总收入</div>
              <div class="stat-value">¥{{ stats.totalRevenue || 0 }}</div>
            </div>
            <div class="stat-icon icon-blue">💵</div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend trend-up">↑ 18%</span>
            <span class="stat-desc">较上月增长</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div>
              <div class="stat-label">待处理订单</div>
              <div class="stat-value">{{ stats.pendingOrders || 0 }}</div>
            </div>
            <div class="stat-icon icon-orange">⏳</div>
          </div>
          <div class="stat-footer">
            <span class="stat-desc">需要及时处理</span>
          </div>
        </div>
      </div>

      <div class="content-grid">
        <div class="recent-orders">
          <h2 class="section-title">🕐 最近订单</h2>
          <div v-if="recentOrders.length > 0" class="order-list">
            <div v-for="order in recentOrders" :key="order.id" class="order-item">
              <div class="order-info">
                <div class="order-id">订单 #{{ order.id }}</div>
                <div class="order-time">{{ formatTime(order.createdAt) }}</div>
              </div>
              <div class="order-details">
                <span class="order-amount">¥{{ order.totalAmount }}</span>
                <span :class="['order-status', getOrderStatusClass(order.status)]">
                  {{ getOrderStatusText(order.status) }}
                </span>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p>暂无最近订单</p>
          </div>
        </div>

        <div class="top-goods">
          <h2 class="section-title">🏆 热销商品</h2>
          <div v-if="topGoods.length > 0" class="goods-list">
            <div v-for="(item, index) in topGoods" :key="item.id" class="goods-item">
              <div class="rank-badge" :class="{ 'rank-top': index < 3 }">{{ index + 1 }}</div>
              <img :src="item.mainImage || '/img/placeholder.jpg'" :alt="item.name" class="goods-image">
              <div class="goods-info">
                <div class="goods-name">{{ item.name }}</div>
                <div class="goods-sales">销量: {{ item.salesCount || 0 }}</div>
              </div>
              <div class="goods-price">¥{{ item.price }}</div>
            </div>
          </div>
          <div v-else class="empty-state">
            <p>暂无热销商品数据</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const stats = reactive({
  totalGoods: 0,
  totalSales: 0,
  totalRevenue: 0,
  pendingOrders: 0
})

const recentOrders = ref([])
const topGoods = ref([])

onMounted(async () => {
  await fetchSellerStats()
  await fetchRecentOrders()
  await fetchTopGoods()
})

async function fetchSellerStats() {
  try {
    const res = await request.get('/seller/stats')
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.error('获取卖家统计数据失败:', error)
  }
}

async function fetchRecentOrders() {
  try {
    const res = await request.get('/seller/orders/recent')
    if (res.data) {
      recentOrders.value = res.data
    }
  } catch (error) {
    console.error('获取最近订单失败:', error)
  }
}

async function fetchTopGoods() {
  try {
    const res = await request.get('/seller/goods/top')
    if (res.data) {
      topGoods.value = res.data
    }
  } catch (error) {
    console.error('获取热销商品失败:', error)
  }
}

function formatTime(time) {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function getOrderStatusClass(status) {
  const classes = {
    0: 'status-pending',
    1: 'status-paid',
    2: 'status-shipped',
    3: 'status-completed',
    4: 'status-cancelled'
  }
  return classes[status] || 'status-pending'
}

function getOrderStatusText(status) {
  const texts = {
    0: '待付款',
    1: '待发货',
    2: '已发货',
    3: '已完成',
    4: '已取消'
  }
  return texts[status] || '未知'
}

async function handleLogout() {
  try {
    await userStore.logout()
    ElMessage.success('已安全退出 👋')
    router.push('/login')
  } catch (error) {
    ElMessage.error('退出失败，请重试')
  }
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.seller-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 250px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px 0;
  position: fixed;
  height: 100vh;
  overflow-y: auto;
  box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  z-index: 1000;
}

.sidebar-header {
  text-align: center;
  padding: 30px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 10px;
}

.user-info {
  font-size: 14px;
  opacity: 0.9;
}

.nav-menu {
  list-style: none;
  padding: 20px 0;
}

.nav-item {
  margin: 5px 15px;
}

.nav-link {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: rgba(255, 255, 255, 0.85);
  text-decoration: none;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-size: 15px;
}

.nav-link:hover,
.nav-link.active {
  background: rgba(255, 255, 255, 0.15);
  color: white;
  transform: translateX(5px);
}

.nav-icon {
  margin-right: 12px;
  font-size: 18px;
}

.main-content {
  flex: 1;
  margin-left: 250px;
  padding: 30px;
  background-color: #f5f7fa;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: 600;
  color: #2c3e50;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-logout {
  background: #dc3545;
  color: white;
}

.btn-logout:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.4);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.stat-label {
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.icon-purple {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.icon-green {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.icon-blue {
  background: linear-gradient(135deg, #17a2b8 0%, #138496 100%);
}

.icon-orange {
  background: linear-gradient(135deg, #fd7e14 0%, #e67e22 100%);
}

.stat-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.stat-trend {
  font-size: 13px;
  font-weight: 600;
}

.trend-up {
  color: #28a745;
}

.trend-down {
  color: #dc3545;
}

.stat-desc {
  font-size: 13px;
  color: #6c757d;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.recent-orders,
.top-goods {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 20px;
}

.order-list,
.goods-list {
  max-height: 400px;
  overflow-y: auto;
}

.order-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #f0f0f0;
}

.order-item:last-child {
  border-bottom: none;
}

.order-info {
  flex: 1;
}

.order-id {
  font-size: 14px;
  font-weight: 600;
  color: #2c3e50;
  margin-bottom: 4px;
}

.order-time {
  font-size: 12px;
  color: #6c757d;
}

.order-details {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}

.order-amount {
  font-size: 16px;
  font-weight: 600;
  color: #dc3545;
}

.order-status {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-pending {
  background: #fff3cd;
  color: #856404;
}

.status-paid {
  background: #cce5ff;
  color: #004085;
}

.status-shipped {
  background: #d4edda;
  color: #155724;
}

.status-completed {
  background: #d1ecf1;
  color: #0c5460;
}

.status-cancelled {
  background: #f8d7da;
  color: #721c24;
}

.goods-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.goods-item:last-child {
  border-bottom: none;
}

.rank-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #e9ecef;
  color: #495057;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.rank-top {
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
  color: white;
}

.goods-image {
  width: 50px;
  height: 50px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-name {
  font-size: 14px;
  font-weight: 500;
  color: #2c3e50;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-sales {
  font-size: 12px;
  color: #6c757d;
}

.goods-price {
  font-size: 16px;
  font-weight: 600;
  color: #dc3545;
  flex-shrink: 0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #6c757d;
  font-size: 14px;
}
</style>