<template>
  <div class="min-h-screen flex items-center justify-center bg-[#fdf8f5] p-4">
    <div class="w-full max-w-md glass-card p-8 shadow-2xl">
      <div class="text-center mb-8">
        <div
          class="w-16 h-16 bg-primary mx-auto rounded-2xl flex items-center justify-center shadow-lg mb-4"
        >
          <el-icon color="white" size="32"><Lock /></el-icon>
        </div>
        <h2 class="text-2xl font-bold text-gray-800">欢迎回来</h2>
        <p class="text-gray-500 mt-2">开启您的奶茶之旅</p>
      </div>

      <el-form :model="form" @submit.prevent="handleLogin" label-position="top">
        <el-form-item label="用户名">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Key"
            show-password
            size="large"
          />
        </el-form-item>

        <div class="mt-8">
          <el-button
            type="primary"
            native-type="submit"
            class="w-full !rounded-xl !h-12 !text-lg"
            :loading="loading"
          >
            登录
          </el-button>
        </div>
      </el-form>

      <div class="text-center mt-6 flex items-center justify-center">
        <span class="text-gray-500">还没有账号？</span>
        <el-button link type="primary" @click="$router.push('/register')"
          >立即注册</el-button
        >
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    const data = await login(form)
    userStore.setToken(data.token)
    localStorage.setItem('user', JSON.stringify(data.user))
    userStore.user = data.user
    ElMessage.success('登录成功')
    router.push('/')
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>
