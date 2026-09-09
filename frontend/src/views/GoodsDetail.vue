<template>
  <div class="goods-detail-page">
    <!-- 导航栏 -->
    <nav class="navbar">
      <div class="nav-container">
        <router-link to="/goods" class="back-link">返回列表</router-link>
      </div>
    </nav>

    <div class="container">
      <!-- 加载中状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="10" animated />
      </div>

      <!-- 商品详情主体 -->
      <div v-else-if="goods" class="detail-wrapper">
        <!-- 主要内容区：图片 + 信息 -->
        <div class="detail-main">
          <!-- 左侧：商品图片 -->
          <div class="image-section">
            <img 
              :src="goods.mainImage || '/img/placeholder.jpg'" 
              :alt="goods.name"
              class="main-image"
              @error="handleImageError"
            >
          </div>

          <!-- 右侧：商品信息 -->
          <div class="info-section">
            <!-- 状态标签 -->
            <span :class="['goods-status', getStatusClass(goods.status)]">
              {{ getStatusText(goods.status) }}
            </span>

            <!-- 商品标题 -->
            <h1 class="goods-title">{{ goods.name }}</h1>

            <!-- 商品描述 -->
            <p class="goods-desc">{{ goods.description }}</p>

            <!-- 价格区域 -->
            <div class="price-box">
              <div class="price-row">
                <span class="price-label">售价</span>
                <span class="current-price">{{ goods.price }}</span>
              </div>
              <div v-if="goods.originalPrice > goods.price" class="price-row">
                <span class="price-label">原价</span>
                <span class="original-price">{{ goods.originalPrice }}</span>
              </div>
            </div>

            <!-- 元数据网格 -->
            <div class="meta-grid">
              <div class="meta-box">
                <div class="meta-box-label">库存</div>
                <div :class="['meta-box-value', getStockClass(goods.stock)]">
                  {{ goods.stock }}
                </div>
              </div>
              <div class="meta-box">
                <div class="meta-box-label">销量</div>
                <div class="meta-box-value">{{ goods.salesCount || 0 }}</div>
              </div>
              <div class="meta-box">
                <div class="meta-box-label">分类ID</div>
                <div class="meta-box-value">{{ goods.categoryId }}</div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="action-buttons">
              <button 
                class="btn btn-primary" 
                @click="buyNow"
                :disabled="goods.status !== 1 || goods.stock <= 0"
              >
                💰 立即购买
              </button>
              <button 
                class="btn btn-secondary" 
                @click="addToCart"
                :disabled="goods.status !== 1 || goods.stock <= 0"
              >
                🛒 加入购物车
              </button>
            </div>
          </div>
        </div>

        <!-- Tab 切换区域 -->
        <div class="detail-tabs">
          <div class="tab-header">
            <div 
              :class="['tab-item', { active: activeTab === 'detail' }]"
              @click="activeTab = 'detail'"
            >
              📋 商品详情
            </div>
          </div>
          
          <div v-show="activeTab === 'detail'" class="tab-content">
            <div class="detail-text">{{ goods.description || '暂无详细描述' }}</div>
          </div>
        </div>

        <!-- 评论区 -->
        <div class="comment-section">
          <div class="comment-header">
            <div class="comment-title">
              <span>💬</span>
              <span>用户评论</span>
            </div>
            <span class="comment-count">{{ comments.length }} 条评论</span>
          </div>

          <!-- 发表评论表单 -->
          <div v-if="isLoggedIn" class="comment-form">
            <div class="form-row">
              <input 
                type="text"
                v-model="commentForm.username"
                placeholder="请输入您的昵称"
                class="form-input"
              >
              <select v-model="commentForm.rating" class="form-input">
                <option value="5">⭐⭐⭐⭐⭐</option>
                <option value="4">⭐⭐⭐⭐</option>
                <option value="3">⭐⭐⭐</option>
                <option value="2">⭐⭐</option>
                <option value="1">⭐</option>
              </select>
            </div>
            <textarea 
              v-model="commentForm.content"
              placeholder="分享您对这个商品的看法..."
              class="form-textarea"
            ></textarea>
            <button @click="submitComment" class="btn-submit">
              发表评论
            </button>
          </div>

          <!-- 未登录提示 -->
          <div v-else class="login-prompt-comment">
            <p>🔐 登录后即可发表评论</p>
            <router-link to="/login" class="btn-login-small">立即登录</router-link>
          </div>

          <!-- 评论列表 -->
          <div v-if="comments.length > 0" class="comment-list">
            <div v-for="comment in comments" :key="comment.id" class="comment-item">
              <div class="comment-meta">
                <div class="comment-user">
                  <div class="user-avatar">
                    {{ (comment.username || '用').charAt(0).toUpperCase() }}
                  </div>
                  <div class="user-info">
                    <div class="username">
                      {{ comment.username || '匿名用户' }}
                    </div>
                    <div class="comment-time">
                      {{ formatTime(comment.createdAt) }}
                    </div>
                  </div>
                </div>
                <div class="comment-rating">
                  <span 
                    v-for="i in 5" 
                    :key="i"
                    :class="['star', { empty: i > (comment.rating || 5) }]"
                  >
                    ★
                  </span>
                </div>
              </div>
              
              <div class="comment-content">{{ comment.content }}</div>

              <!-- 商家回复 -->
              <div v-if="comment.reply" class="admin-reply">
                <div class="reply-header">
                  <span class="reply-icon">💬</span>
                  <span class="reply-label">商家回复</span>
                  <span v-if="comment.replyTime" class="reply-time">
                    {{ formatTime(comment.replyTime) }}
                  </span>
                </div>
                <div class="reply-content">{{ comment.reply }}</div>
              </div>
            </div>
          </div>

          <!-- 无评论提示 -->
          <div v-else class="empty-comments">
            <div class="empty-icon">💭</div>
            <p class="empty-text">暂无评论，快来发表第一条评论吧！</p>
          </div>
        </div>
      </div>

      <!-- 商品不存在 -->
      <div v-else class="empty-state">
        <div class="empty-icon">❌</div>
        <h2>商品不存在</h2>
        <p>该商品可能已下架或删除</p>
        <router-link to="/goods" class="back-btn">返回商品列表</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const goods = ref(null)
const comments = ref([])
const loading = ref(true)
const activeTab = ref('detail')

const commentForm = ref({
  username: '',
  rating: 5,
  content: ''
})

const isLoggedIn = computed(() => !!userStore.token)

onMounted(async () => {
  await fetchGoodsDetail()
  await fetchComments()
})

async function fetchGoodsDetail() {
  loading.value = true
  try {
    const id = route.params.id
    const res = await request.get(`/goods/${id}`)
    
    if (res.data) {
      goods.value = res.data
      document.title = `${res.data.name} - 商品详情`
    }
  } catch (error) {
    console.error('获取商品详情失败:', error)
    ElMessage.error('获取商品详情失败')
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  try {
    const id = route.params.id
    const res = await request.get(`/goods/${id}/comments`)
    
    if (res.data) {
      comments.value = res.data
    }
  } catch (error) {
    console.error('获取评论失败:', error)
  }
}

function buyNow() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  ElMessage.info('正在跳转到结算页面...')
}

async function addToCart() {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    const cartData = {
      goodsId: goods.value.id,
      quantity: 1
    }

    await request.post('/cart', cartData)
    ElMessage.success('已添加到购物车 🛒')
  } catch (error) {
    console.error('添加购物车失败:', error)
    ElMessage.error('添加失败，请重试')
  }
}

async function submitComment() {
  if (!commentForm.value.username.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }

  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  try {
    const id = route.params.id
    
    await request.post(`/goods/${id}/comments`, {
      username: commentForm.value.username,
      rating: commentForm.value.rating,
      content: commentForm.value.content
    })

    ElMessage.success('评论发表成功 ✨')

    // 重置表单
    commentForm.value = {
      username: '',
      rating: 5,
      content: ''
    }

    // 刷新评论列表
    await fetchComments()
  } catch (error) {
    console.error('发表评论失败:', error)
    ElMessage.error('发表失败，请重试')
  }
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
  if (stock > 50) return 'stock-normal'
  if (stock > 10) return 'stock-warning'
  return 'stock-danger'
}

function formatTime(time) {
  if (!time) return '-'
  
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  
  return `${year}-${month}-${day} ${hours}:${minutes}`
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.goods-detail-page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', Arial, sans-serif;
  background-color: #f8f9fa;
  color: #333;
  line-height: 1.6;
  min-height: 100vh;
}

/* 导航栏 */
.navbar {
  background: white;
  border-bottom: 1px solid #dee2e6;
  padding: 16px 0;
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.back-link {
  color: #495057;
  text-decoration: none;
  font-size: 14px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: color 0.2s;
}

.back-link:hover {
  color: #212529;
}

.back-link::before {
  content: '←';
  font-size: 18px;
}

/* 容器 */
.container {
  max-width: 1200px;
  margin: 30px auto;
  padding: 0 20px;
}

/* 加载状态 */
.loading-state {
  background: white;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  padding: 40px;
}

/* 详情包装器 */
.detail-wrapper {
  background: white;
  border-radius: 8px;
  border: 1px solid #dee2e6;
  overflow: hidden;
}

/* 主内容区 */
.detail-main {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
}

/* 图片区域 */
.image-section {
  padding: 40px;
  background: #f8f9fa;
  border-right: 1px solid #dee2e6;
}

.main-image {
  width: 100%;
  height: auto;
  max-height: 500px;
  object-fit: contain;
  border-radius: 4px;
}

/* 信息区域 */
.info-section {
  padding: 40px;
  display: flex;
  flex-direction: column;
}

.goods-status {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 16px;
  width: fit-content;
}

.status-on {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.status-off {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.status-out {
  background: #fff3cd;
  color: #856404;
  border: 1px solid #ffeaa7;
}

.goods-title {
  font-size: 28px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 16px;
  line-height: 1.4;
}

.goods-desc {
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 24px;
  line-height: 1.8;
}

/* 价格区域 */
.price-box {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 6px;
  margin-bottom: 24px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 8px;
}

.price-label {
  font-size: 14px;
  color: #6c757d;
  min-width: 60px;
}

.current-price {
  font-size: 36px;
  color: #dc3545;
  font-weight: 600;
}

.current-price::before {
  content: '¥';
  font-size: 24px;
}

.original-price {
  font-size: 16px;
  color: #adb5bd;
  text-decoration: line-through;
}

.original-price::before {
  content: '¥';
  font-size: 14px;
}

/* 元数据网格 */
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 24px;
  padding: 20px 0;
  border-top: 1px solid #e9ecef;
  border-bottom: 1px solid #e9ecef;
}

.meta-box {
  text-align: center;
  padding: 12px;
}

.meta-box-label {
  font-size: 12px;
  color: #adb5bd;
  margin-bottom: 6px;
  text-transform: uppercase;
}

.meta-box-value {
  font-size: 20px;
  font-weight: 600;
  color: #212529;
}

.stock-normal { color: #28a745; }
.stock-warning { color: #ffc107; }
.stock-danger { color: #dc3545; }

/* 操作按钮 */
.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: auto;
}

.btn {
  flex: 1;
  padding: 14px 24px;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  text-align: center;
  text-decoration: none;
  display: inline-block;
}

.btn-primary {
  background: #007bff;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #0056b3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 123, 255, 0.3);
}

.btn-secondary {
  background: white;
  color: #495057;
  border: 1px solid #dee2e6;
}

.btn-secondary:hover:not(:disabled) {
  background: #f8f9fa;
  border-color: #adb5bd;
  transform: translateY(-2px);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Tab 区域 */
.detail-tabs {
  border-top: 1px solid #dee2e6;
}

.tab-header {
  display: flex;
  border-bottom: 2px solid #e9ecef;
  background: #f8f9fa;
}

.tab-item {
  padding: 16px 32px;
  font-size: 15px;
  font-weight: 500;
  color: #6c757d;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  margin-bottom: -2px;
  transition: all 0.2s;
}

.tab-item.active {
  color: #007bff;
  border-bottom-color: #007bff;
  background: white;
}

.tab-item:hover:not(.active) {
  background: #f1f3f5;
}

.tab-content {
  padding: 32px 40px;
}

.detail-text {
  font-size: 14px;
  line-height: 1.8;
  color: #495057;
}

/* 评论区 */
.comment-section {
  border-top: 2px solid #e9ecef;
  padding: 40px;
  background: white;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f1f3f5;
}

.comment-title {
  font-size: 20px;
  font-weight: 600;
  color: #212529;
  display: flex;
  align-items: center;
  gap: 8px;
}

.comment-count {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 500;
}

/* 评论表单 */
.comment-form {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 100px;
  gap: 12px;
  margin-bottom: 12px;
}

.form-input {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #dee2e6;
  border-radius: 6px;
  font-size: 14px;
  transition: all 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.form-textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1.5px solid #dee2e6;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  min-height: 80px;
  font-family: inherit;
  margin-bottom: 12px;
  transition: all 0.2s;
}

.form-textarea:focus {
  outline: none;
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.btn-submit {
  padding: 10px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-submit:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

/* 未登录提示 */
.login-prompt-comment {
  text-align: center;
  padding: 30px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 30px;
}

.login-prompt-comment p {
  color: #6c757d;
  margin-bottom: 15px;
}

.btn-login-small {
  display: inline-block;
  padding: 8px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.3s;
}

.btn-login-small:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 评论列表 */
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #667eea;
  transition: all 0.2s;
}

.comment-item:hover {
  background: #f1f3f5;
  transform: translateX(4px);
}

.comment-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.comment-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.user-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.username {
  font-weight: 600;
  color: #212529;
  font-size: 14px;
}

.comment-time {
  font-size: 12px;
  color: #adb5bd;
}

.comment-rating {
  display: flex;
  gap: 2px;
}

.star {
  color: #ffc107;
  font-size: 16px;
}

.star.empty {
  color: #dee2e6;
}

.comment-content {
  font-size: 14px;
  line-height: 1.6;
  color: #495057;
  margin-top: 10px;
}

/* 商家回复 */
.admin-reply {
  margin: 12px 0 0 0;
  padding: 12px 15px;
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 8px;
  border-left: 3px solid #28a745;
  margin-left: 20px;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(40,167,69,0.15);
}

.reply-icon {
  font-size: 14px;
}

.reply-label {
  font-weight: 600;
  color: #28a745;
  font-size: 13px;
}

.reply-time {
  margin-left: auto;
  color: #999;
  font-size: 11px;
}

.reply-content {
  color: #555;
  font-size: 13px;
  line-height: 1.5;
  padding-left: 20px;
  position: relative;
}

.reply-content::before {
  content: '↳';
  position: absolute;
  left: 4px;
  color: #28a745;
  font-weight: bold;
}

/* 无评论 */
.empty-comments {
  text-align: center;
  padding: 60px 20px;
  color: #adb5bd;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-text {
  font-size: 14px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: white;
  border-radius: 8px;
  border: 1px solid #dee2e6;
}

.empty-state .empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h2 {
  font-size: 24px;
  color: #dc3545;
  margin-bottom: 10px;
}

.empty-state p {
  color: #6c757d;
  margin-bottom: 20px;
}

.back-btn {
  display: inline-block;
  padding: 12px 30px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s;
}

.back-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .detail-main {
    grid-template-columns: 1fr;
  }

  .image-section {
    border-right: none;
    border-bottom: 1px solid #dee2e6;
    padding: 20px;
  }

  .info-section {
    padding: 24px;
  }

  .goods-title {
    font-size: 22px;
  }

  .current-price {
    font-size: 28px;
  }

  .meta-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .action-buttons {
    flex-direction: column;
  }

  .tab-content,
  .comment-section {
    padding: 24px 20px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .navbar {
    padding: 12px 0;
  }
}
</style>