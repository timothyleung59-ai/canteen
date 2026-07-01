<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="brand">
        <el-icon :size="40" color="#409eff"><Bowl /></el-icon>
        <h2>饭堂报餐管理后台</h2>
        <p>请登录管理员账号</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="onSubmit">
        <el-form-item prop="username">
          <el-input v-model="form.username" size="large" placeholder="管理员账号"
            :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" size="large" placeholder="密码"
            :prefix-icon="Lock" show-password @keyup.enter="onSubmit" />
        </el-form-item>
        <el-button type="primary" size="large" class="submit" :loading="loading"
          @click="onSubmit">登 录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '../api/bc'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return
  loading.value = true
  try {
    const data = await login(form.username, form.password)
    if (!data || !data.appid) {
      ElMessage.error('后台未配置 appid，请在服务端设置 admin.appid')
      return
    }
    auth.setSession(data)
    ElMessage.success('登录成功')
    router.replace('/')
  } catch (e) {
    /* 错误已在拦截器提示 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #3a6073 100%);
  padding: 20px;
  box-sizing: border-box;
}
.login-card {
  width: 380px;
  max-width: 100%;
  border-radius: 12px;
}
.brand {
  text-align: center;
  margin-bottom: 18px;
}
.brand h2 {
  margin: 10px 0 4px;
  font-size: 20px;
}
.brand p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}
.submit {
  width: 100%;
}
</style>
