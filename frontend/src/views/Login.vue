<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 头部 Logo 和标题 -->
      <div class="login-header">
        <img src="/img/login-1.jpg" alt="Logo" class="logo-img">
        <h1 class="title">综合系统登录</h1>
      </div>

      <!-- 欢迎消息 -->
      <div class="welcome-message">
        ✨ 欢迎回来，请输入您的账号信息
      </div>

      <!-- 错误消息提示 -->
      <div v-if="errorMessage" class="error-message">
        ⚠️ {{ errorMessage }}
      </div>

      <!-- 成功消息提示 -->
      <div v-if="successMessage" class="success-message">
        🎉 {{ successMessage }}
      </div>

      <!-- 登录表单 -->
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules"
        @submit.prevent="handleLogin"
      >
        <div class="form-group">
          <label for="username">👤 用户名</label>
          <input
            type="text"
            id="username"
            v-model="form.username"
            placeholder="请输入您的用户名"
            required
            autocomplete="username"
            class="form-input"
          >
        </div>

        <div class="form-group">
          <label for="password">🔒 密码</label>
          <input
            type="password"
            id="password"
            v-model="form.password"
            placeholder="请输入您的密码"
            required
            autocomplete="current-password"
            class="form-input"
          >
        </div>

        <div class="checkbox-group">
          <input type="checkbox" id="rememberMe" v-model="form.rememberMe">
          <label for="rememberMe">记住我，下次自动登录</label>
        </div>

        <button 
          type="button" 
          class="login-btn" 
          @click="handleLogin"
          :disabled="loading"
        >
          {{ loading ? '登录中...' : '立即登录' }}
        </button>
      </el-form>

      <!-- 注册链接 -->
      <div class="register-section">
        <p class="register-text">还没有账号？</p>
        <router-link to="/register" class="register-btn">📝 注册新账号</router-link>
      </div>

      <!-- 页脚 -->
      <div class="footer">
        <p>&copy; {{ currentYear }} 商品系统版权所有。</p>
        <p>XXX商贸有限公司</p>
        <div class="language-switch">
          <a href="#" @click.prevent="switchLanguage('zh_CN')">🇨🇳 中文</a>
          <a href="#" @click.prevent="switchLanguage('en_US')">🇺🇸 English</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const form = reactive({
  username: '',
  password: '',
  rememberMe: false
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const currentYear = computed(() => new Date().getFullYear())

async function handleLogin() {
  if (!form.username || !form.password) {
    errorMessage.value = '请填写完整的用户名和密码'
    return
  }

  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    await userStore.login({
      username: form.username,
      password: form.password
    })

    successMessage.value = '登录成功！正在跳转...'
    
    ElMessage.success('登录成功！欢迎回来 🎉')
    
    setTimeout(() => {
      const redirectPath = router.currentRoute.value.query.redirect || '/goods'
      router.push(redirectPath)
    }, 1000)

  } catch (error) {
    console.error('登录失败:', error)
    errorMessage.value = error.response?.data?.message || '用户名或密码错误'
  } finally {
    loading.value = false
  }
}

function switchLanguage(lang) {
  ElMessage.info(`切换语言: ${lang}`)
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.login-page {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Microsoft YaHei', Arial, sans-serif;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* 动态背景动画 */
.login-page::before {
  content: '';
  position: absolute;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.1) 1px, transparent 1px);
  background-size: 50px 50px;
  animation: moveBackground 20s linear infinite;
}

@keyframes moveBackground {
  0% { transform: translate(0, 0); }
  100% { transform: translate(50px, 50px); }
}

/* 登录容器 */
.login-container {
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  max-width: 450px;
  width: 100%;
  padding: 50px 40px;
  position: relative;
  z-index: 1;
  animation: slideUp 0.6s ease-out;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 头部 */
.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin-bottom: 20px;
  box-shadow: 0 8px 20px rgba(102, 126, 234, 0.3);
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.title {
  font-size: 28px;
  font-weight: 600;
  color: #212529;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 欢迎消息 */
.welcome-message {
  text-align: center;
  color: #6c757d;
  font-size: 14px;
  margin-bottom: 30px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  border-left: 3px solid #667eea;
}

/* 错误/成功消息 */
.error-message {
  color: #dc3545;
  padding: 12px 16px;
  margin: 15px 0;
  background: #fff5f5;
  border-radius: 8px;
  border-left: 3px solid #dc3545;
  font-size: 14px;
  animation: shake 0.5s;
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-10px); }
  75% { transform: translateX(10px); }
}

.success-message {
  color: #28a745;
  padding: 12px 16px;
  margin: 15px 0;
  background: #f0fff4;
  border-radius: 8px;
  border-left: 3px solid #28a745;
  font-size: 14px;
  animation: slideIn 0.5s;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

/* 表单组 */
.form-group {
  margin-bottom: 24px;
  position: relative;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #495057;
  margin-bottom: 8px;
  transition: color 0.3s;
}

.form-input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e9ecef;
  border-radius: 10px;
  font-size: 15px;
  transition: all 0.3s ease;
  background: #f8f9fa;
}

.form-input:hover {
  border-color: #dee2e6;
  background: white;
}

.form-input:focus {
  outline: none;
  border-color: #667eea;
  background: white;
  box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
}

.form-input::placeholder {
  color: #adb5bd;
}

/* 复选框组 */
.checkbox-group {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s;
}

.checkbox-group:hover {
  background: #e9ecef;
}

.checkbox-group input[type="checkbox"] {
  width: 18px;
  height: 18px;
  margin-right: 10px;
  cursor: pointer;
  accent-color: #667eea;
}

.checkbox-group label {
  font-size: 14px;
  color: #495057;
  cursor: pointer;
  user-select: none;
}

/* 登录按钮 */
.login-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  position: relative;
  overflow: hidden;
}

.login-btn::before {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  width: 0;
  height: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  transform: translate(-50%, -50%);
  transition: width 0.6s, height 0.6s;
}

.login-btn:hover::before {
  width: 300px;
  height: 300px;
}

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.login-btn:active:not(:disabled) {
  transform: translateY(0);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 注册区域 */
.register-section {
  text-align: center;
  margin-top: 25px;
  position: relative;
  z-index: 10;
}

.register-text {
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 15px;
}

.register-btn {
  display: inline-block;
  padding: 14px 40px;
  background: white;
  color: #667eea;
  border: 2px solid #667eea;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.2);
}

.register-btn:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
}

/* 页脚 */
.footer {
  text-align: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e9ecef;
}

.footer p {
  font-size: 13px;
  color: #6c757d;
  margin: 5px 0;
}

.language-switch {
  margin-top: 15px;
  display: flex;
  justify-content: center;
  gap: 12px;
}

.language-switch a {
  font-size: 13px;
  color: #667eea;
  text-decoration: none;
  padding: 6px 16px;
  border: 2px solid #667eea;
  border-radius: 20px;
  transition: all 0.3s ease;
  font-weight: 500;
}

.language-switch a:hover {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-color: transparent;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-container {
    padding: 40px 30px;
  }

  .title {
    font-size: 24px;
  }

  .logo-img {
    width: 70px;
    height: 70px;
  }
}
</style>