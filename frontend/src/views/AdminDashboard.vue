<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-header">
        <h1 class="sidebar-title">🎯 管理后台</h1>
        <p class="sidebar-subtitle">综合管理系统 v1.0</p>
      </div>
      <nav class="nav-menu">
        <router-link to="/admin/dashboard" class="nav-item" :class="{ active: $route.path === '/admin/dashboard' }">
          <span class="nav-icon">📊</span>
          <span>数据概览</span>
        </router-link>
        <router-link to="/admin/goods" class="nav-item" :class="{ active: $route.path === '/admin/goods' }">
          <span class="nav-icon">📦</span>
          <span>商品管理</span>
        </router-link>
        <router-link to="/admin/users" class="nav-item" :class="{ active: $route.path === '/admin/users' }">
          <span class="nav-icon">👥</span>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admin/orders" class="nav-item" :class="{ active: $route.path === '/admin/orders' }">
          <span class="nav-icon">🛒</span>
          <span>订单管理</span>
        </router-link>
        <router-link to="/admin/articles" class="nav-item" :class="{ active: $route.path === '/admin/articles' }">
          <span class="nav-icon">📝</span>
          <span>文章管理</span>
        </router-link>
        <router-link to="/admin/comments" class="nav-item" :class="{ active: $route.path === '/admin/comments' }">
          <span class="nav-icon">💬</span>
          <span>评论管理</span>
        </router-link>
      </nav>
    </aside>

    <main class="main-content">
      <div class="top-bar">
        <h1 class="page-title">📊 数据概览</h1>
        <div class="user-info">
          <div class="user-avatar">{{ userStore.userInfo.username?.charAt(0)?.toUpperCase() || 'A' }}</div>
          <span>{{ userStore.userInfo.username || '管理员' }}</span>
          <button @click="handleLogout" class="btn-logout">🚪 退出登录</button>
        </div>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">总用户数</div>
            <div class="stat-icon stat-icon-blue">👥</div>
          </div>
          <div class="stat-value">{{ stats.totalUsers || 0 }}</div>
          <div class="stat-trend trend-up">↑ 12% 较上月</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">总商品数</div>
            <div class="stat-icon stat-icon-green">📦</div>
          </div>
          <div class="stat-value">{{ stats.totalGoods || 0 }}</div>
          <div class="stat-trend trend-up">↑ 8% 较上月</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">总订单数</div>
            <div class="stat-icon stat-icon-orange">🛒</div>
          </div>
          <div class="stat-value">{{ stats.totalOrders || 0 }}</div>
          <div class="stat-trend trend-up">↑ 25% 较上月</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">总销售额</div>
            <div class="stat-icon stat-icon-red">💰</div>
          </div>
          <div class="stat-value">¥{{ stats.totalSales || 0 }}</div>
          <div class="stat-trend trend-up">↑ 18% 较上月</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">待处理订单</div>
            <div class="stat-icon stat-icon-blue">⏳</div>
          </div>
          <div class="stat-value">{{ stats.pendingOrders || 0 }}</div>
          <div class="stat-description">需要及时处理</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">今日新增用户</div>
            <div class="stat-icon stat-icon-green">✨</div>
          </div>
          <div class="stat-value">{{ stats.todayNewUsers || 0 }}</div>
          <div class="stat-description">活跃度良好</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">今日新增商品</div>
            <div class="stat-icon stat-icon-orange">🆕</div>
          </div>
          <div class="stat-value">{{ stats.todayNewGoods || 0 }}</div>
          <div class="stat-description">持续增长中</div>
        </div>

        <div class="stat-card">
          <div class="stat-header">
            <div class="stat-title">库存预警</div>
            <div class="stat-icon stat-icon-red">⚠️</div>
          </div>
          <div class="stat-value">{{ stats.lowStockCount || 0 }}</div>
          <div class="stat-description">需要补货</div>
        </div>
      </div>

      <div class="recent-section">
        <h2 class="section-title">🕐 最近操作记录</h2>
        <div class="activity-list">
          <div v-for="(activity, index) in recentActivities" :key="index" class="activity-item">
            <div class="activity-time">{{ activity.time }}</div>
            <div class="activity-content">{{ activity.content }}</div>
          </div>
          <div v-if="recentActivities.length === 0" class="empty-activities">
            暂无最近活动记录
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
  totalUsers: 0,
  totalGoods: 0,
  totalOrders: 0,
  totalSales: 0,
  pendingOrders: 0,
  todayNewUsers: 0,
  todayNewGoods: 0,
  lowStockCount: 0
})

const recentActivities = ref([])

onMounted(async () => {
  await fetchDashboardStats()
  await fetchRecentActivities()
})

async function fetchDashboardStats() {
  try {
    const res = await request.get('/admin/stats')
    if (res.data) {
      Object.assign(stats, res.data)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

async function fetchRecentActivities() {
  try {
    const res = await request.get('/admin/activities')
    if (res.data) {
      recentActivities.value = res.data
    }
  } catch (error) {
    console.error('获取活动记录失败:', error)
  }
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

.admin-layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 260px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  color: white;
  position: fixed;
  height: 100vh;
  overflow-y: auto;
  box-shadow: 2px 0 12px rgba(0,0,0,0.1);
  z-index: 1000;
}

.sidebar-header {
  padding: 30px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}

.sidebar-title {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 8px;
}

.sidebar-subtitle {
  font-size: 12px;
  opacity: 0.8;
}

.nav-menu {
  padding: 20px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 24px;
  color: white;
  text-decoration: none;
  transition: all 0.3s ease;
  font-size: 14px;
  border-left: 3px solid transparent;
}

.nav-item:hover {
  background: rgba(255,255,255,0.1);
  border-left-color: white;
}

.nav-item.active {
  background: rgba(255,255,255,0.15);
  border-left-color: white;
  font-weight: 600;
}

.nav-icon {
  font-size: 20px;
  width: 24px;
  text-align: center;
}

.main-content {
  flex: 1;
  margin-left: 260px;
  padding: 40px;
  background-color: #f8f9fa;
}

.top-bar {
  background: white;
  padding: 20px 30px;
  border-radius: 12px;
  margin-bottom: 30px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #212529;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: white;
  font-size: 16px;
}

.btn-logout {
  padding: 8px 16px;
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.btn-logout:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.stat-card {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}

.stat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.stat-title {
  font-size: 14px;
  color: #6c757d;
  font-weight: 500;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon-blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon-green {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.stat-icon-orange {
  background: linear-gradient(135deg, #ffc107 0%, #ff9800 100%);
}

.stat-icon-red {
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #212529;
  margin-bottom: 8px;
}

.stat-trend {
  font-size: 13px;
  font-weight: 500;
}

.trend-up {
  color: #28a745;
}

.trend-down {
  color: #dc3545;
}

.stat-description {
  font-size: 13px;
  color: #6c757d;
}

.recent-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 20px;
}

.activity-list {
  max-height: 400px;
  overflow-y: auto;
}

.activity-item {
  display: flex;
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.activity-item:last-child {
  border-bottom: none;
}

.activity-time {
  font-size: 13px;
  color: #6c757d;
  min-width: 120px;
  font-weight: 500;
}

.activity-content {
  font-size: 14px;
  color: #495057;
  line-height: 1.5;
}

.empty-activities {
  text-align: center;
  padding: 40px;
  color: #6c757d;
  font-size: 14px;
}
</style>