<template>
  <el-menu
    :default-active="activeIndex"
    mode="horizontal"
    :ellipsis="false"
    class="header-menu"
    @select="handleSelect"
  >
    <div class="logo">
      <h2>🛍️ 商品管理系统</h2>
    </div>
    
    <el-menu-item index="/goods">
      <el-icon><Shop /></el-icon>
      商品列表
    </el-menu-item>
    
    <el-menu-item index="/articles">
      <el-icon><Document /></el-icon>
      文章中心
    </el-menu-item>
    
    <el-menu-item index="/cart">
      <el-icon><ShoppingCart /></el-icon>
      购物车
      <el-badge v-if="cartCount > 0" :value="cartCount" class="cart-badge" />
    </el-menu-item>

    <div class="user-info">
      <el-dropdown @command="handleCommand">
        <span class="user-dropdown">
          <el-avatar :size="32" icon="UserFilled" />
          <span class="username">{{ userInfo.username || '用户' }}</span>
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">个人信息</el-dropdown-item>
            <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </el-menu>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import request from '@/api/request'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeIndex = computed(() => route.path)
const userInfo = computed(() => userStore.userInfo)
const cartCount = ref(0)

onMounted(async () => {
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const res = await request.get('/cart/count')
      if (res.data && typeof res.data === 'number') {
        cartCount.value = res.data
      }
    }
  } catch (error) {
    console.log('获取购物车数量失败:', error.message)
  }
})

function handleSelect(index) {
  router.push(index)
}

function handleCommand(command) {
  switch (command) {
    case 'profile':
      ElMessage.info('个人信息功能开发中')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      ElMessage.success('退出成功')
      break
  }
}
</script>

<style scoped>
.header-menu {
  display: flex;
  align-items: center;
  background-color: #fff;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 0 20px;
}

.logo {
  margin-right: 40px;
}

.logo h2 {
  font-size: 20px;
  color: #409eff;
  margin: 0;
}

.user-info {
  margin-left: auto;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 8px;
}

.username {
  font-size: 14px;
  color: #606266;
}

.cart-badge {
  margin-left: 5px;
}
</style>