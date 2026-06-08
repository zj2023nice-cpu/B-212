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
            <div class="flex items-center gap-3 mb-2">
              <h1 class="text-2xl font-bold text-gray-800">{{ product?.name }}</h1>
              <el-button
                circle
                :class="isFavorited ? 'is-favorited' : ''"
                class="favorite-btn-detail"
                @click="toggleFavorite"
              >
                <el-icon :size="18">
                  <svg v-if="isFavorited" viewBox="0 0 24 24" fill="currentColor" width="1em" height="1em"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                  <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="1em" height="1em"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
                </el-icon>
              </el-button>
            </div>
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
import { getProducts, addFavorite, removeFavorite, checkFavorite } from '@/api'
import { useCartStore } from '@/store/cart'
import { ElMessage } from 'element-plus'
import { ShoppingCart } from '@element-plus/icons-vue'
import ReviewList from '@/components/ReviewList.vue'

const route = useRoute()
const cartStore = useCartStore()
const productId = route.params.id
const product = ref(null)
const isFavorited = ref(false)

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

const fetchFavoriteStatus = async () => {
  try {
    const result = await checkFavorite(Number(productId))
    isFavorited.value = result.favorited
  } catch (e) {
    // ignore
  }
}

const toggleFavorite = async () => {
  try {
    if (isFavorited.value) {
      await removeFavorite(Number(productId))
      isFavorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(Number(productId))
      isFavorited.value = true
      ElMessage.success('已收藏')
    }
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  await fetchProduct()
  await fetchFavoriteStatus()
})
</script>

<style scoped>
.favorite-btn-detail {
  border: none !important;
  color: #999 !important;
  transition: all 0.3s;
}
.favorite-btn-detail:hover {
  color: #f56c6c !important;
  transform: scale(1.15);
}
.favorite-btn-detail.is-favorited {
  background: rgba(245, 108, 108, 0.1) !important;
  color: #f56c6c !important;
}
</style>
