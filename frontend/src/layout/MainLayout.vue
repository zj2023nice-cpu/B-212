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
          <el-popover
            placement="bottom"
            :width="360"
            trigger="click"
            @show="handleNotificationPopoverShow"
          >
            <template #reference>
              <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0" :max="99">
                <el-button link>
                  <el-icon size="20"><Bell /></el-icon>
                </el-button>
              </el-badge>
            </template>
            <div class="notification-popover">
              <div class="flex items-center justify-between mb-3">
                <span class="font-bold text-base">消息通知</span>
                <el-button link type="primary" size="small" @click="markAllRead" :disabled="notificationStore.unreadCount === 0">全部已读</el-button>
              </div>
              <div v-if="notificationStore.recentList.length === 0" class="text-center text-gray-400 py-6">
                暂无消息
              </div>
              <div v-else>
                <div
                  v-for="item in notificationStore.recentList"
                  :key="item.id"
                  class="flex items-start py-3 border-b last:border-b-0 cursor-pointer hover:bg-gray-50 rounded px-2 -mx-2"
                  :class="{ 'bg-blue-50': item.isRead === 0 }"
                  @click="handleNotificationClick(item)"
                >
                  <div class="flex-shrink-0 mt-1 mr-3">
                    <el-icon :size="18" :color="getTypeColor(item.type)">
                      <Document v-if="item.type === 'ORDER'" />
                      <Flag v-else-if="item.type === 'ACTIVITY'" />
                      <InfoFilled v-else />
                    </el-icon>
                  </div>
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center justify-between">
                      <span class="text-sm font-medium truncate">{{ item.title }}</span>
                      <span v-if="item.isRead === 0" class="w-2 h-2 bg-red-500 rounded-full flex-shrink-0 ml-2"></span>
                    </div>
                    <p class="text-xs text-gray-500 mt-1 truncate">{{ item.content }}</p>
                    <p class="text-xs text-gray-400 mt-1">{{ formatTime(item.createTime) }}</p>
                  </div>
                </div>
              </div>
              <div class="text-center mt-3 pt-3 border-t">
                <el-button link type="primary" @click="$router.push('/notifications')">查看全部消息</el-button>
              </div>
            </div>
          </el-popover>

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
                <el-dropdown-item @click="$router.push('/member')">会员中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/favorites')">我的收藏</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/orders')">我的订单</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/coupons')">领券中心</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/my-coupons')">我的优惠券</el-dropdown-item>
                <el-dropdown-item @click="$router.push('/notifications')">消息中心</el-dropdown-item>
                <el-dropdown-item v-if="userStore.user?.role === 'ADMIN'" @click="$router.push('/coupon-admin')">优惠券管理</el-dropdown-item>
                <el-dropdown-item v-if="userStore.user?.role === 'ADMIN'" @click="$router.push('/admin/dashboard')">管理仪表盘</el-dropdown-item>
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
import { useNotificationStore } from '@/store/notification'
import { useRouter } from 'vue-router'
import { onMounted } from 'vue'
import { markAllNotificationsRead } from '@/api'

const userStore = useUserStore()
const cartStore = useCartStore()
const notificationStore = useNotificationStore()
const router = useRouter()

const handleLogout = () => {
  userStore.logout()
  router.push('/login')
}

const handleNotificationPopoverShow = () => {
  notificationStore.fetchRecentList()
}

const handleNotificationClick = (item) => {
  if (item.isRead === 0) {
    notificationStore.markReadAndRefresh(item.id)
  }
  if (item.type === 'ORDER' && item.businessId) {
    router.push(`/order/${item.businessId}`)
  }
}

const markAllRead = async () => {
  try {
    await markAllNotificationsRead()
    notificationStore.resetUnread()
    notificationStore.fetchRecentList()
  } catch (e) {
    console.error('标记全部已读失败', e)
  }
}

const getTypeColor = (type) => {
  switch (type) {
    case 'ORDER': return '#409EFF'
    case 'ACTIVITY': return '#E6A23C'
    case 'SYSTEM': return '#909399'
    default: return '#909399'
  }
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString()
}

onMounted(() => {
  cartStore.fetchCart()
  notificationStore.fetchUnreadCount()
})
</script>
