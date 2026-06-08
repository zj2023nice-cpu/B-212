<template>
  <div class="max-w-3xl mx-auto">
    <div class="glass-card overflow-hidden">
      <!-- Header Status -->
      <div class="bg-primary p-8 text-white text-center">
        <div class="text-3xl font-bold mb-2">
          {{ getStatusText(order?.status) }}
        </div>
        <p class="opacity-80">感谢您选择我们的奶茶</p>
      </div>

      <!-- Tracking (Visualized) -->
      <div class="p-8">
        <el-steps
          :active="getStepActive(order?.status)"
          finish-status="success"
          align-center
        >
          <el-step title="已下单" />
          <el-step title="制作中" />
          <el-step title="配送中" />
          <el-step title="已送达" />
        </el-steps>
      </div>

      <!-- Address Section -->
      <div v-if="order?.addressFull" class="p-8 border-t">
        <h4 class="font-bold mb-3">收货信息</h4>
        <div class="flex items-center gap-4 text-sm">
          <span class="text-gray-600"><span class="font-medium text-gray-800">{{ order.addressContactName }}</span></span>
          <span class="text-gray-600">{{ order.addressPhone }}</span>
        </div>
        <div class="text-sm text-gray-500 mt-1">{{ order.addressFull }}</div>
      </div>

      <!-- Items Section -->
      <div class="p-8 bg-gray-50 border-t">
        <h4 class="font-bold mb-4">商品详情</h4>
        <div class="space-y-4">
          <div
            v-for="item in items"
            :key="item.id"
            class="flex justify-between items-center"
          >
            <div>
              <div class="font-medium">{{ item.productName }}</div>
              <div class="text-xs text-gray-400">
                {{ formatSpecs(item.specs) }} x {{ item.quantity }}
              </div>
            </div>
            <div class="font-bold">
              ¥{{ (item.productPrice * item.quantity).toFixed(2) }}
            </div>
          </div>
        </div>
        <div class="mt-6 pt-6 border-t">
          <div class="flex justify-between items-center mb-2">
            <span class="text-gray-500">商品总额</span>
            <span>¥{{ order?.totalAmount }}</span>
          </div>
          <div v-if="order?.discountAmount > 0" class="flex justify-between items-center mb-2">
            <span class="text-gray-500">优惠券减免</span>
            <span class="text-red-500">-¥{{ order.discountAmount }}</span>
          </div>
          <div class="flex justify-between items-center mt-4 pt-4 border-t">
            <span class="text-gray-500 font-bold">实付总计</span>
            <span class="text-2xl font-bold text-primary">¥{{ order?.payAmount || order?.totalAmount }}</span>
          </div>
        </div>
      </div>

      <!-- Logic to simulate status tracking -->
      <div v-if="order?.status < 4 && order?.status !== 3" class="p-8 text-center border-t">
        <p class="text-sm text-gray-400 mb-4 italic">
          系统演示：模拟物流进度推进...
        </p>
        <div class="flex justify-center gap-4">
          <el-button type="danger" plain @click="openCancelDialog"
            >取消订单</el-button
          >
          <el-button type="primary" plain @click="simulateProgress"
            >模拟下一步进度</el-button
          >
        </div>
      </div>
    </div>

    <!-- Cancel Order Dialog -->
    <el-dialog v-model="cancelVisible" title="取消订单" width="400px">
      <div class="text-center py-4">
        <el-icon class="text-4xl text-warning mb-4"><Warning /></el-icon>
        <p class="text-gray-700">确定要取消该订单吗？</p>
        <p class="text-sm text-gray-500 mt-2">取消后库存将自动恢复</p>
      </div>
      <template #footer>
        <div class="flex justify-center gap-4">
          <el-button @click="cancelVisible = false">再想想</el-button>
          <el-button type="danger" @click="handleCancelOrder">确认取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, getOrderItems, updateOrderStatus } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const items = ref([])
const cancelVisible = ref(false)

const getStatusText = status => {
  const map = {
    0: '待支付',
    1: '制作中',
    2: '配送中',
    3: '已取消',
    4: '已送达',
    5: '已评价'
  }
  return map[status]
}

const getStepActive = status => {
  if (status === 3) return 0
  if (status === 1) return 1
  if (status === 2) return 2
  if (status >= 4) return 4
  return 0
}

const formatSpecs = specsStr => {
  try {
    const s = JSON.parse(specsStr)
    return `${s.temp} / ${s.sugar}`
  } catch (e) {
    return specsStr
  }
}

const fetchData = async () => {
  const id = route.params.id
  order.value = await getOrderDetail(id)
  items.value = await getOrderItems(id)
}

const simulateProgress = async () => {
  let nextStatus = order.value.status + 1
  if (nextStatus === 3) nextStatus = 4 // 跳过“已取消”状态
  if (nextStatus > 4) return
  await updateOrderStatus(order.value.id, nextStatus)
  fetchData()
}

const openCancelDialog = () => {
  cancelVisible.value = true
}

const handleCancelOrder = async () => {
  try {
    await updateOrderStatus(order.value.id, 3)
    ElMessage.success('订单已取消')
    cancelVisible.value = false
    router.push('/orders')
  } catch (error) {
    ElMessage.error('取消订单失败：' + (error.response?.data?.message || error.message))
  }
}

onMounted(fetchData)
</script>
