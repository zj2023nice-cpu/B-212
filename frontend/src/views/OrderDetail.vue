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

      <div class="p-4 text-center border-t no-print">
        <el-button plain @click="handlePrint">
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

    <div v-if="showReceipt" class="receipt-overlay no-print" @click.self="closeReceipt">
      <div class="receipt-modal">
        <div class="receipt-modal-header">
          <span>小票预览</span>
          <el-button text @click="closeReceipt">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="receipt-wrapper">
          <div class="receipt" id="receipt-content">
            <div class="receipt-header">
              <div class="store-name">{{ receiptData.storeName }}</div>
              <div class="store-phone">TEL: {{ receiptData.storePhone }}</div>
            </div>

            <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

            <div class="receipt-info">
              <div class="receipt-row">
                <span>单号:</span>
                <span class="receipt-value">{{ receiptData.orderSn }}</span>
              </div>
              <div class="receipt-row">
                <span>时间:</span>
                <span class="receipt-value">{{ receiptData.orderTime }}</span>
              </div>
            </div>

            <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

            <div class="receipt-section-title">商品明细</div>

            <div class="receipt-items">
              <div class="receipt-item" v-for="(item, index) in receiptData.items" :key="index">
                <div class="item-name">{{ item.productName }}</div>
                <div class="item-specs" v-if="item.specs">{{ item.specs }}</div>
                <div class="item-detail">
                  <span class="item-qty">x{{ item.quantity }}</span>
                  <span class="item-price">{{ item.unitPrice }}</span>
                  <span class="item-subtotal">{{ item.subtotal }}</span>
                </div>
              </div>
            </div>

            <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

            <div class="receipt-totals">
              <div class="receipt-row">
                <span>商品总额:</span>
                <span class="receipt-value">¥{{ receiptData.totalAmount }}</span>
              </div>
              <div class="receipt-row" v-if="receiptData.discountAmount > 0">
                <span>优惠减免:</span>
                <span class="receipt-value discount">-¥{{ receiptData.discountAmount }}</span>
              </div>
              <div class="receipt-divider">--------------------------------</div>
              <div class="receipt-row total">
                <span>实付总计:</span>
                <span class="receipt-value">¥{{ receiptData.payAmount }}</span>
              </div>
            </div>

            <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

            <div class="receipt-delivery">
              <div class="receipt-row">
                <span>配送方式:</span>
                <span class="receipt-value">{{ receiptData.deliveryType }}</span>
              </div>
              <template v-if="receiptData.deliveryType === '门店自提'">
                <div class="receipt-row" v-if="receiptData.pickupStore">
                  <span>自提门店:</span>
                  <span class="receipt-value">{{ receiptData.pickupStore }}</span>
                </div>
                <div class="receipt-row" v-if="receiptData.pickupTime">
                  <span>自提时间:</span>
                  <span class="receipt-value">{{ receiptData.pickupTime }}</span>
                </div>
              </template>
              <template v-else>
                <div class="receipt-row" v-if="receiptData.contactName">
                  <span>联系人:</span>
                  <span class="receipt-value">{{ receiptData.contactName }} {{ receiptData.contactPhone }}</span>
                </div>
                <div class="receipt-row" v-if="receiptData.address">
                  <span>地址:</span>
                  <span class="receipt-value address-value">{{ receiptData.address }}</span>
                </div>
              </template>
            </div>

            <div class="receipt-remark" v-if="receiptData.remark">
              <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>
              <div class="receipt-row">
                <span>备注:</span>
                <span class="receipt-value">{{ receiptData.remark }}</span>
              </div>
            </div>

            <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

            <div class="receipt-footer">
              <div>感谢您的惠顾，欢迎再次光临!</div>
              <div class="receipt-qr-hint">--- 小票结束 ---</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, getOrderItems, getOrderReceipt, updateOrderStatus, submitFeedback } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning, EditPen, CircleCheck, Shop, Clock, Location, Printer, Close } from '@element-plus/icons-vue'
import ReviewForm from '@/components/ReviewForm.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const items = ref([])
const cancelVisible = ref(false)
const reviewVisible = ref(false)
const submitting = ref(false)
const showReceipt = ref(false)
const receiptData = ref({
  storeName: '',
  storePhone: '',
  orderSn: '',
  orderTime: '',
  deliveryType: '',
  pickupStore: '',
  pickupTime: '',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
  items: [],
  totalAmount: '0.00',
  discountAmount: '0.00',
  payAmount: '0.00'
})

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

const handlePrint = async () => {
  try {
    const data = await getOrderReceipt(route.params.id)
    receiptData.value = data
    showReceipt.value = true
    await nextTick()
    window.print()
  } catch (error) {
    ElMessage.error('获取小票失败：' + (error.response?.data?.message || error.message))
  }
}

const closeReceipt = () => {
  showReceipt.value = false
}

onMounted(fetchData)
</script>

<style>
.review-dialog {
  border-radius: 1.5rem !important;
}

@media print {
  body * {
    visibility: hidden;
  }

  .receipt-overlay,
  .receipt-overlay * {
    visibility: visible;
  }

  .receipt-overlay {
    position: absolute;
    inset: 0;
    background: #fff;
    padding: 0;
    z-index: 9999;
  }

  .receipt-overlay .no-print,
  .receipt-overlay .receipt-modal-header {
    display: none !important;
  }

  .receipt-overlay .receipt-modal {
    background: #fff;
    border-radius: 0;
    padding: 0;
    max-height: none;
    overflow: visible;
  }

  .receipt-overlay .receipt-wrapper {
    justify-content: flex-start;
  }

  .receipt-overlay .receipt {
    box-shadow: none;
    margin: 0;
    padding: 8px;
    width: 58mm;
  }
}
</style>

<style scoped>
.receipt-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 40px;
  z-index: 2000;
  overflow-y: auto;
}

.receipt-modal {
  background: #f0f0f0;
  border-radius: 12px;
  padding: 16px;
  max-height: calc(100vh - 80px);
  overflow-y: auto;
}

.receipt-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: bold;
  font-size: 16px;
  color: #333;
}

.receipt-wrapper {
  display: flex;
  justify-content: center;
}

.receipt {
  width: 232px;
  background: #fff;
  padding: 12px 10px;
  font-family: 'Courier New', 'Lucida Console', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #000;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.receipt-header {
  text-align: center;
  margin-bottom: 4px;
}

.store-name {
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 2px;
}

.store-phone {
  font-size: 11px;
  margin-top: 2px;
}

.receipt-divider {
  border: none;
  font-size: 10px;
  letter-spacing: -1px;
  color: #999;
  text-align: center;
  line-height: 1.2;
  margin: 6px 0;
  overflow: hidden;
  white-space: nowrap;
}

.receipt-info {
  margin: 4px 0;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 4px;
}

.receipt-value {
  text-align: right;
  word-break: break-all;
}

.address-value {
  max-width: 140px;
  text-align: right;
}

.receipt-section-title {
  font-weight: bold;
  text-align: center;
  margin: 4px 0;
}

.receipt-items {
  margin: 4px 0;
}

.receipt-item {
  margin-bottom: 6px;
}

.item-name {
  font-weight: bold;
}

.item-specs {
  font-size: 10px;
  color: #666;
  margin-top: 1px;
}

.item-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 2px;
}

.item-qty {
  flex: 1;
}

.item-price {
  width: 60px;
  text-align: right;
}

.item-subtotal {
  width: 60px;
  text-align: right;
  font-weight: bold;
}

.receipt-totals {
  margin: 4px 0;
}

.receipt-totals .receipt-row {
  margin: 2px 0;
}

.receipt-totals .total {
  font-weight: bold;
  font-size: 14px;
  margin-top: 4px;
}

.discount {
  color: #e53e3e;
}

.receipt-delivery {
  margin: 4px 0;
}

.receipt-remark {
  margin: 4px 0;
}

.receipt-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 11px;
}

.receipt-qr-hint {
  margin-top: 6px;
  font-size: 10px;
  color: #999;
}
</style>
