<template>
  <div class="register-container">
    <div class="register-background"></div>
    <el-card class="register-card" shadow="always">
      <template #header>
        <div class="card-header">
          <h2>📝 用户注册</h2>
          <p class="subtitle">创建您的账号，开始购物之旅</p>
        </div>
      </template>

      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-position="top"
        size="large"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input 
                v-model="form.username" 
                placeholder="请输入用户名"
                prefix-icon="User"
                clearable
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input 
                v-model="form.phone" 
                placeholder="请输入手机号"
                prefix-icon="Phone"
                clearable
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input 
                v-model="form.password" 
                type="password" 
                placeholder="请输入密码"
                prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </el-col>
          
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input 
                v-model="form.confirmPassword" 
                type="password" 
                placeholder="请再次输入密码"
                prefix-icon="Lock"
                show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="邮箱（选填）" prop="email">
          <el-input 
            v-model="form.email" 
            placeholder="请输入邮箱地址"
            prefix-icon="Message"
            clearable
          />
        </el-form-item>

        <el-form-item prop="agree">
          <el-checkbox v-model="form.agree">
            我已阅读并同意<span class="agreement-link">《用户协议》</span>和<span class="agreement-link">《隐私政策》</span>
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleRegister" 
            :loading="loading"
            style="width: 100%"
          >
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-link">
      已有账号？
      <router-link to="/login" class="login-text">立即登录</router-link>
    </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
  agree: false
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度在 6 到 30 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号格式', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  agree: [
    { validator: (rule, value, callback) => {
      if (!value) {
        callback(new Error('请阅读并同意用户协议'))
      } else {
        callback()
      }
    }, trigger: 'change' }
  ]
}

async function handleRegister() {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true
    
    // TODO: 调用注册API
    await new Promise(resolve => setTimeout(resolve, 1500))
    
    ElMessage.success('注册成功！即将跳转到登录页面 🎉')
    
    setTimeout(() => {
      router.push('/login')
    }, 1500)
  } catch (error) {
    console.error('注册失败:', error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  padding: 20px;
}

.register-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  z-index: -1;
}

.register-card {
  width: 600px;
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.card-header {
  text-align: center;
  padding: 10px 0;
}

.card-header h2 {
  font-size: 28px;
  color: #303133;
  margin-bottom: 8px;
}

.subtitle {
  color: #909399;
  font-size: 14px;
  margin: 0;
}

.login-link {
  text-align: center;
  margin-top: 15px;
  color: #606266;
  font-size: 14px;
}

.login-link a,
.login-text {
  color: #409eff;
  text-decoration: none;
  font-weight: bold;
}

.login-link a:hover,
.login-text:hover {
  text-decoration: underline;
}

.agreement-link {
  color: #409eff;
  cursor: pointer;
}
</style>