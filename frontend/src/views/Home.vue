<template>
  <div class="flex flex-col md:flex-row gap-6">
    <!-- Categories Sidebar -->
    <aside class="md:w-48 shrink-0">
      <div class="glass-card p-2 sticky top-24">
        <div
          v-for="cat in categories"
          :key="cat.id"
          @click="handleCategoryChange(cat.id)"
          class="px-4 py-3 rounded-lg cursor-pointer transition-all mb-1"
          :class="
            activeCategory === cat.id
              ? 'bg-primary text-white shadow-md'
              : 'text-gray-600'
          "
        >
          <span class="font-medium">{{ cat.name }}</span>
        </div>
      </div>
    </aside>

    <!-- Products Grid -->
    <div class="flex-grow">
      <!-- Search and Sort Controls -->
      <div class="glass-card p-4 mb-6 flex flex-col sm:flex-row gap-4 items-center justify-between">
        <div class="flex items-center gap-2 w-full sm:w-auto">
          <el-input
            v-model="keyword"
            placeholder="搜索商品名称或描述..."
            clearable
            class="w-full sm:w-64"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
        </div>
        <div class="flex items-center gap-2">
          <span class="text-gray-600 text-sm">排序：</span>
          <el-select v-model="sortBy" placeholder="选择排序方式" @change="handleSortChange" style="width: 140px">
            <el-option label="默认排序" value="" />
            <el-option label="价格" value="price" />
            <el-option label="名称" value="name" />
            <el-option label="创建时间" value="createtime" />
            <el-option label="库存" value="stock" />
          </el-select>
          <el-radio-group v-model="sortOrder" size="small" @change="handleSortChange">
            <el-radio-button value="asc">升序</el-radio-button>
            <el-radio-button value="desc">降序</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          v-for="product in products"
          :key="product.id"
          class="glass-card overflow-hidden hover:shadow-xl transition-all group"
        >
          <div class="h-48 overflow-hidden relative">
            <img
              :src="product.image"
              class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
            />
            <div class="absolute bottom-2 right-2">
              <el-button
                type="primary"
                circle
                size="large"
                @click="openSpecDialog(product)"
              >
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </div>
          <div class="p-4">
            <h3 class="text-lg font-bold text-gray-800">{{ product.name }}</h3>
            <p class="text-gray-500 text-sm mt-1 line-clamp-2">
              {{ product.description }}
            </p>
            <div class="mt-4 flex items-center justify-between">
              <span class="text-xl font-bold text-primary"
                >¥{{ product.price }}</span
              >
            </div>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div v-if="total > 0" class="mt-8 flex justify-center">
        <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[6, 12, 24, 48]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handlePageSizeChange"
        @current-change="fetchProducts"
      />
      </div>
    </div>

    <!-- Spec Dialog -->
    <el-dialog
      v-model="specDialogVisible"
      :title="currentProduct?.name"
      width="400px"
      custom-class="spec-dialog"
    >
      <div v-if="currentProduct">
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
        <div class="border-t pt-4 flex items-center justify-between">
          <span class="text-xl font-bold text-primary"
            >¥{{ currentProduct.price }}</span
          >
          <el-button type="primary" size="large" @click="handleAddToCart"
            >加入购物车</el-button
          >
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getCategories, getProducts } from '@/api'
import { useCartStore } from '@/store/cart'
import { ElMessage } from 'element-plus'

const cartStore = useCartStore()
const categories = ref([])
const products = ref([])
const activeCategory = ref(null)
const specDialogVisible = ref(false)
const currentProduct = ref(null)

const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const keyword = ref('')
const sortBy = ref('')
const sortOrder = ref('desc')

const specs = reactive({
  temp: '常规冰',
  sugar: '全糖'
})

const openSpecDialog = product => {
  currentProduct.value = product
  specs.temp = '常规冰'
  specs.sugar = '全糖'
  specDialogVisible.value = true
}

const handleAddToCart = async () => {
  await cartStore.add(currentProduct.value, { ...specs })
  ElMessage.success('已加入购物车')
  specDialogVisible.value = false
}

const handleCategoryChange = async (categoryId) => {
  activeCategory.value = categoryId
  currentPage.value = 1
  await fetchProducts()
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await fetchProducts()
}

const handleSearch = async () => {
  currentPage.value = 1
  await fetchProducts()
}

const handleSortChange = async () => {
  currentPage.value = 1
  await fetchProducts()
}

const fetchProducts = async () => {
  const params = {
    categoryId: activeCategory.value,
    page: currentPage.value,
    pageSize: pageSize.value
  }
  
  if (keyword.value && keyword.value.trim()) {
    params.keyword = keyword.value.trim()
  }
  
  if (sortBy.value) {
    params.sortBy = sortBy.value
    params.sortOrder = sortOrder.value
  }
  
  const data = await getProducts(params)
  products.value = data.records
  total.value = data.total
}

onMounted(async () => {
  const cats = await getCategories()
  categories.value = cats
  if (cats.length > 0) activeCategory.value = cats[0].id

  await fetchProducts()
})
</script>

<style>
.spec-dialog {
  border-radius: 1.5rem !important;
}
.spec-dialog .el-dialog__header {
  padding-bottom: 0;
}
</style>
