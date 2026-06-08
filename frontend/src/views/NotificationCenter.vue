<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold">消息中心</h2>
      <div class="flex items-center space-x-3">
        <el-button type="primary" size="small" :disabled="selectedIds.length === 0" @click="batchMarkRead">
          批量标记已读 ({{ selectedIds.length }})
        </el-button>
        <el-button size="small" @click="handleMarkAllRead" :disabled="unreadCount === 0">
          全部已读
        </el-button>
      </div>
    </div>

    <div class="mb-4">
      <el-radio-group v-model="filterType" @change="handleFilterChange">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button label="ORDER">订单</el-radio-button>
        <el-radio-button label="ACTIVITY">活动</el-radio-button>
        <el-radio-button label="SYSTEM">系统</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading" class="text-center py-10">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
    </div>

    <div v-else-if="notifications.length === 0" class="text-center text-gray-400 py-16">
      <el-icon :size="48"><BellFilled /></el-icon>
      <p class="mt-4">暂无消息</p>
    </div>

    <div v-else>
      <div
        v-for="item in notifications"
        :key="item.id"
        class="bg-white rounded-lg shadow-sm mb-3 p-4 flex items-start hover:shadow-md transition-shadow"
        :class="{ 'border-l-4 border-blue-400': item.isRead === 0 }"
      >
        <el-checkbox
          v-model="item._selected"
          class="mr-3 mt-1"
          @change="(val) => handleSelectChange(item.id, val)"
        />
        <div class="flex-shrink-0 mt-1 mr-3">
          <el-icon :size="22" :color="getTypeColor(item.type)">
            <Document v-if="item.type === 'ORDER'" />
            <Flag v-else-if="item.type === 'ACTIVITY'" />
            <InfoFilled v-else />
          </el-icon>
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center justify-between">
            <div class="flex items-center">
              <span class="font-medium" :class="{ 'font-bold': item.isRead === 0 }">{{ item.title }}</span>
              <el-tag size="small" :type="getTypeTag(item.type)" class="ml-2">{{ getTypeLabel(item.type) }}</el-tag>
              <span v-if="item.isRead === 0" class="w-2 h-2 bg-red-500 rounded-full ml-2"></span>
            </div>
            <span class="text-xs text-gray-400 flex-shrink-0 ml-4">{{ formatTime(item.createTime) }}</span>
          </div>
          <p class="text-sm text-gray-600 mt-1">{{ item.content }}</p>
          <div class="flex items-center space-x-3 mt-2">
            <el-button
              v-if="item.isRead === 0"
              link
              type="primary"
              size="small"
              @click="handleMarkRead(item)"
            >标记已读</el-button>
            <el-button
              v-if="item.type === 'ORDER' && item.businessId"
              link
              type="primary"
              size="small"
              @click="goToOrder(item.businessId)"
            >查看订单</el-button>
            <el-popconfirm title="确认删除此消息？" @confirm="handleDelete(item.id)">
              <template #reference>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>

      <div class="flex justify-center mt-6">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getNotifications,
  markNotificationRead,
  markAllNotificationsRead,
  batchMarkNotificationsRead,
  deleteNotification,
  getUnreadCount
} from '@/api'
import { useNotificationStore } from '@/store/notification'

const router = useRouter()
const notificationStore = useNotificationStore()

const notifications = ref([])
const loading = ref(false)
const filterType = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref([])
const unreadCount = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (filterType.value) {
      params.type = filterType.value
    }
    const data = await getNotifications(params)
    notifications.value = (data.records || []).map(item => ({ ...item, _selected: false }))
    total.value = data.total || 0
    selectedIds.value = []
    await refreshUnreadCount()
  } catch (e) {
    console.error('获取通知列表失败', e)
  } finally {
    loading.value = false
  }
}

const refreshUnreadCount = async () => {
  try {
    const count = await getUnreadCount()
    unreadCount.value = count || 0
    notificationStore.unreadCount = count || 0
  } catch (e) {
    console.error('获取未读数量失败', e)
  }
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchData()
}

const handleMarkRead = async (item) => {
  try {
    await markNotificationRead(item.id)
    item.isRead = 1
    await refreshUnreadCount()
    ElMessage.success('已标记为已读')
  } catch (e) {
    console.error('标记已读失败', e)
  }
}

const handleMarkAllRead = async () => {
  try {
    await markAllNotificationsRead()
    notifications.value.forEach(item => { item.isRead = 1 })
    notificationStore.resetUnread()
    unreadCount.value = 0
    ElMessage.success('已全部标记为已读')
  } catch (e) {
    console.error('全部标记已读失败', e)
  }
}

const handleSelectChange = (id, val) => {
  if (val) {
    if (!selectedIds.value.includes(id)) {
      selectedIds.value.push(id)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(i => i !== id)
  }
}

const batchMarkRead = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await batchMarkNotificationsRead(selectedIds.value)
    notifications.value.forEach(item => {
      if (selectedIds.value.includes(item.id)) {
        item.isRead = 1
        item._selected = false
      }
    })
    selectedIds.value = []
    await refreshUnreadCount()
    ElMessage.success('批量标记已读成功')
  } catch (e) {
    console.error('批量标记已读失败', e)
  }
}

const handleDelete = async (id) => {
  try {
    await deleteNotification(id)
    await fetchData()
    ElMessage.success('删除成功')
  } catch (e) {
    console.error('删除失败', e)
  }
}

const goToOrder = (orderId) => {
  router.push(`/order/${orderId}`)
}

const getTypeColor = (type) => {
  switch (type) {
    case 'ORDER': return '#409EFF'
    case 'ACTIVITY': return '#E6A23C'
    case 'SYSTEM': return '#909399'
    default: return '#909399'
  }
}

const getTypeTag = (type) => {
  switch (type) {
    case 'ORDER': return ''
    case 'ACTIVITY': return 'warning'
    case 'SYSTEM': return 'info'
    default: return 'info'
  }
}

const getTypeLabel = (type) => {
  switch (type) {
    case 'ORDER': return '订单'
    case 'ACTIVITY': return '活动'
    case 'SYSTEM': return '系统'
    default: return '其他'
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
  fetchData()
})
</script>
