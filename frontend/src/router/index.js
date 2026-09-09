import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/goods'
  },
  {
    path: '/goods',
    name: 'GoodsList',
    component: () => import('@/views/GoodsList.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/goods/:id',
    name: 'GoodsDetail',
    component: () => import('@/views/GoodsDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/articles',
    name: 'ArticleList',
    component: () => import('@/views/ArticleList.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('@/views/Cart.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('@/views/AdminDashboard.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/goods',
    name: 'AdminGoodsManage',
    component: () => import('@/views/AdminGoodsManage.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/seller/dashboard',
    name: 'SellerDashboard',
    component: () => import('@/views/SellerDashboard.vue'),
    meta: { requiresAuth: true, requiresSeller: true }
  },
  {
    path: '/seller/goods',
    name: 'SellerGoodsManage',
    component: () => import('@/views/SellerGoodsManage.vue'),
    meta: { requiresAuth: true, requiresSeller: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.name === 'Login' || to.name === 'Register') && token) {
    next('/goods')
  } else if (to.meta.requiresAdmin && userInfo.role !== 'admin') {
    ElMessage.error('您没有管理员权限')
    next('/goods')
  } else if (to.meta.requiresSeller && !['admin', 'seller'].includes(userInfo.role)) {
    ElMessage.error('您没有卖家权限')
    next('/goods')
  } else {
    next()
  }
})

export default router