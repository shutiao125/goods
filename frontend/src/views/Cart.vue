<template>
  <div class="cart-container">
    <div class="cart-header">
      <h2>🛒 我的购物车</h2>
      <el-button 
        type="danger" 
        text 
        @click="clearCart"
        :disabled="cartItems.length === 0"
      >
        清空购物车
      </el-button>
    </div>

    <div class="cart-content" v-loading="loading">
      <!-- 空购物车 -->
      <el-empty 
        v-if="!loading && cartItems.length === 0" 
        description="购物车空空如也~"
      >
        <el-button type="primary" @click="$router.push('/goods')">
          去逛逛
        </el-button>
      </el-empty>

      <!-- 购物车列表 -->
      <div v-else class="cart-items">
        <el-table 
          :data="cartItems" 
          stripe 
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          
          <el-table-column label="商品信息" min-width="300">
            <template #default="{ row }">
              <div class="goods-info">
                <img 
                  :src="getGoodsImage(row.goods)" 
                  :alt="row.goods?.name"
                  class="goods-thumb"
                  @error="handleImageError"
                />
                <div class="goods-detail">
                  <h4>{{ row.goods?.name || '未知商品' }}</h4>
                  <p class="goods-spec">{{ getGoodsSpec(row.goods) }}</p>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="单价" width="120" prop="goods.price">
            <template #default="{ row }">
              <span class="price">¥{{ formatPrice(row.goods?.price) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="数量" width="180">
            <template #default="{ row }">
              <el-input-number 
                v-model="row.quantity" 
                :min="1" 
                :max="row.goods?.stock || 999"
                size="small"
                @change="(val) => updateQuantity(row.id, val)"
              />
            </template>
          </el-table-column>

          <el-table-column label="小计" width="120">
            <template #default="{ row }">
              <span class="subtotal">¥{{ calculateSubtotal(row) }}</span>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button 
                type="danger" 
                text 
                size="small"
                @click="removeItem(row.id)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 结算栏 -->
    <div class="cart-footer" v-if="cartItems.length > 0">
      <div class="footer-left">
        <el-checkbox 
          v-model="selectAll" 
          @change="handleSelectAll"
        >
          全选
        </el-checkbox>
        <span class="selected-count">
          已选择 {{ selectedItems.length }} 件商品
        </span>
      </div>

      <div class="footer-right">
        <div class="total-price">
          <span class="label">合计：</span>
          <span class="price">¥{{ totalPrice.toFixed(2) }}</span>
        </div>
        
        <el-button 
          type="primary" 
          size="large"
          :disabled="selectedItems.length === 0"
          @click="checkout"
        >
          结算 ({{ selectedItems.length }})
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const router = useRouter()

const loading = ref(false)
const cartItems = ref([])
const selectedItems = ref([])
const selectAll = ref(false)

onMounted(() => {
  fetchCartList()
})

async function fetchCartList() {
  try {
    loading.value = true
    
    const res = await request.get('/cart')
    
    if (Array.isArray(res.data)) {
      cartItems.value = res.data
    } else {
      cartItems.value = []
    }
  } catch (error) {
    console.error('获取购物车失败:', error)
    ElMessage.error('获取购物车失败')
  } finally {
    loading.value = false
  }
}

function handleSelectionChange(selection) {
  selectedItems.value = selection
  selectAll.value = selection.length === cartItems.value.length
}

function handleSelectAll(val) {
  if (val) {
    selectedItems.value = [...cartItems.value]
  } else {
    selectedItems.value = []
  }
}

const totalPrice = computed(() => {
  return selectedItems.value.reduce((sum, item) => {
    return sum + calculateSubtotal(item)
  }, 0)
})

function getGoodsImage(goods) {
  if (!goods) return '/default-goods.png'
  if (!goods.image) return '/default-goods.png'
  if (goods.image.startsWith('http')) return goods.image
  return `/img/${goods.image}`
}

function handleImageError(e) {
  e.target.src = '/default-goods.png'
}

function getGoodsSpec(goods) {
  if (!goods) return ''
  const specs = []
  if (goods.categoryId) specs.push(`分类: ${goods.categoryId}`)
  return specs.join(' | ')
}

function formatPrice(price) {
  if (!price) return '0.00'
  return Number(price).toFixed(2)
}

function calculateSubtotal(item) {
  const price = item.goods?.price || 0
  const quantity = item.quantity || 1
  return price * quantity
}

async function updateQuantity(cartId, quantity) {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    
    await request.put(`/cart/${cartId}`, {
      userId: userInfo.userId,
      quantity: quantity
    })
    
    // 刷新列表以获取最新价格
    await fetchCartList()
  } catch (error) {
    console.error('更新数量失败:', error)
    ElMessage.error('更新数量失败')
  }
}

async function removeItem(cartId) {
  try {
    await ElMessageBox.confirm(
      '确定要删除该商品吗？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    
    await request.delete(`/cart/${cartId}`, {
      data: { userId: userInfo.userId }
    })
    
    ElMessage.success('删除成功')
    await fetchCartList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

async function clearCart() {
  try {
    await ElMessageBox.confirm(
      '确定要清空购物车吗？此操作不可恢复！',
      '警告',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    
    await request.delete('/cart/clear', {
      data: { userId: userInfo.userId }
    })
    
    ElMessage.success('购物车已清空')
    await fetchCartList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('清空购物车失败:', error)
      ElMessage.error('清空购物车失败')
    }
  }
}

function checkout() {
  if (selectedItems.value.length === 0) {
    ElMessage.warning('请先选择要结算的商品')
    return
  }

  ElMessage.info('结算功能开发中...')
  // TODO: 实现结算逻辑
  // router.push({ name: 'Checkout', query: { items: selectedItems.value.map(i => i.id) } })
}
</script>

<style scoped>
.cart-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.cart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.cart-header h2 {
  margin: 0;
  color: #303133;
}

.cart-content {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  min-height: 400px;
}

.cart-items {
  margin-top: 10px;
}

.goods-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.goods-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.goods-detail h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #303133;
  font-weight: 600;
}

.goods-spec {
  margin: 0;
  font-size: 12px;
  color: #909399;
}

.price {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.subtotal {
  color: #f56c6c;
  font-weight: bold;
  font-size: 16px;
}

.cart-footer {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1400px;
  background: #fff;
  padding: 15px 20px;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 100;
}

.footer-left {
  display: flex;
  align-items: center;
  gap: 20px;
}

.selected-count {
  color: #909399;
  font-size: 14px;
}

.footer-right {
  display: flex;
  align-items: center;
  gap: 30px;
}

.total-price {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.total-price .label {
  font-size: 16px;
  color: #303133;
}

.total-price .price {
  font-size: 28px;
  font-weight: bold;
  color: #f56c6c;
}
</style>