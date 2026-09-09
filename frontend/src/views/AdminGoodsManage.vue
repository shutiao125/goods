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
        <router-link to="/admin/goods" class="nav-item active">
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
        <h1 class="page-title">📦 商品管理</h1>
        <div class="user-info">
          <div class="user-avatar">{{ userStore.userInfo.username?.charAt(0)?.toUpperCase() || 'A' }}</div>
          <button @click="handleLogout" class="btn-logout">🚪 退出登录</button>
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-form">
          <div class="form-group">
            <label class="form-label">搜索关键词</label>
            <input type="text" v-model="filters.keyword" placeholder="商品名称或描述..." class="form-input" @keyup.enter="searchGoods">
          </div>
          <div class="form-group">
            <label class="form-label">分类筛选</label>
            <select v-model="filters.categoryId" class="form-select">
              <option value="">全部分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">状态筛选</label>
            <select v-model="filters.status" class="form-select">
              <option value="">全部状态</option>
              <option value="1">在售</option>
              <option value="0">已下架</option>
              <option value="2">缺货</option>
            </select>
          </div>
          <button @click="searchGoods" class="btn-primary">🔍 搜索</button>
          <button @click="resetFilters" class="btn-secondary">🔄 重置</button>
        </div>
      </div>

      <div class="goods-grid">
        <div v-for="item in goods" :key="item.id" class="goods-card">
          <img :src="item.mainImage || '/img/placeholder.jpg'" :alt="item.name" class="card-image" @error="handleImageError">
          <div class="card-body">
            <h3 class="card-title">{{ item.name }}</h3>
            <p class="card-description">{{ item.description }}</p>
            <div class="card-price">
              <span class="price-current">{{ item.price }}</span>
              <span v-if="item.originalPrice > item.price" class="price-original">{{ item.originalPrice }}</span>
            </div>
            <div class="card-meta">
              <span class="meta-item">库存: {{ item.stock }}</span>
              <span class="meta-item">销量: {{ item.salesCount || 0 }}</span>
            </div>
            <div class="card-status">
              <span :class="['status-badge', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
            </div>
            <div class="card-actions">
              <button @click="editGoods(item)" class="btn-edit">✏️ 编辑</button>
              <button @click="toggleStatus(item)" class="btn-toggle">
                {{ item.status === 1 ? '⏸️ 下架' : '✅ 上架' }}
              </button>
              <button @click="deleteGoods(item)" class="btn-delete">🗑️ 删除</button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="goods.length === 0 && !loading" class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>暂无商品数据</h3>
        <p>{{ hasActiveFilters ? '没有找到符合条件的商品' : '还没有任何商品' }}</p>
      </div>

      <div v-if="total > pageSize" class="pagination-wrapper">
        <div class="pagination-info">
          共 {{ total }} 条记录，第 {{ currentPage }} / {{ totalPages }} 页
        </div>
        <div class="pagination">
          <a href="#" class="page-btn" :class="{ disabled: currentPage === 1 }" @click.prevent="changePage(currentPage - 1)">‹ 上一页</a>
          <template v-for="page in visiblePages" :key="page">
            <a v-if="page !== '...'" href="#" class="page-btn" :class="{ active: page === currentPage }" @click.prevent="changePage(page)">{{ page }}</a>
            <span v-else class="page-btn disabled">...</span>
          </template>
          <a href="#" class="page-btn" :class="{ disabled: currentPage === totalPages }" @click.prevent="changePage(currentPage + 1)">下一页 ›</a>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const goods = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

const filters = reactive({
  keyword: '',
  categoryId: '',
  status: ''
})

const hasActiveFilters = computed(() => {
  return filters.keyword || filters.categoryId || filters.status
})

const totalPages = computed(() => {
  return Math.ceil(total.value / pageSize.value)
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value
  
  if (total <= 7) {
    for (let i = 1; i <= total; i++) pages.push(i)
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...', total)
    } else if (current >= total - 3) {
      pages.push(1, '...')
      for (let i = total - 4; i <= total; i++) pages.push(i)
    } else {
      pages.push(1, '...', current - 1, current, current + 1, '...', total)
    }
  }
  
  return pages
})

onMounted(async () => {
  await fetchGoods()
  await fetchCategories()
})

async function fetchGoods() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...filters
    }
    const res = await request.get('/admin/goods/page', { params })
    if (res.data) {
      goods.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await request.get('/categories')
    if (res.data) {
      categories.value = res.data
    }
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

function searchGoods() {
  currentPage.value = 1
  fetchGoods()
}

function resetFilters() {
  filters.keyword = ''
  filters.categoryId = ''
  filters.status = ''
  currentPage.value = 1
  fetchGoods()
}

function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  fetchGoods()
}

function getStatusClass(status) {
  const classes = {
    1: 'status-active',
    0: 'status-inactive',
    2: 'status-outofstock'
  }
  return classes[status] || 'status-inactive'
}

function getStatusText(status) {
  const texts = {
    1: '在售',
    0: '已下架',
    2: '缺货'
  }
  return texts[status] || '未知'
}

function handleImageError(e) {
  e.target.src = '/img/placeholder.jpg'
}

function editGoods(item) {
  router.push(`/admin/goods/edit/${item.id}`)
}

async function toggleStatus(item) {
  const action = item.status === 1 ? '下架' : '上架'
  try {
    await ElMessageBox.confirm(`确定要${action}该商品吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.put(`/admin/goods/${item.id}/status`, { status: item.status === 1 ? 0 : 1 })
    ElMessage.success(`商品已${action} ✅`)
    fetchGoods()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败，请重试`)
    }
  }
}

async function deleteGoods(item) {
  try {
    await ElMessageBox.confirm(
      `确定要删除"${item.name}"吗？此操作不可恢复！`,
      '⚠️ 危险操作',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    await request.delete(`/admin/goods/${item.id}`)
    ElMessage.success('删除成功 🗑️')
    fetchGoods()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请重试')
    }
  }
}

async function handleLogout() {
  try {
    await userStore.logout()
    router.push('/login')
  } catch (error) {
    ElMessage.error('退出失败')
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
  gap: 16px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.btn-logout {
  padding: 10px 20px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  text-decoration: none;
}

.btn-logout:hover {
  background: #c82333;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220,53,69,0.3);
}

.filter-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.filter-form {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto auto;
  gap: 16px;
  align-items: end;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  font-weight: 500;
  color: #495057;
}

.form-input,
.form-select {
  padding: 10px 14px;
  border: 1.5px solid #e9ecef;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.form-input:focus,
.form-select:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 4px rgba(102,126,234,0.1);
}

.btn-primary,
.btn-secondary {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 42px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102,126,234,0.3);
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102,126,234,0.4);
}

.btn-secondary {
  background: white;
  color: #495057;
  border: 1.5px solid #dee2e6;
}

.btn-secondary:hover {
  background: #f8f9fa;
  transform: translateY(-1px);
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
  margin-bottom: 24px;
}

.goods-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e9ecef;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.goods-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  transform: translateY(-4px);
  border-color: #667eea;
}

.card-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
  background: #f8f9fa;
}

.card-body {
  padding: 16px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 44px;
}

.card-description {
  font-size: 13px;
  color: #6c757d;
  line-height: 1.5;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  height: 39px;
}

.card-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 0;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
}

.price-current {
  font-size: 22px;
  color: #dc3545;
  font-weight: 600;
}

.price-current::before {
  content: '¥';
  font-size: 14px;
  margin-right: 2px;
}

.price-original {
  font-size: 14px;
  color: #999;
  text-decoration: line-through;
}

.price-original::before {
  content: '¥';
  margin-right: 2px;
}

.card-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #6c757d;
  margin-bottom: 12px;
}

.meta-item {
  font-weight: 500;
}

.card-status {
  margin-bottom: 12px;
}

.status-badge {
  display: inline-block;
  padding: 5px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
}

.status-active {
  background: #d4edda;
  color: #155724;
}

.status-inactive {
  background: #f8d7da;
  color: #721c24;
}

.status-outofstock {
  background: #fff3cd;
  color: #856404;
}

.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.btn-edit,
.btn-toggle,
.btn-delete {
  padding: 6px 14px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-edit {
  background: #17a2b8;
  color: white;
}

.btn-edit:hover {
  background: #138496;
  transform: translateY(-2px);
}

.btn-toggle {
  background: #ffc107;
  color: #212529;
}

.btn-toggle:hover {
  background: #e0a800;
  transform: translateY(-2px);
}

.btn-delete {
  background: #dc3545;
  color: white;
}

.btn-delete:hover {
  background: #c82333;
  transform: translateY(-2px);
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  color: #495057;
  margin-bottom: 8px;
}

.empty-state p {
  font-size: 14px;
  color: #6c757d;
}

.pagination-wrapper {
  background: white;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.pagination-info {
  font-size: 14px;
  color: #6c757d;
}

.pagination {
  display: flex;
  gap: 6px;
  list-style: none;
}

.page-btn {
  padding: 8px 14px;
  border: 1px solid #dee2e6;
  border-radius: 6px;
  color: #495057;
  text-decoration: none;
  font-size: 13px;
  transition: all 0.3s ease;
  background: white;
}

.page-btn:hover:not(.disabled):not(.active) {
  background: #f8f9fa;
  border-color: #667eea;
  color: #667eea;
}

.page-btn.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
}

.page-btn.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>