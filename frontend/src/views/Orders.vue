<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">{{ isAdmin ? '订单管理' : '我的订单' }}</h2>
      <el-button v-if="isAdmin" type="success" @click="exportVisible = true">
        <el-icon class="mr-1"><Download /></el-icon>导出订单
      </el-button>
    </div>

    <div class="mb-4 flex flex-wrap gap-3 items-center">
      <el-select v-model="filterStatus" placeholder="订单状态" clearable style="width: 160px" @change="handleFilterChange">
        <el-option label="待支付" value="PENDING_PAYMENT" />
        <el-option label="已支付" value="PAID" />
        <el-option label="制作中" value="PREPARING" />
        <el-option label="配送中" value="DELIVERING" />
        <el-option label="已取消" value="CANCELLED" />
        <el-option label="已送达" value="COMPLETED" />
        <el-option label="已评价" value="REVIEWED" />
      </el-select>
      <el-date-picker
        v-model="filterDateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        style="width: 260px"
        @change="handleFilterChange"
      />
      <el-input
        v-model="filterOrderSn"
        placeholder="订单编号"
        clearable
        style="width: 180px"
        @clear="handleFilterChange"
      />
      <el-input
        v-model="filterProductName"
        placeholder="商品名称"
        clearable
        style="width: 180px"
        @clear="handleFilterChange"
      />
      <el-button type="primary" plain @click="resetFilters">重置筛选</el-button>
    </div>

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
          <div class="text-right">
            <div class="text-xs text-gray-400">
              {{ formatDate(order.createTime) }}
            </div>
            <div v-if="order.status === 'CANCELLED' && order.cancelReason" class="text-xs text-red-400 mt-1">
              {{ order.cancelReason }}
            </div>
          </div>
        </div>

        <div class="mt-4 flex justify-end gap-3">
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            type="primary"
            size="small"
            @click.stop="handlePay(order)"
            >去支付</el-button
          >
          <el-button
            v-if="order.status === 'COMPLETED'"
            type="primary"
            plain
            size="small"
            @click.stop="$router.push(`/order/${order.id}`)"
            >待评价</el-button
          >
          <el-button
            v-if="['PENDING_PAYMENT', 'PAID', 'PREPARING', 'DELIVERING'].includes(order.status)"
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

    <el-dialog v-model="exportVisible" title="导出订单数据" width="480px" :close-on-click-modal="false">
      <el-form label-width="100px">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="exportDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="exportStatus" placeholder="全部状态" clearable style="width: 100%">
            <el-option label="待支付" value="PENDING_PAYMENT" />
            <el-option label="已支付" value="PAID" />
            <el-option label="制作中" value="PREPARING" />
            <el-option label="配送中" value="DELIVERING" />
            <el-option label="已取消" value="CANCELLED" />
            <el-option label="已送达" value="COMPLETED" />
            <el-option label="已评价" value="REVIEWED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="exportVisible = false">取消</el-button>
          <el-button type="primary" :loading="exportLoading" @click="handleExport">确认导出</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { getMyOrders, updateOrderStatus, adminListOrders, exportOrders } from '@/api'
import { ElMessage } from 'element-plus'
import { Warning, Download } from '@element-plus/icons-vue'

const orders = ref([])
const cancelVisible = ref(false)
const currentOrder = ref(null)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const user = computed(() => JSON.parse(localStorage.getItem('user') || 'null'))
const isAdmin = computed(() => user.value?.role === 'ADMIN')

const filterStatus = ref(null)
const filterDateRange = ref(null)
const filterOrderSn = ref('')
const filterProductName = ref('')

const exportVisible = ref(false)
const exportDateRange = ref(null)
const exportStatus = ref(null)
const exportLoading = ref(false)

const STATUS_MAP = {
  PENDING_PAYMENT: { text: '待支付', type: 'warning' },
  PAID: { text: '已支付', type: '' },
  PREPARING: { text: '制作中', type: 'primary' },
  DELIVERING: { text: '配送中', type: 'primary' },
  COMPLETED: { text: '已送达', type: 'success' },
  CANCELLED: { text: '已取消', type: 'danger' },
  REVIEWED: { text: '已评价', type: 'success' }
}

const getStatusText = status => STATUS_MAP[status]?.text || '未知状态'
const getStatusType = status => STATUS_MAP[status]?.type || 'info'

const formatDate = dateStr => {
  return new Date(dateStr).toLocaleString()
}

const openCancelDialog = order => {
  currentOrder.value = order
  cancelVisible.value = true
}

const handlePay = async order => {
  try {
    await updateOrderStatus(order.id, 'PAID')
    ElMessage.success('支付成功')
    fetchOrders()
  } catch (error) {
    ElMessage.error('支付失败：' + (error.response?.data?.message || error.message))
  }
}

const handleCancelOrder = async () => {
  try {
    await updateOrderStatus(currentOrder.value.id, 'CANCELLED')
    ElMessage.success('订单已取消')
    cancelVisible.value = false
    fetchOrders()
  } catch (error) {
    ElMessage.error('取消订单失败：' + (error.response?.data?.message || error.message))
  }
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await fetchOrders()
}

const handleFilterChange = () => {
  currentPage.value = 1
  fetchOrders()
}

const resetFilters = () => {
  filterStatus.value = null
  filterDateRange.value = null
  filterOrderSn.value = ''
  filterProductName.value = ''
  currentPage.value = 1
  fetchOrders()
}

const buildFilterParams = () => {
  const params = {
    page: currentPage.value,
    pageSize: pageSize.value
  }
  if (filterStatus.value !== null && filterStatus.value !== undefined) {
    params.status = filterStatus.value
  }
  if (filterDateRange.value && filterDateRange.value.length === 2) {
    params.startDate = filterDateRange.value[0]
    params.endDate = filterDateRange.value[1]
  }
  if (filterOrderSn.value.trim()) {
    params.orderSn = filterOrderSn.value.trim()
  }
  if (filterProductName.value.trim()) {
    params.productName = filterProductName.value.trim()
  }
  return params
}

const fetchOrders = async () => {
  const params = buildFilterParams()
  if (isAdmin.value) {
    const data = await adminListOrders(params)
    orders.value = data.records
    total.value = data.total
  } else {
    const data = await getMyOrders(params)
    orders.value = data.records
    total.value = data.total
  }
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    const params = {}
    if (exportDateRange.value && exportDateRange.value.length === 2) {
      params.startDate = exportDateRange.value[0]
      params.endDate = exportDateRange.value[1]
    }
    if (exportStatus.value !== null && exportStatus.value !== undefined) {
      params.status = exportStatus.value
    }

    const blob = await exportOrders(params)

    if (blob.type && blob.type.includes('application/json')) {
      const text = await blob.text()
      const errorData = JSON.parse(text)
      ElMessage.error(errorData.message || '导出失败')
      return
    }

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const today = new Date().toISOString().slice(0, 10)
    link.setAttribute('download', `订单数据_${today}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('订单导出成功')
    exportVisible.value = false
  } catch (error) {
    ElMessage.error('导出失败：' + (error.message || '未知错误'))
  } finally {
    exportLoading.value = false
  }
}

let debounceTimer = null

const debouncedFilterChange = () => {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    currentPage.value = 1
    fetchOrders()
  }, 400)
}

watch(filterOrderSn, () => debouncedFilterChange())
watch(filterProductName, () => debouncedFilterChange())

onUnmounted(() => {
  clearTimeout(debounceTimer)
})

onMounted(fetchOrders)
</script>
