<template>
  <div class="min-h-screen bg-gray-50 flex flex-col">
    <!-- Header -->
    <header class="bg-white shadow-sm sticky top-0 z-50">
      <div class="max-w-7xl mx-auto px-4 h-16 flex items-center justify-between">
        <div class="flex items-center space-x-2 cursor-pointer" @click="$router.push('/')">
          <div class="w-10 h-10 bg-primary rounded-full flex items-center justify-center">
            <el-icon color="white" size="24"><IceTea /></el-icon>
          </div>
          <h1 class="text-xl font-bold text-primary">奶茶点餐</h1>
        </div>
        
        <div class="flex items-center space-x-6">
          <el-badge :value="cartStore.totalCount" :hidden="cartStore.totalCount === 0">
            <el-button link @click="$router.push('/cart')">
              <el-icon size="20"><ShoppingCart /></el-icon>
            </el-button>
          </el-badge>
          
          <el-dropdown>
            <div class="flex items-center space-x-2 cursor-pointer">
              <el-avatar :size="32" :src="userStore.user?.avatar">{{ userStore.user?.nickname?.charAt(0) }}</el-avatar>
              <span class="hidden md:inline font-medium">{{ userStore.user?.nickname }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/coupons')">领券中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/my-coupons')">我的优惠券</el-dropdown-item>
                <el-dropdown-item v-if="userStore.user?.role === 'ADMIN'" @click="$router.push('/coupon-admin')">优惠券管理</el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <!-- Main Content -->
    <main class="flex-grow max-w-7xl mx-auto w-full px-4 py-6">
      <router-view />
    </main>

    <!-- TabBar Mobile (Optional) -->
    <footer class="md:hidden bg-white border-t h-16 flex items-center justify-around">
      <el-button link @click="$router.push('/')" :class="{'text-primary': $route.path === '/'}">
        <el-icon size="24"><HomeFilled /></el-icon>
      </el-button>
      <el-button link @click="$router.push('/coupons')" :class="{'text-primary': $route.path === '/coupons'}">
        <el-icon size="24"><Ticket /></el-icon>
      </el-button>
      <el-button link @click="$router.push('/cart')" :class="{'text-primary': $route.path === '/cart'}">
        <el-icon size="24"><ShoppingCartFull /></el-icon>
      </el-button>
      <el-button link @click="$router.push('/orders')" :class="{'text-primary': $route.path === '/orders'}">
        <el-icon size="24"><List /></el-icon>
      </el-button>
    </footer>
  </div>
</template>

<script setup>
import { useUserStore } from '@/store/user'
import { useCartStore } from '@/store/cart'
import { useRouter } from 'vue-router'
import { onMounted } from 'vue'

const userStore = useUserStore()
const cartStore = useCartStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  cartStore.fetchCart()
})
</script>
