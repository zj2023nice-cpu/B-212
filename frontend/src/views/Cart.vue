<template>
  <div class="max-w-3xl mx-auto">
    <h2 class="text-2xl font-bold mb-6 text-gray-800">购物车</h2>
    
    <div v-if="cartStore.items.length > 0" class="space-y-4">
      <div v-for="item in cartItemsWithProduct" :key="item.id" class="glass-card p-4 flex items-center gap-4">
        <img :src="item.product?.image" class="w-20 h-20 rounded-xl object-cover" />
        <div class="flex-grow">
          <h3 class="font-bold text-gray-800">{{ item.product?.name }}</h3>
          <p class="text-gray-400 text-xs mt-1">{{ formatSpecs(item.specs) }}</p>
          <div class="mt-2 text-primary font-bold">¥{{ item.product?.price }}</div>
        </div>
        <div class="flex items-center gap-3">
          <el-input-number v-model="item.quantity" :min="1" size="small" @change="(val) => handleUpdateQuantity(item.id, val)" />
          <el-button type="danger" link @click="handleRemove(item.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </div>

      <!-- Settlement -->
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

    <!-- Checkout Dialog -->
    <el-dialog v-model="checkoutVisible" title="确认订单" width="450px">
      <el-form label-position="top">
        <el-form-item label="备注信息">
          <el-input v-model="remark" type="textarea" placeholder="口味要求等..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex items-center justify-between">
          <span class="text-lg font-bold text-primary">应付：¥{{ totalPrice }}</span>
          <el-button type="primary" @click="confirmOrder" :loading="submitting">确认付款</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useCartStore } from '@/store/cart'
import { getProducts, updateCartItem, removeCartItem, createOrder } from '@/api'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const cartStore = useCartStore()
const router = useRouter()
const allProducts = ref([])
const checkoutVisible = ref(false)
const remark = ref('')
const submitting = ref(false)

const cartItemsWithProduct = computed(() => {
  return cartStore.items.map(item => ({
    ...item,
    product: allProducts.value.find(p => p.id === item.productId)
  }))
})

const totalPrice = computed(() => {
  return cartItemsWithProduct.value.reduce((sum, item) => sum + (item.product?.price || 0) * item.quantity, 0).toFixed(2)
})

const formatSpecs = (specsStr) => {
  try {
    const s = JSON.parse(specsStr)
    return `${s.temp} / ${s.sugar}`
  } catch (e) { return specsStr }
}

const handleUpdateQuantity = async (id, val) => {
  await updateCartItem(id, val)
  await cartStore.fetchCart()
}

const handleRemove = async (id) => {
  await removeCartItem(id)
  await cartStore.fetchCart()
}

const handleCheckout = () => {
  checkoutVisible.value = true
}

const confirmOrder = async () => {
  submitting.value = true
  try {
    await createOrder({
      totalAmount: totalPrice.value,
      remark: remark.value
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
  const prods = await getProducts()
  allProducts.value = prods
})
</script>
