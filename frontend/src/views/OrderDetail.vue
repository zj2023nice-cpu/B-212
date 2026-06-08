<template>
  <div class="max-w-3xl mx-auto">
    <div class="glass-card overflow-hidden">
      <div class="bg-primary p-8 text-white text-center">
        <div class="text-3xl font-bold mb-2">
          {{ getStatusText(order?.status) }}
        </div>
        <p v-if="order?.status === 'PENDING_PAYMENT'" class="opacity-80">请在15分钟内完成支付，超时订单将自动取消</p>
        <p v-else-if="order?.status === 'CANCELLED' && order?.cancelReason" class="opacity-80 text-yellow-200">
          取消原因：{{ order.cancelReason }}
        </p>
        <p v-else class="opacity-80">感谢您选择我们的奶茶</p>
      </div>

      <div class="p-8">
        <el-steps
          :active="getStepActive(order?.status)"
          finish-status="success"
          align-center
        >
          <el-step title="待支付" />
          <el-step title="已支付" />
          <el-step title="制作中" />
          <el-step :title="order?.deliveryType === 'SELF_PICKUP' ? '待自提' : '配送中'" />
          <el-step :title="order?.deliveryType === 'SELF_PICKUP' ? '已自提' : '已送达'" />
        </el-steps>
      </div>

      <div v-if="order?.deliveryType === 'SELF_PICKUP'" class="p-8 border-t">
        <h4 class="font-bold mb-3">自提信息</h4>
        <div class="flex items-center gap-2 text-sm mb-2">
          <el-icon class="text-primary"><Shop /></el-icon>
          <span class="font-medium text-gray-800">{{ order.pickupStore }}</span>
        </div>
        <div class="flex items-center gap-2 text-sm text-gray-600">
          <el-icon class="text-primary"><Clock /></el-icon>
          <span>预计自提时间：{{ formatPickupTime(order.pickupTime) }}</span>
        </div>
      </div>

      <div v-if="order?.deliveryType !== 'SELF_PICKUP' && (order?.address || order?.addressFull)" class="p-8 border-t">
        <h4 class="font-bold mb-3">配送信息</h4>
        <div class="flex items-center gap-4 text-sm mb-2">
          <span class="text-gray-600"><span class="font-medium text-gray-800">{{ order.addressContactName }}</span></span>
          <span class="text-gray-600">{{ order.addressPhone }}</span>
        </div>
        <div class="text-sm text-gray-500 mb-4">{{ order.address || order.addressFull }}</div>
        <div class="rounded-xl overflow-hidden border bg-gray-50">
          <img
            :src="deliveryMapUrl"
            alt="配送地址地图示意"
            class="w-full h-40 object-cover"
            @error="$event.target.style.display='none'"
          />
          <div class="flex items-center gap-2 px-4 py-2 bg-white">
            <el-icon class="text-primary"><Location /></el-icon>
            <span class="text-xs text-gray-500 truncate">{{ order.address || order.addressFull }}</span>
          </div>
        </div>
      </div>

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

      <div class="p-4 text-center border-t">
        <el-button plain @click="router.push(`/order/${route.params.id}/receipt`)">
          <el-icon class="mr-1"><Printer /></el-icon>
          打印小票
        </el-button>
      </div>

      <div v-if="order?.status === 'PENDING_PAYMENT'" class="p-8 text-center border-t">
        <p class="text-sm text-gray-400 mb-4">订单待支付，请尽快完成支付</p>
        <div class="flex justify-center gap-4">
          <el-button type="danger" plain @click="openCancelDialog">取消订单</el-button>
          <el-button type="primary" @click="handlePay">立即支付</el-button>
        </div>
      </div>
      <div v-else-if="isInProgress(order?.status)" class="p-8 text-center border-t">
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
      <div v-if="order?.status === 'CANCELLED' && order?.cancelReason" class="p-6 border-t bg-red-50">
        <div class="flex items-center gap-2 text-red-600">
          <el-icon><Warning /></el-icon>
          <span class="font-medium">取消原因：{{ order.cancelReason }}</span>
        </div>
      </div>

      <div v-if="order?.status === 'COMPLETED'" class="p-8 border-t">
        <div class="text-center mb-6">
          <el-button type="primary" size="large" @click="openReviewDialog">
            <el-icon class="mr-1"><EditPen /></el-icon>
            评价订单
          </el-button>
        </div>
      </div>

      <div v-if="order?.status === 'REVIEWED'" class="p-8 border-t bg-green-50">
        <div class="flex items-center justify-center gap-2 text-green-600">
          <el-icon><CircleCheck /></el-icon>
          <span class="font-medium">该订单已完成评价</span>
        </div>
      </div>
    </div>

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

    <el-dialog
      v-model="reviewVisible"
      title="评价订单"
      width="600px"
      :close-on-click-modal="false"
      custom-class="review-dialog"
    >
      <div class="space-y-4 max-h-[60vh] overflow-y-auto pr-2">
        <ReviewForm
          v-for="item in items"
          :key="item.id"
          :ref="el => setReviewRef(item.id, el)"
          :product="item"
          :order-id="order?.id"
        />
      </div>
      <template #footer>
        <div class="flex justify-center">
          <el-button @click="reviewVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmitReview">
            提交评价
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, getOrderItems, updateOrderStatus, submitFeedback } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning, EditPen, CircleCheck, Shop, Clock, Location, Printer } from '@element-plus/icons-vue'
import ReviewForm from '@/components/ReviewForm.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const items = ref([])
const cancelVisible = ref(false)
const reviewVisible = ref(false)
const submitting = ref(false)

const reviewRefs = ref({})

const NEXT_STATUS_MAP = {
  PENDING_PAYMENT: 'PAID',
  PAID: 'PREPARING',
  PREPARING: 'DELIVERING',
  DELIVERING: 'COMPLETED',
  COMPLETED: 'REVIEWED'
}

const setReviewRef = (id, el) => {
  if (el) {
    reviewRefs.value[id] = el
  }
}

const getStatusText = status => {
  const map = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    PREPARING: '制作中',
    DELIVERING: '配送中',
    CANCELLED: '已取消',
    COMPLETED: '已送达',
    REVIEWED: '已评价'
  }
  return map[status] || '未知状态'
}

const getStepActive = status => {
  const stepMap = {
    PENDING_PAYMENT: 0,
    PAID: 1,
    PREPARING: 2,
    DELIVERING: 3,
    COMPLETED: 4,
    REVIEWED: 4,
    CANCELLED: 0
  }
  return stepMap[status] ?? 0
}

const isInProgress = status => {
  return ['PAID', 'PREPARING', 'DELIVERING'].includes(status)
}

const formatSpecs = specsStr => {
  try {
    const s = JSON.parse(specsStr)
    const parts = []
    if (s.size) parts.push(s.size)
    parts.push(s.temp)
    parts.push(s.sugar)
    if (s.topping && s.topping.length > 0) parts.push(s.topping.join('/'))
    return parts.join(' / ')
  } catch (e) {
    return specsStr
  }
}

const formatPickupTime = timeStr => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const deliveryMapUrl = computed(() => {
  const addr = order.value?.address || order.value?.addressFull || ''
  if (!addr) return ''
  const prompt = encodeURIComponent(`a clean minimal map illustration showing a delivery route to an address in a city, ${addr}, simple flat design, light colors, no text, cartographic style`)
  return `https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=${prompt}&image_size=landscape_4_3`
})

const fetchData = async () => {
  const id = route.params.id
  order.value = await getOrderDetail(id)
  items.value = await getOrderItems(id)
}

const simulateProgress = async () => {
  const nextStatus = NEXT_STATUS_MAP[order.value.status]
  if (!nextStatus) return
  await updateOrderStatus(order.value.id, nextStatus)
  fetchData()
}

const handlePay = async () => {
  try {
    await updateOrderStatus(order.value.id, 'PAID')
    ElMessage.success('支付成功')
    fetchData()
  } catch (error) {
    ElMessage.error('支付失败：' + (error.response?.data?.message || error.message))
  }
}

const openCancelDialog = () => {
  cancelVisible.value = true
}

const handleCancelOrder = async () => {
  try {
    await updateOrderStatus(order.value.id, 'CANCELLED')
    ElMessage.success('订单已取消')
    cancelVisible.value = false
    router.push('/orders')
  } catch (error) {
    ElMessage.error('取消订单失败：' + (error.response?.data?.message || error.message))
  }
}

const openReviewDialog = () => {
  reviewRefs.value = {}
  reviewVisible.value = true
}

const handleSubmitReview = async () => {
  const feedbacks = []
  for (const item of items.value) {
    const ref = reviewRefs.value[item.id]
    if (!ref) continue
    if (!ref.validate()) return
    feedbacks.push(ref.getFeedbackData())
  }

  if (feedbacks.length === 0) {
    ElMessage.warning('请至少评价一件商品')
    return
  }

  submitting.value = true
  try {
    await submitFeedback(feedbacks)
    ElMessage.success('评价提交成功，感谢您的反馈！')
    reviewVisible.value = false
    fetchData()
  } catch (error) {
    ElMessage.error('评价提交失败：' + (error.response?.data?.message || error.message))
  } finally {
    submitting.value = false
  }
}

onMounted(fetchData)
</script>

<style>
.review-dialog {
  border-radius: 1.5rem !important;
}
</style>
