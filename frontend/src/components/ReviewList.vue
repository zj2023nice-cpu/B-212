<template>
  <div>
    <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
      <h3 class="text-lg font-bold text-gray-800">用户评价</h3>
      <div class="flex items-center gap-3 flex-wrap">
        <el-select
          v-model="filterRating"
          placeholder="评分筛选"
          clearable
          size="small"
          style="width: 120px"
          @change="handleFilterChange"
        >
          <el-option label="全部评分" :value="null" />
          <el-option label="5星好评" :value="5" />
          <el-option label="4星" :value="4" />
          <el-option label="3星" :value="3" />
          <el-option label="2星" :value="2" />
          <el-option label="1星差评" :value="1" />
        </el-select>
        <el-checkbox v-model="filterHasImage" label="有图" size="small" @change="handleFilterChange" />
        <el-radio-group v-model="sortOrder" size="small" @change="handleFilterChange">
          <el-radio-button value="desc">最新优先</el-radio-button>
          <el-radio-button value="asc">最早优先</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-if="reviews.length > 0" class="space-y-4">
      <div
        v-for="review in reviews"
        :key="review.id"
        class="glass-card p-4"
      >
        <div class="flex items-start gap-3">
          <el-avatar :size="36" :src="review.avatarUrl">
            {{ review.nickname?.charAt(0) || '?' }}
          </el-avatar>
          <div class="flex-grow min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-gray-800">{{ review.nickname || '匿名用户' }}</span>
              <el-rate
                :model-value="review.rating"
                disabled
                show-score
                score-template="{value}分"
                size="small"
              />
            </div>
            <p class="text-gray-600 text-sm mb-2 whitespace-pre-wrap">{{ review.content }}</p>
            <div v-if="review.images && review.images.length > 0" class="flex flex-wrap gap-2 mb-2">
              <div
                v-for="(img, imgIdx) in review.images"
                :key="imgIdx"
                class="w-16 h-16 rounded-lg overflow-hidden border border-gray-200 cursor-pointer hover:opacity-80 transition-opacity"
                @click="previewImage(img, review.images)"
              >
                <img :src="img" class="w-full h-full object-cover" />
              </div>
            </div>
            <div v-if="review.adminReply" class="bg-blue-50 rounded-lg p-3 mb-2">
              <div class="flex items-center gap-1 mb-1">
                <el-icon class="text-blue-500" size="14"><ChatDotRound /></el-icon>
                <span class="text-xs font-medium text-blue-600">商家回复</span>
              </div>
              <p class="text-sm text-blue-800">{{ review.adminReply }}</p>
            </div>
            <div class="text-xs text-gray-400">
              {{ formatDate(review.createTime) }}
            </div>
            <div v-if="isAdmin && !review.adminReply" class="mt-2">
              <div class="flex items-center gap-2">
                <el-input
                  v-model="replyMap[review.id]"
                  size="small"
                  placeholder="回复该评价..."
                  class="flex-grow"
                  @keyup.enter="handleReply(review.id)"
                />
                <el-button
                  type="primary"
                  size="small"
                  :loading="replyLoadingMap[review.id]"
                  @click="handleReply(review.id)"
                >
                  回复
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="!loading" class="text-center py-12">
      <el-empty description="暂无评价" />
    </div>

    <div v-if="loading" class="text-center py-8">
      <el-icon class="is-loading" size="24"><Loading /></el-icon>
      <p class="text-gray-400 text-sm mt-2">加载中...</p>
    </div>

    <el-image-viewer
      v-if="showViewer"
      :url-list="viewerList"
      :initial-index="viewerIndex"
      @close="showViewer = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, computed } from 'vue'
import { getProductFeedbacks, replyFeedback } from '@/api'
import { Loading, ChatDotRound } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const props = defineProps({
  productId: {
    type: Number,
    required: true
  }
})

const userStore = useUserStore()
const isAdmin = computed(() => userStore.user?.role === 'ADMIN')

const reviews = ref([])
const loading = ref(false)
const filterRating = ref(null)
const filterHasImage = ref(false)
const sortOrder = ref('desc')
const replyMap = reactive({})
const replyLoadingMap = reactive({})

const showViewer = ref(false)
const viewerList = ref([])
const viewerIndex = ref(0)

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const previewImage = (img, images) => {
  viewerList.value = images
  viewerIndex.value = images.indexOf(img)
  showViewer.value = true
}

const fetchReviews = async () => {
  loading.value = true
  try {
    const params = { sortOrder: sortOrder.value }
    if (filterRating.value !== null) {
      params.rating = filterRating.value
    }
    if (filterHasImage.value) {
      params.hasImage = true
    }
    const data = await getProductFeedbacks(props.productId, params)
    reviews.value = data
  } catch (e) {
    reviews.value = []
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  fetchReviews()
}

const handleReply = async (reviewId) => {
  const reply = replyMap[reviewId]
  if (!reply || !reply.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replyLoadingMap[reviewId] = true
  try {
    await replyFeedback(reviewId, reply.trim())
    ElMessage.success('回复成功')
    delete replyMap[reviewId]
    const review = reviews.value.find(r => r.id === reviewId)
    if (review) {
      review.adminReply = reply.trim()
    }
  } catch (e) {
    ElMessage.error('回复失败：' + (e.response?.data?.message || e.message))
  } finally {
    delete replyLoadingMap[reviewId]
  }
}

watch(() => props.productId, () => {
  fetchReviews()
})

onMounted(fetchReviews)
</script>
