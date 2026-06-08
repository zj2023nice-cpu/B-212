<template>
  <div class="max-w-5xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">领券中心</h2>
      <el-button @click="$router.push('/my-coupons')">
        <el-icon><Ticket /></el-icon>
        我的优惠券
      </el-button>
    </div>

    <div v-if="coupons.length > 0" class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div
        v-for="coupon in coupons"
        :key="coupon.id"
        class="glass-card overflow-hidden flex coupon-card"
      >
        <div class="coupon-left flex flex-col items-center justify-center px-6 py-4">
          <span class="text-xs text-white/80 mb-1">{{ coupon.type === 1 ? '满减券' : '折扣券' }}</span>
          <div class="text-white">
            <span v-if="coupon.type === 1" class="text-3xl font-bold">¥{{ coupon.value }}</span>
            <span v-else class="text-3xl font-bold">{{ (coupon.value * 10).toFixed(1) }}折</span>
          </div>
          <span v-if="coupon.threshold > 0" class="text-xs text-white/70 mt-1">满¥{{ coupon.threshold }}可用</span>
          <span v-else class="text-xs text-white/70 mt-1">无门槛</span>
        </div>
        <div class="flex-grow p-4 flex flex-col justify-between">
          <div>
            <h3 class="font-bold text-gray-800 text-lg">{{ coupon.type === 1 ? '满减优惠券' : '折扣优惠券' }}</h3>
            <p class="text-gray-400 text-xs mt-1">券码：{{ coupon.code }}</p>
            <p class="text-gray-400 text-xs mt-1">
              {{ formatDate(coupon.startTime) }} ~ {{ formatDate(coupon.endTime) }}
            </p>
          </div>
          <div class="flex items-center justify-between mt-3">
            <span class="text-xs text-gray-500">剩余 {{ coupon.totalCount - coupon.usedCount }} 张</span>
            <el-button
              type="primary"
              size="small"
              :disabled="claimedIds.has(coupon.id) || coupon.usedCount >= coupon.totalCount || coupon.status !== 1"
              @click="handleClaim(coupon.id)"
            >
              {{ claimedIds.has(coupon.id) ? '已领取' : (coupon.usedCount >= coupon.totalCount ? '已领完' : '立即领取') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-20">
      <el-empty description="暂无可领取的优惠券" />
    </div>

    <div v-if="total > pageSize" class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchCoupons"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCoupons, claimCoupon, getMyCoupons } from '@/api'
import { ElMessage } from 'element-plus'

const coupons = ref([])
const claimedIds = ref(new Set())
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 10)
}

const fetchCoupons = async () => {
  const data = await getCoupons({ page: currentPage.value, pageSize: pageSize.value, status: 1 })
  coupons.value = data.records
  total.value = data.total
}

const fetchClaimedCoupons = async () => {
  try {
    const data = await getMyCoupons({ page: 1, pageSize: 1000 })
    const ids = new Set()
    data.records.forEach(uc => ids.add(uc.couponId))
    claimedIds.value = ids
  } catch (e) {}
}

const handleClaim = async (couponId) => {
  try {
    await claimCoupon(couponId)
    ElMessage.success('领取成功！')
    claimedIds.value.add(couponId)
    await fetchCoupons()
  } catch (e) {}
}

onMounted(async () => {
  await fetchCoupons()
  await fetchClaimedCoupons()
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
.coupon-card {
  position: relative;
}
</style>
