<template>
  <div class="goods-list-page">
    <div class="container">
      <!-- 页面头部 -->
      <div class="page-header">
        <h1>🛍️ 商品列表</h1>
        
        <!-- 用户信息区域 -->
        <div v-if="isLoggedIn" class="user-info-section">
          <div class="user-avatar">
            {{ userStore.userInfo.username?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="user-details">
            <span class="user-name">{{ userStore.userInfo.username }}</span>
            <span class="user-role">{{ getRoleText(userStore.userInfo.role) }}</span>
          </div>
          <button @click="handleLogout" class="btn-logout">
            🚪 退出
          </button>
        </div>

        <!-- 未登录提示 -->
        <div v-else class="login-prompt">
          <router-link to="/login" class="btn-login">
            🔐 登录
          </router-link>
        </div>
      </div>

      <!-- 操作按钮区域 -->
      <div class="header-actions">
        <router-link to="/goods/publish" v-if="isSellerOrAdmin" class="btn-publish">
          发布商品
        </router-link>
        <router-link to="/cart" class="btn-cart">
          🛒 购物车 ({{ cartCount }})
        </router-link>
      </div>

      <!-- 筛选区域 -->
      <div class="filter-section">
        <div class="filter-header">
          <span class="filter-icon">🔍</span>
          <span class="filter-title">筛选条件</span>
        </div>

        <div class="filter-form">
          <div class="filter-group">
            <label class="filter-label">关键词</label>
            <input 
              type="text" 
              v-model="filters.keyword"
              placeholder="搜索商品名称或描述..."
              class="filter-input"
              @keyup.enter="searchGoods"
            >
          </div>

          <div class="filter-group">
            <label class="filter-label">分类</label>
            <select v-model="filters.categoryId" class="filter-input">
              <option value="">全部分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>

          <div class="filter-group">
            <label class="filter-label">状态</label>
            <select v-model="filters.status" class="filter-input">
              <option value="">全部状态</option>
              <option value="1">在售</option>
              <option value="0">已下架</option>
              <option value="2">缺货</option>
            </select>
          </div>

          <button @click="searchGoods" class="btn-filter">
            🔍 搜索
          </button>
          
          <button @click="resetFilters" class="btn-reset">
            🔄 重置
          </button>
        </div>

        <!-- 当前筛选标签 -->
        <div v-if="hasActiveFilters" class="filter-tags">
          <span v-if="filters.keyword" class="filter-tag">
            <span class="filter-tag-label">关键词:</span>
            {{ filters.keyword }}
          </span>
          <span v-if="filters.categoryId" class="filter-tag">
            <span class="filter-tag-label">分类:</span>
            {{ getCategoryName(filters.categoryId) }}
          </span>
          <span v-if="filters.status !== ''" class="filter-tag">
            <span class="filter-tag-label">状态:</span>
            {{ getStatusText(filters.status) }}
          </span>
        </div>
      </div>

      <!-- 批量操作 -->
      <div v-if="goods.length > 0 && isSellerOrAdmin" class="batch-actions">
        <div class="batch-left">
          <label class="checkbox-all">
            <input type="checkbox" v-model="selectAll" @change="toggleSelectAll">
            全选
          </label>
          <span class="selected-count">
            已选 <strong>{{ selectedIds.length }}</strong> 件商品
          </span>
        </div>
        <div class="batch-right">
          <button 
            @click="batchDelete" 
            class="btn-batch-delete"
            :disabled="selectedIds.length === 0"
          >
            🗑️ 批量删除
          </button>
        </div>
      </div>

      <!-- 商品网格 -->
      <div v-if="goods.length > 0" class="goods-grid">
        <div 
          v-for="item in goods" 
          :key="item.id"
          class="goods-card"
          @click="goToDetail(item.id)"
        >
          <!-- 复选框（管理员/卖家） -->
          <input 
            v-if="isSellerOrAdmin"
            type="checkbox" 
            class="card-checkbox"
            :value="item.id"
            v-model="selectedIds"
            @click.stop
          >

          <!-- 商品图片 -->
          <div class="goods-image-wrapper">
            <img 
              :src="item.mainImage || '/img/placeholder.jpg'" 
              :alt="item.name"
              class="goods-image"
              @error="handleImageError"
            >
            
            <!-- 状态标签 -->
            <span :class="['status-tag', getStatusClass(item.status)]">
              {{ getStatusText(item.status) }}
            </span>

            <!-- 下载按钮 -->
            <button 
              v-if="item.mainImage"
              class="image-download-btn"
              @click.stop="downloadImage(item.mainImage)"
              title="下载图片"
            >
              ⬇️
            </button>
          </div>

          <!-- 商品信息 -->
          <div class="goods-info">
            <h3 class="goods-name">{{ item.name }}</h3>
            <p class="goods-description">{{ item.description }}</p>

            <!-- 价格 -->
            <div class="goods-price">
              <span class="price-current">{{ item.price }}</span>
              <span v-if="item.originalPrice > item.price" class="price-original">
                {{ item.originalPrice }}
              </span>
            </div>

            <!-- 元数据 -->
            <div class="goods-meta">
              <div class="meta-item">
                <span class="meta-label">库存</span>
                <span :class="['meta-value', getStockClass(item.stock)]">
                  {{ item.stock }}
                </span>
              </div>
              <div class="meta-item">
                <span class="meta-label">销量</span>
                <span class="meta-value">{{ item.salesCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <div class="empty-icon">📦</div>
        <h2>暂无商品</h2>
        <p>{{ hasActiveFilters ? '没有找到符合条件的商品' : '还没有发布任何商品' }}</p>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="5" animated />
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination-wrapper">
        <div class="pagination">
          <a 
            href="#"
            class="page-item"
            :class="{ disabled: currentPage === 1 }"
            @click.prevent="changePage(currentPage - 1)"
          >
            ‹ 上一页
          </a>

          <template v-for="page in visiblePages" :key="page">
            <a
              v-if="page !== '...'"
              href="#"
              class="page-item"
              :class="{ active: page === currentPage }"
              @click.prevent="changePage(page)"
            >
              {{ page }}
            </a>
            <span v-else class="page-item disabled">...</span>
          </template>

          <a
            href="#"
            class="page-item"
            :class="{ disabled: currentPage === totalPages }"
            @click.prevent="changePage(currentPage + 1)"
          >
            下一页 ›
          </a>
        </div>

        <div class="page-info">
          第 <strong>{{ currentPage }}</strong> / 共 <strong>{{ totalPages }}</strong> 页，
          总计 <strong>{{ total }}</strong> 条记录
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const router = useRouter()
const userStore = useUserStore()

const goods = ref([])
const categories = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)
const selectedIds = ref([])
const selectAll = ref(false)
const cartCount = ref(0)

const filters = ref({
  keyword: '',
  categoryId: '',
  status: ''
})

const isLoggedIn = computed(() => !!userStore.token)
const isSellerOrAdmin = computed(() => {
  const role = userStore.userInfo.role
  return role === 'SELLER' || role === 'ADMIN'
})

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

const hasActiveFilters = computed(() => {
  return filters.value.keyword || filters.value.categoryId || filters.value.status !== ''
})

const visiblePages = computed(() => {
  const pages = []
  const total = totalPages.value
  const current = currentPage.value

  if (total <= 7) {
    for (let i = 1; i <= total; i++) {
      pages.push(i)
    }
  } else {
    if (current <= 4) {
      for (let i = 1; i <= 5; i++) pages.push(i)
      pages.push('...')
      pages.push(total)
    } else if (current >= total - 3) {
      pages.push(1)
      pages.push('...')
      for (let i = total - 4; i <= total; i++) pages.push(i)
    } else {
      pages.push(1)
      pages.push('...')
      for (let i = current - 1; i <= current + 1; i++) pages.push(i)
      pages.push('...')
      pages.push(total)
    }
  }

  return pages
})

onMounted(() => {
  fetchGoods()
  fetchCategories()
  fetchCartCount()
})

watch(() => userStore.token, () => {
  fetchCartCount()
})

async function fetchGoods() {
  loading.value = true
  try {
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...filters.value
    }
    
    const res = await request.get('/goods/page', { params })
    
    if (res.data) {
      goods.value = res.data.records || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取商品失败:', error)
    ElMessage.error('获取商品列表失败')
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await request.get('/categories')
    categories.value = res.data || []
  } catch (error) {
    console.error('获取分类失败:', error)
  }
}

async function fetchCartCount() {
  try {
    if (!isLoggedIn.value) return
    
    const res = await request.get('/cart/count')
    cartCount.value = res.data || 0
  } catch (error) {
    console.log('获取购物车数量失败')
  }
}

function searchGoods() {
  currentPage.value = 1
  selectedIds.value = []
  selectAll.value = false
  fetchGoods()
}

function resetFilters() {
  filters.value = {
    keyword: '',
    categoryId: '',
    status: ''
  }
  searchGoods()
}

function changePage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
  fetchGoods()
}

function goToDetail(id) {
  router.push(`/goods/${id}`)
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
  ElMessage.success('已退出登录')
}

function toggleSelectAll() {
  if (selectAll.value) {
    selectedIds.value = goods.value.map(item => item.id)
  } else {
    selectedIds.value = []
  }
}

async function batchDelete() {
  if (selectedIds.value.length === 0) return

  try {
    await El.confirm(`确定要删除选中的 ${selectedIds.value.length} 个商品吗？`, '批量删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await request.post('/goods/batch-delete', selectedIds.value)
    ElMessage.success('删除成功')
    selectedIds.value = []
    selectAll.value = false
    fetchGoods()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

function downloadImage(url) {
  const link = document.createElement('a')
  link.href = url
  link.download = url.split('/').pop()
  link.target = '_blank'
  link.click()
}

function handleImageError(e) {
  e.target.src = '/img/placeholder.jpg'
}

function getStatusClass(status) {
  switch (status) {
    case 1: return 'status-on'
    case 0: return 'status-off'
    case 2: return 'status-out'
    default: return 'status-on'
  }
}

function getStatusText(status) {
  switch (status) {
    case 1: return '在售'
    case 0: return '已下架'
    case 2: return '缺货'
    default: return '未知'
  }
}

function getStockClass(stock) {
  if (stock > 50) return 'stock-high'
  if (stock > 10) return 'stock-medium'
  return 'stock-low'
}

function getRoleText(role) {
  const roleMap = {
    'ADMIN': '管理员',
    'SELLER': '卖家',
    'USER': '普通用户'
  }
  return roleMap[role] || '用户'
}

function getCategoryName(categoryId) {
  const cat = categories.value.find(c => c.id == categoryId)
  return cat ? cat.name : '未知分类'
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.goods-list-page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f8f9fa;
  padding: 40px 20px;
  color: #333;
  min-height: 100vh;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 2px solid #e9ecef;
  position: relative;
}

.page-header h1 {
  font-size: 32px;
  color: #212529;
  margin-bottom: 10px;
}

/* 用户信息区域 */
.user-info-section {
  position: absolute;
  top: 20px;
  right: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 12px 24px;
  border-radius: 25px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  z-index: 10;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #667eea;
  font-size: 16px;
  border: 2px solid white;
}

.user-details {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  color: white;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}

.user-role {
  font-size: 11px;
  opacity: 0.9;
}

.btn-logout {
  padding: 8px 16px;
  background: white;
  color: #667eea;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-logout:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 255, 255, 0.3);
}

.login-prompt {
  position: absolute;
  top: 20px;
  right: 20px;
}

.btn-login {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 25px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.btn-login:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 操作按钮 */
.header-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  padding-right: 200px;
}

.btn-publish,
.btn-cart {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  text-decoration: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

.btn-publish {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-publish::before {
  content: '+';
  font-size: 20px;
  font-weight: 300;
}

.btn-publish:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

.btn-cart {
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  color: white;
}

.btn-cart:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(40, 167, 69, 0.4);
}

/* 筛选区域 */
.filter-section {
  background: white;
  border-radius: 12px;
  border: 1px solid #e9ecef;
  padding: 24px;
  margin-bottom: 30px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}

.filter-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f1f3f5;
}

.filter-icon {
  font-size: 20px;
  color: #667eea;
}

.filter-title {
  font-size: 16px;
  font-weight: 600;
  color: #212529;
}

.filter-form {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr auto auto;
  gap: 16px;
  align-items: end;
}

.filter-group {
  display: flex;
  flex-direction: column;
}

.filter-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #495057;
  margin-bottom: 8px;
}

.filter-label::before {
  content: '';
  width: 3px;
  height: 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 2px;
}

.filter-input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #e9ecef;
  border-radius: 8px;
  font-size: 14px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.filter-input:hover {
  border-color: #dee2e6;
  background: white;
}

.filter-input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
}

.btn-filter {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 42px;
  display: flex;
  align-items: center;
  gap: 6px;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-filter:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-reset {
  padding: 10px 24px;
  background: white;
  color: #495057;
  border: 1.5px solid #dee2e6;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  height: 42px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.btn-reset:hover {
  background: #f8f9fa;
  border-color: #adb5bd;
  transform: translateY(-1px);
}

.filter-tags {
  display: flex;
  gap: 8px;
  margin-top: 16px;
  flex-wrap: wrap;
}

.filter-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f1f3f5;
  border-radius: 20px;
  font-size: 12px;
  color: #495057;
}

.filter-tag-label {
  font-weight: 500;
  color: #667eea;
}

/* 批量操作 */
.batch-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e9ecef;
  box-shadow: 0 2px 4px rgba(0,0,0,0.04);
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.checkbox-all {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #495057;
  cursor: pointer;
  user-select: none;
  padding: 6px 12px;
  border-radius: 6px;
  transition: all 0.2s;
}

.checkbox-all:hover {
  background: #f8f9fa;
}

.checkbox-all input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
  accent-color: #667eea;
}

.selected-count {
  font-size: 14px;
  color: #6c757d;
  padding: 6px 12px;
  background: #f8f9fa;
  border-radius: 6px;
}

.selected-count strong {
  color: #667eea;
  font-weight: 600;
  font-size: 16px;
}

.batch-right {
  display: flex;
  gap: 10px;
}

.btn-batch-delete {
  padding: 10px 20px;
  background: linear-gradient(135deg, #dc3545 0%, #c82333 100%);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 2px 8px rgba(220, 53, 69, 0.3);
}

.btn-batch-delete:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(220, 53, 69, 0.4);
}

.btn-batch-delete:disabled {
  background: #e9ecef;
  color: #adb5bd;
  cursor: not-allowed;
  box-shadow: none;
}

/* 商品网格 */
.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 32px;
  padding: 20px 0;
}

.goods-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #e9ecef;
  transition: all 0.2s ease;
  cursor: pointer;
  position: relative;
}

.goods-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
  transform: translateY(-2px);
  border-color: #adb5bd;
}

.card-checkbox {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 10;
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #667eea;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  border-radius: 4px;
}

.goods-image-wrapper {
  width: 100%;
  height: 220px;
  overflow: hidden;
  background-color: #f8f9fa;
  position: relative;
}

.goods-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-download-btn {
  position: absolute;
  bottom: 10px;
  right: 10px;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.95);
  border: none;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  opacity: 0;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
  z-index: 5;
}

.goods-card:hover .image-download-btn {
  opacity: 1;
}

.image-download-btn:hover {
  background: white;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(0,0,0,0.2);
}

.status-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255, 255, 255, 0.95);
}

.status-on { color: #28a745; border: 1px solid #28a745; }
.status-off { color: #dc3545; border: 1px solid #dc3545; }
.status-out { color: #ffc107; border: 1px solid #ffc107; }

.goods-info {
  padding: 20px;
}

.goods-name {
  font-size: 16px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 12px;
  line-height: 1.4;
  height: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-description {
  font-size: 13px;
  color: #6c757d;
  margin-bottom: 12px;
  line-height: 1.5;
  height: 39px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.goods-price {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 0;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
}

.price-current {
  font-size: 24px;
  color: #dc3545;
  font-weight: 600;
}

.price-current::before {
  content: '¥';
  font-size: 16px;
  margin-right: 2px;
}

.price-original {
  font-size: 13px;
  color: #adb5bd;
  text-decoration: line-through;
}

.price-original::before {
  content: '¥';
  font-size: 12px;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #6c757d;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 11px;
  color: #adb5bd;
}

.meta-value {
  font-weight: 500;
  color: #495057;
}

.stock-high { color: #28a745; }
.stock-medium { color: #ffc107; }
.stock-low { color: #dc3545; }

/* 空状态和加载状态 */
.empty-state,
.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: #6c757d;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-state h2 {
  font-size: 20px;
  margin-bottom: 8px;
  color: #495057;
  font-weight: 500;
}

.empty-state p {
  font-size: 16px;
}

/* 分页 */
.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  gap: 16px;
}

.pagination {
  display: flex;
  gap: 8px;
  align-items: center;
}

.page-item {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 40px;
  height: 40px;
  padding: 0 12px;
  border: 1.5px solid #dee2e6;
  border-radius: 8px;
  background: white;
  color: #495057;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.page-item:hover:not(.disabled):not(.active) {
  border-color: #667eea;
  color: #667eea;
  background: #f8f9ff;
  transform: translateY(-2px);
}

.page-item.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-color: #667eea;
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.page-item.disabled {
  opacity: 0.5;
  cursor: not-allowed;
  pointer-events: none;
}

.page-info {
  font-size: 14px;
  color: #6c757d;
}

.page-info strong {
  color: #667eea;
  font-weight: 600;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .filter-form {
    grid-template-columns: 1fr 1fr;
  }

  .filter-group:first-child {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .filter-section {
    padding: 20px;
  }

  .filter-form {
    grid-template-columns: 1fr;
  }

  .filter-group:first-child {
    grid-column: auto;
  }

  .btn-filter,
  .btn-reset {
    width: 100%;
    justify-content: center;
  }

  .header-actions {
    padding-right: 0;
  }

  .user-info-section {
    position: static;
    margin-top: 15px;
  }

  .login-prompt {
    position: static;
  }
}
</style>