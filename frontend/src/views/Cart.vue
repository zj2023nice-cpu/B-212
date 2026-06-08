<template>
  <div class="max-w-3xl mx-auto">
    <h2 class="text-2xl font-bold mb-6 text-gray-800">购物车</h2>
    
    <div v-if="cartStore.groups.length > 0" class="space-y-4">
      <div v-for="group in cartStore.groups" :key="group.productId" class="glass-card p-4">
        <div class="flex items-center gap-4">
          <img :src="group.image" class="w-20 h-20 rounded-xl object-cover" />
          <div class="flex-grow">
            <h3 class="font-bold text-gray-800">{{ group.productName }}</h3>
            <div class="mt-1 text-primary font-bold">¥{{ group.price }}<span class="text-xs text-gray-400 font-normal ml-1">起</span></div>
          </div>
        </div>
        <div class="mt-3 space-y-2">
          <div v-for="spec in group.specs" :key="spec.id" class="flex items-center gap-3 pl-2 py-2 border-t border-gray-100">
            <div class="flex-grow">
              <span class="text-gray-500 text-sm">{{ formatSpecs(spec.specs) }}</span>
              <span class="text-primary font-bold text-sm ml-2">¥{{ spec.unitPrice }}</span>
            </div>
            <el-input-number v-model="spec.quantity" :min="1" size="small" @change="(val) => handleUpdateQuantity(spec.id, val)" />
            <el-button type="danger" link @click="handleRemove(spec.id)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>

      <div class="glass-card p-6 mt-8 flex items-center justify-between">
        <div>
          <span class="text-gray-500">共 {{ cartStore.totalCount }} 件商品</span>
          <div class="text-2xl font-bold text-primary mt-1">合计：¥{{ totalPrice }}</div>
        </div>
        <el-button type="primary" size="large" class="!px-10 !rounded-xl" @click="handleCheckout">去结算</el-button>
      </div>
    </div>

    <div v-else class="text-center py-20">
      <el-empty description="购物车空空如也">
        <el-button type="primary" @click="$router.push('/')">去点餐</el-button>
      </el-empty>
    </div>

    <el-dialog v-model="checkoutVisible" title="确认订单" width="500px">
      <el-form label-position="top">
        <el-form-item label="收货地址">
          <div v-if="selectedAddress" class="w-full border rounded-lg p-3 cursor-pointer hover:border-primary transition-all" @click="addressDialogVisible = true">
            <div class="flex items-center gap-2">
              <span class="font-bold">{{ selectedAddress.contactName }}</span>
              <span class="text-gray-500">{{ selectedAddress.phone }}</span>
              <el-tag v-if="selectedAddress.isDefault === 1" size="small" type="danger">默认</el-tag>
            </div>
            <div class="text-sm text-gray-600 mt-1">{{ selectedAddress.province }}{{ selectedAddress.city }}{{ selectedAddress.district }}{{ selectedAddress.detailAddress }}</div>
          </div>
          <el-button v-else type="primary" plain class="w-full" @click="addressDialogVisible = true">选择收货地址</el-button>
        </el-form-item>
        <el-form-item label="选择优惠券">
          <el-select
            v-model="selectedCouponId"
            placeholder="不使用优惠券"
            clearable
            style="width: 100%"
            @change="handleCouponChange"
          >
            <el-option
              v-for="uc in availableCoupons"
              :key="uc.id"
              :label="getCouponLabel(uc)"
              :value="uc.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="discountAmount > 0 || memberDiscountAmount > 0" label="">
          <div class="w-full flex items-center justify-between text-sm">
            <span class="text-gray-500">商品总额</span>
            <span>¥{{ totalPrice }}</span>
          </div>
          <div v-if="discountAmount > 0" class="w-full flex items-center justify-between text-sm mt-2">
            <span class="text-gray-500">优惠券减免</span>
            <span class="text-red-500">-¥{{ discountAmount }}</span>
          </div>
          <div v-if="memberDiscountAmount > 0" class="w-full flex items-center justify-between text-sm mt-2">
            <span class="text-gray-500">会员折扣({{ memberLevelName }} {{ (memberDiscountRate * 100).toFixed(0) }}%)</span>
            <span class="text-red-500">-¥{{ memberDiscountAmount }}</span>
          </div>
          <el-divider class="my-3" />
          <div class="w-full flex items-center justify-between">
            <span class="font-bold">实付金额</span>
            <span class="text-xl font-bold text-primary">¥{{ finalPrice }}</span>
          </div>
        </el-form-item>
        <el-form-item label="备注信息">
          <el-input v-model="remark" type="textarea" placeholder="口味要求等..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold text-primary">应付：¥{{ finalPrice }}</span>
          <el-button type="primary" @click="confirmOrder" :loading="submitting">确认付款</el-button>
        </div>
      </template>
    </el-dialog>

    <AddressDialog v-model="addressDialogVisible" :selectedAddressId="selectedAddress?.id" @select="handleAddressSelect" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useCartStore } from '@/store/cart'
import { useAddressStore } from '@/store/address'
import { updateCartItem, removeCartItem, createOrder, getAvailableCoupons, applyCoupon, getMemberDiscount } from '@/api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AddressDialog from '@/views/AddressDialog.vue'

const cartStore = useCartStore()
const addressStore = useAddressStore()
const router = useRouter()
const checkoutVisible = ref(false)
const remark = ref('')
const submitting = ref(false)
const availableCoupons = ref([])
const selectedCouponId = ref(null)
const discountAmount = ref(0)
const memberDiscountAmount = ref(0)
const memberDiscountRate = ref(0)
const memberLevelName = ref('普通会员')
const addressDialogVisible = ref(false)
const selectedAddress = ref(null)

const totalPrice = computed(() => {
  return cartStore.groups.reduce((sum, g) => sum + (g.specs || []).reduce((s, item) => s + (Number(item.unitPrice) || 0) * item.quantity, 0), 0).toFixed(2)
})

const finalPrice = computed(() => {
  const val = Math.max(0, parseFloat(totalPrice.value) - discountAmount.value - memberDiscountAmount.value)
  return val.toFixed(2)
})

const formatSpecs = (specsStr) => {
  try {
    const s = JSON.parse(specsStr)
    const parts = []
    if (s.size) parts.push(s.size)
    parts.push(s.temp)
    parts.push(s.sugar)
    if (s.topping && s.topping.length > 0) parts.push(s.topping.join('/'))
    return parts.join(' / ')
  } catch (e) { return specsStr }
}

const getCouponLabel = (uc) => {
  const coupon = uc.coupon
  if (!coupon) return '优惠券'
  const typeStr = coupon.type === 1 ? `满减¥${coupon.value}` : `${(coupon.value * 10).toFixed(1)}折`
  const thresholdStr = coupon.threshold > 0 ? `满¥${coupon.threshold}` : '无门槛'
  return `[${typeStr}] ${thresholdStr} - ${coupon.code}`
}

const handleUpdateQuantity = async (id, val) => {
  await updateCartItem(id, val)
  await cartStore.fetchCart()
}

const handleRemove = async (id) => {
  await removeCartItem(id)
  await cartStore.fetchCart()
}

const fetchMemberDiscount = async (baseAmount) => {
  try {
    const memberData = await getMemberDiscount({ amount: baseAmount })
    memberDiscountAmount.value = parseFloat(memberData.discountAmount) || 0
    memberDiscountRate.value = parseFloat(memberData.discountRate) || 0
    const rateToName = { 0: '普通会员', '0.05': '银卡会员', '0.10': '金卡会员', '0.15': '黑卡会员' }
    memberLevelName.value = rateToName[String(memberData.discountRate)] || '普通会员'
  } catch (e) {
    memberDiscountAmount.value = 0
    memberDiscountRate.value = 0
    memberLevelName.value = '普通会员'
  }
}

const handleCheckout = async () => {
  selectedCouponId.value = null
  discountAmount.value = 0
  checkoutVisible.value = true
  try {
    await addressStore.fetchAddresses()
    if (selectedAddress.value) {
      const still = addressStore.addresses.find(a => a.id === selectedAddress.value.id)
      if (!still) selectedAddress.value = null
    }
    if (!selectedAddress.value && addressStore.defaultAddress) {
      selectedAddress.value = addressStore.defaultAddress
    }
  } catch (e) {}
  try {
    const amount = parseFloat(totalPrice.value)
    const data = await getAvailableCoupons({ orderAmount: amount })
    availableCoupons.value = data.map(uc => ({
      ...uc,
      coupon: uc.coupon || null
    }))
    const couponIds = availableCoupons.value.map(uc => uc.couponId)
    if (couponIds.length > 0) {
      const { getCoupons } = await import('@/api')
      const allData = await getCoupons({ page: 1, pageSize: 1000 })
      const couponMap = {}
      allData.records.forEach(c => { couponMap[c.id] = c })
      availableCoupons.value = availableCoupons.value.map(uc => ({
        ...uc,
        coupon: couponMap[uc.couponId] || null
      }))
    }
  } catch (e) {
    availableCoupons.value = []
  }
  await fetchMemberDiscount(parseFloat(totalPrice.value))
}

const handleCouponChange = async (val) => {
  if (!val) {
    discountAmount.value = 0
    await fetchMemberDiscount(parseFloat(totalPrice.value))
    return
  }
  try {
    const data = await applyCoupon({
      userCouponId: val,
      orderAmount: parseFloat(totalPrice.value)
    })
    discountAmount.value = parseFloat(data.discount) || 0
    const afterCoupon = Math.max(0, parseFloat(totalPrice.value) - discountAmount.value)
    await fetchMemberDiscount(afterCoupon)
  } catch (e) {
    discountAmount.value = 0
    selectedCouponId.value = null
    await fetchMemberDiscount(parseFloat(totalPrice.value))
  }
}

const handleAddressSelect = (addr) => {
  selectedAddress.value = addr
}

const confirmOrder = async () => {
  submitting.value = true
  try {
    await createOrder({
      totalAmount: totalPrice.value,
      remark: remark.value,
      userCouponId: selectedCouponId.value || undefined,
      addressId: selectedAddress.value?.id || undefined
    })
    ElMessage.success('支付成功，订单已下达')
    await cartStore.fetchCart()
    router.push('/orders')
  } catch (err) {
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await cartStore.fetchCart()
})
</script>
