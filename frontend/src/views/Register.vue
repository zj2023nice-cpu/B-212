<template>
  <div class="min-h-screen flex items-center justify-center bg-[#fdf8f5] p-4">
    <div class="w-full max-w-md glass-card p-8 shadow-2xl">
      <div class="text-center mb-8">
        <h2 class="text-2xl font-bold text-gray-800">注册账号</h2>
        <p class="text-gray-500 mt-2">享受更多专享优惠</p>
      </div>

      <el-form :model="form" @submit.prevent="handleRegister" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="建议使用手机号" size="large" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="您希望我们如何称呼您" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password size="large" />
        </el-form-item>
        
        <div class="mt-8">
          <el-button type="primary" native-type="submit" class="w-full !rounded-xl !h-12 !text-lg" :loading="loading">
            立即注册
          </el-button>
        </div>
      </el-form>

      <div class="text-center mt-6">
        <el-button link @click="$router.push('/login')">返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  nickname: '',
  phone: ''
})

const handleRegister = async () => {
  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>
