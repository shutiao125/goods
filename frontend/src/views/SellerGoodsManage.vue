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
          <router-link to="/seller/goods" class="nav-link active">
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
        <h1 class="page-title">📦 商品管理</h1>
        <div class="header-actions">
          <router-link to="/goods/publish" class="btn btn-primary">➕ 发布新商品</router-link>
        </div>
      </div>

      <div class="filter-section">
        <div class="filter-form">
          <div class="form-group">
            <label>搜索关键词</label>
            <input type="text" v-model="filters.keyword" placeholder="输入商品名称..." class="form-control" @keyup.enter="searchGoods">
          </div>
          <button @click="searchGoods" class="btn btn-success">🔍 搜索</button>
          <button @click="resetFilters" class="btn btn-secondary">🔄 重置</button>
        </div>
      </div>

      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <div v-else-if="goods.length > 0" class="table-container">
        <table class="goods-table">
          <thead>
            <tr>
              <th>商品信息</th>
              <th>价格</th>
              <th>库存</th>
              <th>销量</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in goods" :key="item.id">
              <td>
                <div class="goods-info">
                  <img :src="item.mainImage || '/img/placeholder.jpg'" :alt="item.name" class="goods-image" @error="handleImageError">
                  <div class="goods-name">{{ item.name }}</div>
                </div>
              </td>
              <td>
                <div class="price">¥{{ item.price }}</div>
                <div v-if="item.originalPrice > item.price" class="original-price">¥{{ item.originalPrice }}</div>
              </td>
              <td>
                <span :class="['stock', getStockClass(item.stock)]">{{ item.stock }}</span>
              </td>
              <td>{{ item.salesCount || 0 }}</td>
              <td>
                <span :class="['status-badge', getStatusClass(item.status)]">{{ getStatusText(item.status) }}</span>
              </td>
              <td>
                <div class="action-buttons">
                  <button @click="editGoods(item)" class="btn btn-sm btn-info">✏️ 编辑</button>
                  <button @click="toggleStatus(item)" class="btn btn-sm btn-warning">
                    {{ item.status === 1 ? '⏸️ 下架' : '✅ 上架' }}
                  </button>
                  <button @click="deleteGoods(item)" class="btn btn-sm btn-danger">🗑️ 删除</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination-wrapper">
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
      </div>

      <div v-else class="empty-state">
        <div class="empty-icon">📦</div>
        <h3>暂无商品数据</h3>
        <p>{{ hasActiveFilters ? '没有找到符合条件的商品' : '您还没有发布任何商品，点击上方按钮发布吧！' }}</p>
        <router-link v-if="!hasActiveFilters" to="/goods/publish" class="btn btn-primary">发布第一个商品</router-link>
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
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: ''
})

const hasActiveFilters = computed(() => {
  return filters.keyword
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
})

async function fetchGoods() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...filters
    }
    const res = await request.get('/seller/goods/page', { params })
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

function searchGoods() {
  currentPage.value = 1
  fetchGoods()
}

function resetFilters() {
  filters.keyword = ''
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

function getStockClass(stock) {
  if (stock <= 0) return 'stock-empty'
  if (stock <= 10) return 'stock-low'
  return 'stock-normal'
}

function handleImageError(e) {
  e.target.src = '/img/placeholder.jpg'
}

function editGoods(item) {
  router.push(`/seller/goods/edit/${item.id}`)
}

async function toggleStatus(item) {
  const action = item.status === 1 ? '下架' : '上架'
  try {
    await ElMessageBox.confirm(`确定要${action}该商品吗？`, '确认操作', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await request.put(`/seller/goods/${item.id}/status`, { status: item.status === 1 ? 0 : 1 })
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
    
    await request.delete(`/seller/goods/${item.id}`)
    ElMessage.success('删除成功 🗑️')
    fetchGoods()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败，请重试')
    }
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

.btn-success {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-success:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(40, 167, 69, 0.4);
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

.btn-sm {
  padding: 6px 14px;
  font-size: 13px;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover {
  background: #138496;
  transform: translateY(-2px);
}

.btn-warning {
  background: #ffc107;
  color: #212529;
}

.btn-warning:hover {
  background: #e0a800;
  transform: translateY(-2px);
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
  transform: translateY(-2px);
}

.filter-section {
  background: white;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  margin-bottom: 20px;
}

.filter-form {
  display: grid;
  grid-template-columns: 1fr auto auto;
  gap: 15px;
  align-items: end;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  margin-bottom: 6px;
}

.form-control {
  padding: 10px 14px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
}

.form-control:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.loading-state {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow-x: auto;
}

.goods-table {
  width: 100%;
  border-collapse: collapse;
}

.goods-table th,
.goods-table td {
  padding: 16px 14px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.goods-table th {
  background: #f8f9fa;
  font-weight: 600;
  color: #495057;
  font-size: 13px;
  text-transform: uppercase;
}

.goods-table tr:hover {
  background: #f8f9fa;
}

.goods-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.goods-image {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  flex-shrink: 0;
}

.goods-name {
  font-weight: 500;
  color: #2c3e50;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.price {
  color: #dc3545;
  font-weight: 600;
  font-size: 16px;
}

.original-price {
  font-size: 12px;
  color: #999;
  text-decoration: line-through;
  margin-top: 2px;
}

.stock {
  font-weight: 600;
  font-size: 14px;
}

.stock-normal {
  color: #28a745;
}

.stock-low {
  color: #ffc107;
}

.stock-empty {
  color: #dc3545;
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

.action-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-top: 1px solid #f0f0f0;
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

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
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
  margin-bottom: 20px;
}
</style>