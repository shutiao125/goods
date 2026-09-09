<template>
  <div class="article-list-container">
    <div class="page-header">
      <h1>📰 文章中心</h1>
      <p class="subtitle">发现精彩内容，获取最新资讯</p>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索文章标题或内容..."
        size="large"
        clearable
        @clear="handleSearch"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button @click="handleSearch" type="primary">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </template>
      </el-input>
    </div>

    <!-- 文章列表 -->
    <div class="article-grid" v-loading="loading">
      <el-empty 
        v-if="!loading && articles.length === 0" 
        description="暂无文章"
      />
      
      <el-card 
        v-for="article in articles" 
        :key="article.id" 
        class="article-card"
        shadow="hover"
        @click="viewArticle(article.id)"
      >
        <div class="article-image" v-if="article.coverImage">
          <img 
            :src="getImageUrl(article.coverImage)" 
            :alt="article.title"
            @error="handleImageError"
          />
        </div>
        
        <div class="article-content">
          <h3 class="article-title">{{ article.title }}</h3>
          
          <p class="article-summary">
            {{ getSummary(article.content) }}
          </p>
          
          <div class="article-meta">
            <span class="author">
              <el-icon><User /></el-icon>
              {{ article.author || '匿名' }}
            </span>
            
            <span class="time">
              <el-icon><Clock /></el-icon>
              {{ formatTime(article.createdAt) }}
            </span>
            
            <span class="views" v-if="article.viewCount">
              <el-icon><View /></el-icon>
              {{ article.viewCount }} 阅读
            </span>
          </div>
          
          <div class="article-tags" v-if="article.tags">
            <el-tag 
              v-for="tag in parseTags(article.tags)" 
              :key="tag"
              size="small"
              type="info"
            >
              {{ tag }}
            </el-tag>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 30]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchArticles"
        @current-change="fetchArticles"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/api/request'

const router = useRouter()

const loading = ref(false)
const articles = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

onMounted(() => {
  fetchArticles()
})

async function fetchArticles() {
  try {
    loading.value = true
    
    const params = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined
    }

    let res
    
    if (searchKeyword.value) {
      res = await request.get('/articles/search', { params })
    } else {
      res = await request.get('/articles/page', { params })
    }
    
    if (res.data && res.data.records) {
      articles.value = res.data.records
      total.value = res.data.total
    } else if (Array.isArray(res.data)) {
      articles.value = res.data
      total.value = res.data.length
    } else {
      articles.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('获取文章列表失败:', error)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchArticles()
}

function getImageUrl(image) {
  if (!image) return '/default-article.png'
  if (image.startsWith('http')) return image
  return `/img/${image}`
}

function handleImageError(e) {
  e.target.src = '/default-article.png'
}

function getSummary(content) {
  if (!content) return '暂无内容摘要'
  
  // 移除 HTML 标签
  const text = content.replace(/<[^>]+>/g, '')
  
  // 截取前 150 个字符
  if (text.length > 150) {
    return text.substring(0, 150) + '...'
  }
  
  return text
}

function parseTags(tags) {
  if (!tags) return []
  
  if (typeof tags === 'string') {
    try {
      return JSON.parse(tags)
    } catch {
      return tags.split(',').map(t => t.trim()).filter(Boolean)
    }
  }
  
  return Array.isArray(tags) ? tags : []
}

function formatTime(time) {
  if (!time) return ''
  return new Date(time).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

function viewArticle(id) {
  ElMessage.info('文章详情页开发中...')
  // TODO: 跳转到文章详情页
  // router.push(`/articles/${id}`)
}
</script>

<style scoped>
.article-list-container {
  padding: 20px 0;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: white;
}

.page-header h1 {
  font-size: 32px;
  margin-bottom: 10px;
}

.subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin: 0;
}

.search-bar {
  max-width: 600px;
  margin: -25px auto 30px;
  position: relative;
  z-index: 1;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
  min-height: 400px;
}

.article-card {
  cursor: pointer;
  transition: all 0.3s ease;
  border-radius: 12px;
  overflow: hidden;
}

.article-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
}

.article-image {
  width: 100%;
  height: 200px;
  overflow: hidden;
  background-color: #f5f7fa;
}

.article-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s ease;
}

.article-card:hover .article-image img {
  transform: scale(1.05);
}

.article-content {
  padding: 20px;
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-summary {
  color: #606266;
  line-height: 1.6;
  font-size: 14px;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
  color: #909399;
  font-size: 13px;
  margin-bottom: 12px;
}

.article-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.article-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.pagination-wrapper {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>