<template>
  <div class="border rounded-xl p-4 bg-white">
    <div class="flex items-center gap-3 mb-4">
      <div class="w-12 h-12 rounded-lg overflow-hidden bg-gray-100 shrink-0">
        <img v-if="product.image" :src="product.image" class="w-full h-full object-cover" />
      </div>
      <div class="flex-grow min-w-0">
        <h4 class="font-bold text-gray-800 truncate">{{ product.productName || product.name }}</h4>
        <p v-if="product.specs" class="text-xs text-gray-400">{{ formatSpecs(product.specs) }}</p>
      </div>
    </div>

    <div class="mb-4">
      <div class="flex items-center gap-2 mb-1">
        <span class="text-sm text-gray-600 font-medium">评分</span>
        <el-rate
          v-model="form.rating"
          :colors="rateColors"
          show-text
          :texts="rateTexts"
          size="large"
        />
      </div>
    </div>

    <div class="mb-4">
      <div class="relative">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="3"
          :maxlength="maxLength"
          show-word-limit
          placeholder="分享您的饮用体验，帮助更多人做出选择..."
          resize="none"
        />
      </div>
      <div class="flex justify-between mt-1">
        <span v-if="form.content.length > 0 && form.content.length < minLength" class="text-xs text-amber-500">
          还需输入 {{ minLength - form.content.length }} 个字
        </span>
        <span v-else-if="form.content.length >= minLength" class="text-xs text-green-500">
          字数符合要求
        </span>
        <span v-else class="text-xs text-gray-400">
          至少 {{ minLength }} 个字
        </span>
      </div>
    </div>

    <div class="mb-2">
      <div class="flex items-center gap-2 mb-2">
        <span class="text-sm text-gray-600 font-medium">上传图片</span>
        <span class="text-xs text-gray-400">（最多{{ maxImages }}张）</span>
      </div>
      <div class="flex flex-wrap gap-2">
        <div
          v-for="(img, index) in form.images"
          :key="index"
          class="relative w-20 h-20 rounded-lg overflow-hidden border border-gray-200 group"
        >
          <img :src="img" class="w-full h-full object-cover" />
          <div
            class="absolute inset-0 bg-black bg-opacity-40 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
            @click="removeImage(index)"
          >
            <el-icon class="text-white text-xl"><Delete /></el-icon>
          </div>
        </div>
        <div
          v-if="form.images.length < maxImages"
          class="w-20 h-20 rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center cursor-pointer hover:border-primary hover:text-primary transition-colors"
          @click="triggerUpload"
        >
          <el-icon size="20"><Plus /></el-icon>
          <span class="text-xs mt-1">添加图片</span>
        </div>
      </div>
      <input
        ref="fileInput"
        type="file"
        accept="image/*"
        multiple
        class="hidden"
        @change="handleFileChange"
      />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { uploadFeedbackImage } from '@/api'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'

const props = defineProps({
  product: {
    type: Object,
    required: true
  },
  orderId: {
    type: Number,
    required: true
  }
})

const maxLength = 500
const minLength = 5
const maxImages = 6

const rateColors = ['#99A9BF', '#F7BA2A', '#FF9900']
const rateTexts = ['很差', '一般', '还行', '很好', '非常好']

const form = reactive({
  rating: 5,
  content: '',
  images: []
})

const fileInput = ref(null)

const formatSpecs = specsStr => {
  try {
    const s = JSON.parse(specsStr)
    return `${s.temp} / ${s.sugar}`
  } catch (e) {
    return specsStr
  }
}

const triggerUpload = () => {
  fileInput.value.click()
}

const handleFileChange = async (event) => {
  const files = Array.from(event.target.files)
  if (!files.length) return

  const remaining = maxImages - form.images.length
  const toUpload = files.slice(0, remaining)

  for (const file of toUpload) {
    if (file.size > 5 * 1024 * 1024) {
      ElMessage.warning(`${file.name} 超过5MB，已跳过`)
      continue
    }
    try {
      const formData = new FormData()
      formData.append('file', file)
      const url = await uploadFeedbackImage(formData)
      form.images.push(url)
    } catch (e) {
      ElMessage.error(`${file.name} 上传失败`)
    }
  }

  event.target.value = ''
}

const removeImage = (index) => {
  form.images.splice(index, 1)
}

const getFeedbackData = () => ({
  orderId: props.orderId,
  productId: props.product.productId || props.product.id,
  rating: form.rating,
  content: form.content,
  images: form.images.join(',')
})

const validate = () => {
  if (!form.rating || form.rating < 1) {
    ElMessage.warning('请选择评分')
    return false
  }
  if (form.content.trim().length < minLength) {
    ElMessage.warning(`评价内容至少需要${minLength}个字`)
    return false
  }
  return true
}

defineExpose({ getFeedbackData, validate })
</script>
