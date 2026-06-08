<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold mb-6 text-gray-800">我的订单</h2>

    <div v-if="orders.length > 0" class="space-y-4">
      <div
        v-for="order in orders"
        :key="order.id"
        class="glass-card p-6 hover:shadow-lg transition-all cursor-pointer"
        @click="$router.push(`/order/${order.id}`)"
      >
        <div class="flex items-center justify-between mb-4">
          <div class="text-sm text-gray-500">
            单号：{{ order.orderSn.substring(0, 12) }}...
          </div>
          <el-tag :type="getStatusType(order.status)" effect="light" round>
            {{ getStatusText(order.status) }}
          </el-tag>
        </div>

        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-2">
            <span class="text-gray-600">总计：</span>
            <span class="text-xl font-bold text-primary">¥{{ order.payAmount || order.totalAmount }}</span>
            <span v-if="order.discountAmount > 0" class="text-xs text-red-500 line-through">¥{{ order.totalAmount }}</span>
          </div>
          <div class="text-xs text-gray-400">
            {{ formatDate(order.createTime) }}
          </div>
        </div>

        <div class="mt-4 flex justify-end gap-3">
          <el-button
            v-if="order.status === 4"
            type="primary"
            plain
            size="small"
            @click.stop="openFeedbackDialog(order)"
            >待评价</el-button
          >
          <el-button
            v-if="order.status === 0 || order.status === 1 || order.status === 2"
            type="danger"
            plain
            size="small"
            @click.stop="openCancelDialog(order)"
            >取消订单</el-button
          >
          <el-button type="info" plain size="small">查看详情</el-button>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-20">
      <el-empty description="暂无订单" />
    </div>

    <!-- Pagination -->
    <div v-if="orders.length > 0" class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="fetchOrders"
      />
    </div>

    <!-- Feedback Dialog -->
    <el-dialog v-model="feedbackVisible" title="评价订单" width="400px">
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="feedbackForm.rating" />
        </el-form-item>
        <el-form-item label="评论内容">
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            placeholder="分享您的饮用体验..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" class="w-full" @click="handleFeedbackSubmit"
          >提交评价</el-button
        >
      </template>
    </el-dialog>

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
import { ref, onMounted, reactive } from 'vue'
import { getMyOrders, submitFeedback, updateOrderStatus } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'

const orders = ref([])
const feedbackVisible = ref(false)
const cancelVisible = ref(false)
const currentOrder = ref(null)
const feedbackForm = reactive({
  rating: 5,
  content: ''
})

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

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

const getStatusType = status => {
  const map = {
    0: 'warning',
    1: 'primary',
    2: 'primary',
    3: 'danger',
    4: 'success',
    5: 'success'
  }
  return map[status]
}

const formatDate = dateStr => {
  return new Date(dateStr).toLocaleString()
}

const openFeedbackDialog = order => {
  currentOrder.value = order
  feedbackVisible.value = true
}

const openCancelDialog = order => {
  currentOrder.value = order
  cancelVisible.value = true
}

const handleCancelOrder = async () => {
  try {
    await updateOrderStatus(currentOrder.value.id, 3)
    ElMessage.success('订单已取消')
    cancelVisible.value = false
    fetchOrders()
  } catch (error) {
    ElMessage.error('取消订单失败：' + (error.response?.data?.message || error.message))
  }
}

const handleFeedbackSubmit = async () => {
  await submitFeedback({
    orderId: currentOrder.value.id,
    rating: feedbackForm.rating,
    content: feedbackForm.content
  })
  ElMessage.success('感谢您的评价！')
  feedbackVisible.value = false
  fetchOrders()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await fetchOrders()
}

const fetchOrders = async () => {
  const data = await getMyOrders(currentPage.value, pageSize.value)
  orders.value = data.records
  total.value = data.total
}

onMounted(fetchOrders)
</script>
