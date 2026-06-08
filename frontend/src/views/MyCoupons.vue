<template>
  <div class="max-w-3xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">我的优惠券</h2>
      <el-button @click="$router.push('/coupons')">
        <el-icon><Ticket /></el-icon>
        去领券
      </el-button>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="未使用" name="0" />
      <el-tab-pane label="已使用" name="1" />
    </el-tabs>

    <div v-if="userCoupons.length > 0" class="space-y-4 mt-4">
      <div
        v-for="uc in userCouponsWithCoupon"
        :key="uc.id"
        class="glass-card overflow-hidden flex coupon-card"
        :class="{ 'coupon-used': uc.status === 1 }"
      >
        <div class="coupon-left flex flex-col items-center justify-center px-6 py-4">
          <span class="text-xs text-white/80 mb-1">{{ uc.coupon?.type === 1 ? '满减券' : '折扣券' }}</span>
          <div class="text-white">
            <span v-if="uc.coupon?.type === 1" class="text-3xl font-bold">¥{{ uc.coupon?.value }}</span>
            <span v-else class="text-3xl font-bold">{{ uc.coupon ? (uc.coupon.value * 10).toFixed(1) : '' }}折</span>
          </div>
          <span v-if="uc.coupon?.threshold > 0" class="text-xs text-white/70 mt-1">满¥{{ uc.coupon?.threshold }}可用</span>
          <span v-else class="text-xs text-white/70 mt-1">无门槛</span>
        </div>
        <div class="flex-grow p-4 flex flex-col justify-between">
          <div>
            <h3 class="font-bold text-gray-800 text-lg">{{ uc.coupon?.type === 1 ? '满减优惠券' : '折扣优惠券' }}</h3>
            <p class="text-gray-400 text-xs mt-1">券码：{{ uc.coupon?.code }}</p>
            <p class="text-gray-400 text-xs mt-1">
              {{ formatDate(uc.coupon?.startTime) }} ~ {{ formatDate(uc.coupon?.endTime) }}
            </p>
          </div>
          <div class="flex items-center justify-end mt-3">
            <el-tag v-if="uc.status === 1" type="info">已使用</el-tag>
            <el-tag v-else-if="isExpired(uc.coupon)" type="danger">已过期</el-tag>
            <el-button v-else type="primary" size="small" @click="$router.push('/cart')">去使用</el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-20">
      <el-empty :description="activeTab === '0' ? '暂无可用优惠券' : '暂无已使用优惠券'">
        <el-button v-if="activeTab === '0'" type="primary" @click="$router.push('/coupons')">去领券</el-button>
      </el-empty>
    </div>

    <div v-if="total > pageSize" class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchUserCoupons"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getMyCoupons, getCoupons } from '@/api'

const activeTab = ref('0')
const userCoupons = ref([])
const allCoupons = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const userCouponsWithCoupon = computed(() => {
  return userCoupons.value.map(uc => ({
    ...uc,
    coupon: allCoupons.value.find(c => c.id === uc.couponId)
  }))
})

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 10)
}

const isExpired = (coupon) => {
  if (!coupon) return false
  return new Date(coupon.endTime) < new Date()
}

const fetchUserCoupons = async () => {
  const data = await getMyCoupons({ page: currentPage.value, pageSize: pageSize.value, status: activeTab.value })
  userCoupons.value = data.records
  total.value = data.total

  const couponIds = data.records.map(uc => uc.couponId)
  if (couponIds.length > 0) {
    const allData = await getCoupons({ page: 1, pageSize: 1000 })
    allCoupons.value = allData.records
  }
}

const handleTabChange = async () => {
  currentPage.value = 1
  await fetchUserCoupons()
}

onMounted(async () => {
  await fetchUserCoupons()
})
</script>

<style scoped>
.coupon-left {
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  min-width: 120px;
  position: relative;
}
.coupon-left::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 50%;
  transform: translateY(-50%);
  width: 12px;
  height: 12px;
  background: white;
  border-radius: 50%;
}
.coupon-used .coupon-left {
  background: linear-gradient(135deg, #999, #bbb);
}
</style>
