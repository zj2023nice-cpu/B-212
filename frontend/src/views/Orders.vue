<template>
  <div class="max-w-4xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">{{ isAdmin ? '订单管理' : '我的订单' }}</h2>
      <el-button v-if="isAdmin" type="success" @click="exportVisible = true">
        <el-icon class="mr-1"><Download /></el-icon>导出订单
      </el-button>
    </div>

    <div v-if="isAdmin" class="mb-4 flex flex-wrap gap-3 items-center">
      <el-select v-model="filterStatus" placeholder="订单状态" clearable style="width: 140px" @change="handleFilterChange">
        <el-option label="待支付" :value="0" />
        <el-option label="制作中" :value="1" />
        <el-option label="配送中" :value="2" />
        <el-option label="已取消" :value="3" />
        <el-option label="已送达" :value="4" />
        <el-option label="已评价" :value="5" />
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
            <div v-if="order.status === 3 && order.cancelReason" class="text-xs text-red-400 mt-1">
              {{ order.cancelReason }}
            </div>
          </div>
        </div>

        <div class="mt-4 flex justify-end gap-3">
          <el-button
            v-if="order.status === 0"
            type="primary"
            size="small"
            @click.stop="handlePay(order)"
            >去支付</el-button
          >
          <el-button
            v-if="order.status === 4"
            type="primary"
            plain
            size="small"
            @click.stop="$router.push(`/order/${order.id}`)"
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
            <el-option label="待支付" :value="0" />
            <el-option label="制作中" :value="1" />
            <el-option label="配送中" :value="2" />
            <el-option label="已取消" :value="3" />
            <el-option label="已送达" :value="4" />
            <el-option label="已评价" :value="5" />
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
import { ref, computed, onMounted } from 'vue'
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

const exportVisible = ref(false)
const exportDateRange = ref(null)
const exportStatus = ref(null)
const exportLoading = ref(false)

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

const openCancelDialog = order => {
  currentOrder.value = order
  cancelVisible.value = true
}

const handlePay = async order => {
  try {
    await updateOrderStatus(order.id, 1)
    ElMessage.success('支付成功，订单制作中')
    fetchOrders()
  } catch (error) {
    ElMessage.error('支付失败：' + (error.response?.data?.message || error.message))
  }
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
  currentPage.value = 1
  fetchOrders()
}

const fetchOrders = async () => {
  if (isAdmin.value) {
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
    const data = await adminListOrders(params)
    orders.value = data.records
    total.value = data.total
  } else {
    const data = await getMyOrders(currentPage.value, pageSize.value)
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

onMounted(fetchOrders)
</script>
