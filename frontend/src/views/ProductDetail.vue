<template>
  <div class="max-w-4xl mx-auto">
    <div class="glass-card overflow-hidden">
      <div class="flex flex-col md:flex-row">
        <div class="md:w-1/2 h-64 md:h-auto overflow-hidden">
          <img
            v-if="product"
            :src="product.image"
            class="w-full h-full object-cover"
          />
        </div>
        <div class="md:w-1/2 p-8 flex flex-col justify-between">
          <div>
            <h1 class="text-2xl font-bold text-gray-800 mb-2">{{ product?.name }}</h1>
            <p class="text-gray-500 mb-6">{{ product?.description }}</p>
            <div class="text-3xl font-bold text-primary mb-6">
              ¥{{ product?.price }}
            </div>
          </div>

          <div>
            <div class="mb-6">
              <p class="text-sm font-bold text-gray-700 mb-3">规格选项</p>
              <div class="space-y-4">
                <div>
                  <p class="text-xs text-gray-400 mb-2">温度</p>
                  <el-radio-group v-model="specs.temp" size="small">
                    <el-radio-button label="常规冰" />
                    <el-radio-button label="少冰" />
                    <el-radio-button label="去冰" />
                    <el-radio-button label="热饮" />
                  </el-radio-group>
                </div>
                <div>
                  <p class="text-xs text-gray-400 mb-2">糖度</p>
                  <el-radio-group v-model="specs.sugar" size="small">
                    <el-radio-button label="全糖" />
                    <el-radio-button label="七分糖" />
                    <el-radio-button label="五分糖" />
                    <el-radio-button label="三分糖" />
                    <el-radio-button label="不另外加糖" />
                  </el-radio-group>
                </div>
              </div>
            </div>

            <el-button type="primary" size="large" class="w-full" @click="handleAddToCart">
              <el-icon class="mr-1"><ShoppingCart /></el-icon>
              加入购物车
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="mt-6">
      <ReviewList :product-id="Number(productId)" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { getProducts } from '@/api'
import { useCartStore } from '@/store/cart'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import ReviewList from '@/components/ReviewList.vue'

const route = useRoute()
const cartStore = useCartStore()
const productId = route.params.id
const product = ref(null)

const specs = reactive({
  temp: '常规冰',
  sugar: '全糖'
})

const fetchProduct = async () => {
  try {
    const data = await getProducts({ page: 1, pageSize: 100 })
    const found = data.records.find(p => p.id === Number(productId))
    if (found) {
      product.value = found
    }
  } catch (e) {
    ElMessage.error('获取商品信息失败')
  }
}

const handleAddToCart = async () => {
  await cartStore.add(product.value, { ...specs })
  ElMessage.success('已加入购物车')
}

onMounted(fetchProduct)
</script>
