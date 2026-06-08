<template>
  <div class="max-w-3xl mx-auto">
    <div class="glass-card overflow-hidden">
      <div class="bg-primary p-8 text-white text-center">
        <div class="text-3xl font-bold mb-2">
          {{ getStatusText(order?.status) }}
        </div>
        <p v-if="order?.status === 0" class="opacity-80">请在15分钟内完成支付，超时订单将自动取消</p>
        <p v-else-if="order?.status === 3 && order?.cancelReason" class="opacity-80 text-yellow-200">
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
          <el-step title="制作中" />
          <el-step title="配送中" />
          <el-step title="已送达" />
        </el-steps>
      </div>

      <div v-if="order?.addressFull" class="p-8 border-t">
        <h4 class="font-bold mb-3">收货信息</h4>
        <div class="flex items-center gap-4 text-sm">
          <span class="text-gray-600"><span class="font-medium text-gray-800">{{ order.addressContactName }}</span></span>
          <span class="text-gray-600">{{ order.addressPhone }}</span>
        </div>
        <div class="text-sm text-gray-500 mt-1">{{ order.addressFull }}</div>
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

      <div v-if="order?.status === 0" class="p-8 text-center border-t">
        <p class="text-sm text-gray-400 mb-4">订单待支付，请尽快完成支付</p>
        <div class="flex justify-center gap-4">
          <el-button type="danger" plain @click="openCancelDialog">取消订单</el-button>
          <el-button type="primary" @click="handlePay">立即支付</el-button>
        </div>
      </div>
      <div v-else-if="order?.status < 4 && order?.status !== 3" class="p-8 text-center border-t">
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
      <div v-if="order?.status === 3 && order?.cancelReason" class="p-6 border-t bg-red-50">
        <div class="flex items-center gap-2 text-red-600">
          <el-icon><Warning /></el-icon>
          <span class="font-medium">取消原因：{{ order.cancelReason }}</span>
        </div>
      </div>

      <div v-if="order?.status === 4" class="p-8 border-t">
        <div class="text-center mb-6">
          <el-button type="primary" size="large" @click="openReviewDialog">
            <el-icon class="mr-1"><EditPen /></el-icon>
            评价订单
          </el-button>
        </div>
      </div>

      <div v-if="order?.status === 5" class="p-8 border-t bg-green-50">
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
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, getOrderItems, updateOrderStatus, submitFeedback } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning, EditPen, CircleCheck } from '@element-plus/icons-vue'
import ReviewForm from '@/components/ReviewForm.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const items = ref([])
const cancelVisible = ref(false)
const reviewVisible = ref(false)
const submitting = ref(false)

const reviewRefs = ref({})

const setReviewRef = (id, el) => {
  if (el) {
    reviewRefs.value[id] = el
  }
}

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
  if (status === 0) return 0
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
  if (nextStatus === 3) nextStatus = 4
  if (nextStatus > 4) return
  await updateOrderStatus(order.value.id, nextStatus)
  fetchData()
}

const handlePay = async () => {
  try {
    await updateOrderStatus(order.value.id, 1)
    ElMessage.success('支付成功，订单制作中')
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
    await updateOrderStatus(order.value.id, 3)
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
