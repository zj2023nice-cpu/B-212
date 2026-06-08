<template>
  <div class="max-w-6xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">优惠券管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        创建优惠券
      </el-button>
    </div>

    <div class="glass-card p-4 mb-6">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px" @change="handleFilter">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="coupons" stripe class="glass-card">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="code" label="券码" width="100" />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag :type="row.type === 1 ? 'danger' : 'warning'" size="small">
            {{ row.type === 1 ? '满减' : '折扣' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="门槛" width="90">
        <template #default="{ row }">
          {{ row.threshold > 0 ? '¥' + row.threshold : '无门槛' }}
        </template>
      </el-table-column>
      <el-table-column label="优惠值" width="90">
        <template #default="{ row }">
          {{ row.type === 1 ? '¥' + row.value : (row.value * 10).toFixed(1) + '折' }}
        </template>
      </el-table-column>
      <el-table-column label="有效期" min-width="180">
        <template #default="{ row }">
          {{ formatDate(row.startTime) }} ~ {{ formatDate(row.endTime) }}
        </template>
      </el-table-column>
      <el-table-column label="发行/已领" width="100">
        <template #default="{ row }">
          {{ row.usedCount }} / {{ row.totalCount }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 1"
            type="warning"
            size="small"
            link
            @click="handleToggleStatus(row.id, 0)"
          >禁用</el-button>
          <el-button
            v-else
            type="success"
            size="small"
            link
            @click="handleToggleStatus(row.id, 1)"
          >启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > pageSize" class="mt-6 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @size-change="fetchCoupons"
        @current-change="fetchCoupons"
      />
    </div>

    <el-dialog v-model="createDialogVisible" title="创建优惠券" width="500px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="类型">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="满减券" :value="1" />
            <el-option label="折扣券" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="门槛金额">
          <el-input-number v-model="createForm.threshold" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="createForm.type === 1 ? '减免金额' : '折扣率'">
          <el-input-number
            v-model="createForm.value"
            :min="createForm.type === 1 ? 0.01 : 0.01"
            :max="createForm.type === 1 ? 9999 : 0.99"
            :precision="2"
            :step="createForm.type === 1 ? 1 : 0.05"
            style="width: 100%"
          />
          <div class="text-xs text-gray-400 mt-1">
            {{ createForm.type === 1 ? '订单减免的金额' : '折扣率，如0.8表示8折' }}
          </div>
        </el-form-item>
        <el-form-item label="总发行量">
          <el-input-number v-model="createForm.totalCount" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="有效期">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCoupons, createCoupon, updateCouponStatus } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const coupons = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterStatus = ref(null)

const createDialogVisible = ref(false)
const creating = ref(false)
const dateRange = ref(null)

const createForm = ref({
  type: 1,
  threshold: 0,
  value: 10,
  totalCount: 100
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

const fetchCoupons = async () => {
  const params = { page: currentPage.value, pageSize: pageSize.value }
  if (filterStatus.value !== null && filterStatus.value !== '') {
    params.status = filterStatus.value
  }
  const data = await getCoupons(params)
  coupons.value = data.records
  total.value = data.total
}

const handleFilter = async () => {
  currentPage.value = 1
  await fetchCoupons()
}

const showCreateDialog = () => {
  createForm.value = { type: 1, threshold: 0, value: 10, totalCount: 100 }
  dateRange.value = null
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!dateRange.value || !dateRange.value[0] || !dateRange.value[1]) {
    ElMessage.warning('请选择有效期')
    return
  }

  creating.value = true
  try {
    await createCoupon({
      type: createForm.value.type,
      threshold: createForm.value.threshold,
      value: createForm.value.value,
      totalCount: createForm.value.totalCount,
      startTime: dateRange.value[0].toISOString(),
      endTime: dateRange.value[1].toISOString()
    })
    ElMessage.success('创建成功')
    createDialogVisible.value = false
    await fetchCoupons()
  } catch (e) {} finally {
    creating.value = false
  }
}

const handleToggleStatus = async (id, status) => {
  const action = status === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}该优惠券？`, '提示', { type: 'warning' })
    await updateCouponStatus(id, status)
    ElMessage.success(`${action}成功`)
    await fetchCoupons()
  } catch (e) {}
}

onMounted(() => {
  fetchCoupons()
})
</script>
