<template>
  <div class="max-w-5xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">我的收藏</h2>
      <span class="text-gray-400 text-sm">共 {{ total }} 件商品</span>
    </div>

    <div v-if="favoriteList.length > 0" class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
      <div
        v-for="item in favoriteList"
        :key="item.favoriteId"
        class="glass-card overflow-hidden hover:shadow-xl transition-all group relative"
      >
        <div class="h-36 overflow-hidden relative cursor-pointer" @click="$router.push(`/product/${item.productId}`)">
          <img
            :src="item.image"
            class="w-full h-full object-cover group-hover:scale-110 transition-transform duration-500"
          />
          <div v-if="item.status !== 1" class="absolute inset-0 bg-black/40 flex items-center justify-center">
            <span class="text-white text-sm font-bold">已下架</span>
          </div>
        </div>
        <div class="p-3">
          <h3 class="text-sm font-bold text-gray-800 truncate cursor-pointer" @click="$router.push(`/product/${item.productId}`)">{{ item.name }}</h3>
          <div class="mt-2 flex items-center justify-between">
            <span class="text-base font-bold text-primary">¥{{ item.price }}</span>
            <el-button
              circle
              size="small"
              class="favorite-btn-active"
              @click="handleRemoveFavorite(item)"
            >
              <el-icon :size="14">
                <svg viewBox="0 0 24 24" fill="currentColor" width="1em" height="1em"><path d="M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z"/></svg>
              </el-icon>
            </el-button>
          </div>
          <div class="text-xs text-gray-400 mt-1">{{ formatDate(item.favoriteTime) }}</div>
        </div>
      </div>
    </div>

    <div v-else class="text-center py-20">
      <el-empty description="暂无收藏商品">
        <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
      </el-empty>
    </div>

    <div v-if="total > pageSize" class="mt-8 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[12, 24, 48]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handlePageSizeChange"
        @current-change="fetchFavorites"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavoriteList, removeFavorite } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const favoriteList = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const fetchFavorites = async () => {
  const data = await getFavoriteList({ page: currentPage.value, pageSize: pageSize.value })
  favoriteList.value = data.records
  total.value = data.total
}

const handlePageSizeChange = async () => {
  currentPage.value = 1
  await fetchFavorites()
}

const handleRemoveFavorite = async (item) => {
  try {
    await ElMessageBox.confirm('确定取消收藏该商品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await removeFavorite(item.productId)
    ElMessage.success('已取消收藏')
    await fetchFavorites()
  } catch (e) {
    // cancelled or error
  }
}

onMounted(fetchFavorites)
</script>

<style scoped>
.favorite-btn-active {
  border: none !important;
  background: rgba(245, 108, 108, 0.1) !important;
  color: #f56c6c !important;
  transition: all 0.3s;
}
.favorite-btn-active:hover {
  color: #f56c6c !important;
  transform: scale(1.15);
}
</style>
