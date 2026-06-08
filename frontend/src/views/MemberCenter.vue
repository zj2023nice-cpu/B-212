<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold mb-6 text-gray-800">会员中心</h2>

    <div class="glass-card p-6 mb-6 member-card" :class="'level-' + memberInfo.level">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-4">
          <div class="avatar-wrapper" @click="triggerUpload">
            <el-avatar :size="72" :src="avatarFullUrl">
              <span class="text-2xl">{{ userStore.user?.nickname?.charAt(0) || 'U' }}</span>
            </el-avatar>
            <div class="avatar-overlay">
              <el-icon color="white" :size="20"><Camera /></el-icon>
            </div>
          </div>
          <div>
            <div class="flex items-center gap-3 mb-1">
              <span class="level-badge">{{ memberInfo.levelName }}</span>
              <span v-if="memberInfo.discountRate > 0" class="text-sm text-white/80">
                享受 {{ (memberInfo.discountRate * 100).toFixed(0) }}% 折扣
              </span>
            </div>
            <div class="text-white text-xl font-bold">{{ userStore.user?.nickname || '用户' }}</div>
          </div>
        </div>
        <div class="text-right">
          <div class="text-white/70 text-sm mb-1">累计积分</div>
          <div class="text-white text-xl font-bold">{{ memberInfo.totalPoints }}</div>
        </div>
      </div>

      <div v-if="memberInfo.nextLevel !== null" class="mt-6">
        <div class="flex justify-between text-sm text-white/80 mb-2">
          <span>距离 {{ memberInfo.nextLevelName }}</span>
          <span>还需 {{ memberInfo.pointsToNextLevel }} 积分</span>
        </div>
        <el-progress
          :percentage="progressPercent"
          :stroke-width="8"
          color="rgba(255,255,255,0.9)"
          :show-text="false"
        />
        <div class="flex justify-between text-xs text-white/60 mt-1">
          <span>{{ memberInfo.levelName }}</span>
          <span>{{ memberInfo.nextLevelName }} ({{ memberInfo.nextLevelPoints }})</span>
        </div>
      </div>
      <div v-else class="mt-6 text-center text-white/80 text-sm">
        已达到最高等级
      </div>
    </div>

    <el-upload
      ref="uploadRef"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :http-request="handleUploadRequest"
      accept="image/jpeg,image/png"
      class="hidden-upload"
    >
    </el-upload>

    <el-dialog v-model="cropDialogVisible" title="裁剪头像" width="420px" :close-on-click-modal="false" destroy-on-close>
      <div class="crop-container" ref="cropContainerRef">
        <canvas ref="cropCanvasRef" class="crop-canvas" @mousedown="onCropStart" @mousemove="onCropMove" @mouseup="onCropEnd" @mouseleave="onCropEnd"></canvas>
        <div v-if="cropImageSrc" class="crop-preview-box">
          <div class="crop-preview-label">预览</div>
          <canvas ref="previewCanvasRef" class="crop-preview-canvas"></canvas>
        </div>
      </div>
      <template #footer>
        <el-button @click="cropDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="confirmCrop">确认上传</el-button>
      </template>
    </el-dialog>

    <div class="grid grid-cols-4 gap-4 mb-6">
      <div
        v-for="level in levelList"
        :key="level.level"
        class="glass-card p-4 text-center"
        :class="{ 'ring-2 ring-primary': memberInfo.level === level.level }"
      >
        <div class="text-2xl mb-1">{{ level.icon }}</div>
        <div class="font-bold text-gray-800 text-sm">{{ level.name }}</div>
        <div class="text-xs text-gray-500 mt-1">{{ level.threshold }} 积分</div>
        <div class="text-xs text-primary mt-1">{{ level.discount }} 折扣</div>
      </div>
    </div>

    <div class="glass-card p-6">
      <h3 class="text-lg font-bold text-gray-800 mb-4">积分明细</h3>

      <div v-if="pointsRecords.length > 0">
        <div
          v-for="record in pointsRecords"
          :key="record.id"
          class="flex items-center justify-between py-3 border-b border-gray-100 last:border-0"
        >
          <div>
            <div class="font-medium text-gray-800">{{ getTypeName(record.type) }}</div>
            <div class="text-xs text-gray-400 mt-1">{{ formatDate(record.createTime) }}</div>
          </div>
          <div class="text-right">
            <div :class="record.points > 0 ? 'text-green-500' : 'text-red-500'" class="font-bold">
              {{ record.points > 0 ? '+' : '' }}{{ record.points }}
            </div>
            <div class="text-xs text-gray-400 mt-1">余额: {{ record.balance }}</div>
          </div>
        </div>
      </div>

      <div v-else class="text-center py-10">
        <el-empty description="暂无积分记录" />
      </div>

      <div v-if="total > pageSize" class="mt-6 flex justify-center">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchPointsRecords"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { getMemberLevel, getPointsRecords, uploadAvatar } from '@/api'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const memberInfo = ref({
  level: 0,
  levelName: '普通会员',
  totalPoints: 0,
  currentPoints: 0,
  discountRate: 0,
  nextLevel: null,
  nextLevelName: '',
  nextLevelPoints: 0,
  pointsToNextLevel: 0
})

const pointsRecords = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const uploading = ref(false)
const uploadRef = ref(null)
const cropDialogVisible = ref(false)
const cropCanvasRef = ref(null)
const previewCanvasRef = ref(null)
const cropContainerRef = ref(null)
const cropImageSrc = ref(null)

let cropImage = null
let cropState = {
  imgX: 0,
  imgY: 0,
  imgW: 0,
  imgH: 0,
  boxX: 0,
  boxY: 0,
  boxSize: 200,
  dragging: false,
  dragStartX: 0,
  dragStartY: 0,
  boxStartX: 0,
  boxStartY: 0
}

const avatarFullUrl = computed(() => {
  const avatar = userStore.user?.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http')) return avatar
  return avatar
})

const levelList = [
  { level: 0, name: '普通会员', icon: '🥤', threshold: 0, discount: '0%' },
  { level: 1, name: '银卡会员', icon: '🥈', threshold: 500, discount: '5%' },
  { level: 2, name: '金卡会员', icon: '🥇', threshold: 1500, discount: '10%' },
  { level: 3, name: '黑卡会员', icon: '💎', threshold: 5000, discount: '15%' }
]

const progressPercent = computed(() => {
  if (memberInfo.value.nextLevel === null) return 100
  const currentThreshold = [0, 500, 1500, 5000][memberInfo.value.level] || 0
  const nextThreshold = memberInfo.value.nextLevelPoints
  if (nextThreshold <= currentThreshold) return 100
  const progress = ((memberInfo.value.totalPoints - currentThreshold) / (nextThreshold - currentThreshold)) * 100
  return Math.min(Math.max(progress, 0), 100)
})

const getTypeName = (type) => {
  const map = { 1: '消费获得积分', 2: '管理员调整', 3: '订单取消扣减' }
  return map[type] || '其他'
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const fetchMemberInfo = async () => {
  const data = await getMemberLevel()
  memberInfo.value = data
}

const fetchPointsRecords = async () => {
  const data = await getPointsRecords({ page: currentPage.value, pageSize: pageSize.value })
  pointsRecords.value = data.records
  total.value = data.total
}

const triggerUpload = () => {
  const input = uploadRef.value?.$el?.querySelector('input[type="file"]')
  if (input) input.click()
}

const beforeUpload = (file) => {
  const isJpgOrPng = ['image/jpeg', 'image/png'].includes(file.type)
  if (!isJpgOrPng) {
    ElMessage.error('只支持 JPG/PNG 格式的图片')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

const handleUploadRequest = async ({ file }) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    cropImageSrc.value = e.target.result
    cropDialogVisible.value = true
    nextTick(() => initCropCanvas(file))
  }
  reader.readAsDataURL(file)
}

const initCropCanvas = (file) => {
  const canvas = cropCanvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  cropImage = new Image()
  cropImage.onload = () => {
    const maxW = 380
    const maxH = 380
    let w = cropImage.width
    let h = cropImage.height
    const ratio = Math.min(maxW / w, maxH / h, 1)
    w = Math.round(w * ratio)
    h = Math.round(h * ratio)

    canvas.width = w
    canvas.height = h

    const boxSize = Math.min(w, h, 200)
    cropState = {
      imgX: 0,
      imgY: 0,
      imgW: w,
      imgH: h,
      boxX: (w - boxSize) / 2,
      boxY: (h - boxSize) / 2,
      boxSize: boxSize,
      dragging: false,
      dragStartX: 0,
      dragStartY: 0,
      boxStartX: 0,
      boxStartY: 0
    }

    drawCropCanvas()
    drawPreview()
  }
  cropImage.src = cropImageSrc.value
}

const drawCropCanvas = () => {
  const canvas = cropCanvasRef.value
  if (!canvas || !cropImage) return
  const ctx = canvas.getContext('2d')
  const { imgX, imgY, imgW, imgH, boxX, boxY, boxSize } = cropState

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.drawImage(cropImage, imgX, imgY, imgW, imgH)

  ctx.fillStyle = 'rgba(0,0,0,0.5)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)

  ctx.save()
  ctx.beginPath()
  ctx.rect(boxX, boxY, boxSize, boxSize)
  ctx.clip()
  ctx.clearRect(boxX, boxY, boxSize, boxSize)
  ctx.drawImage(cropImage, imgX, imgY, imgW, imgH)
  ctx.restore()

  ctx.strokeStyle = '#ffffff'
  ctx.lineWidth = 2
  ctx.strokeRect(boxX, boxY, boxSize, boxSize)

  const third = boxSize / 3
  ctx.strokeStyle = 'rgba(255,255,255,0.3)'
  ctx.lineWidth = 1
  for (let i = 1; i < 3; i++) {
    ctx.beginPath()
    ctx.moveTo(boxX + third * i, boxY)
    ctx.lineTo(boxX + third * i, boxY + boxSize)
    ctx.stroke()
    ctx.beginPath()
    ctx.moveTo(boxX, boxY + third * i)
    ctx.lineTo(boxX + boxSize, boxY + third * i)
    ctx.stroke()
  }
}

const drawPreview = () => {
  const previewCanvas = previewCanvasRef.value
  const cropCanvas = cropCanvasRef.value
  if (!previewCanvas || !cropCanvas || !cropImage) return
  const pCtx = previewCanvas.getContext('2d')
  const previewSize = 120
  previewCanvas.width = previewSize
  previewCanvas.height = previewSize

  const { boxX, boxY, boxSize } = cropState
  const scaleX = cropImage.width / cropCanvas.width
  const scaleY = cropImage.height / cropCanvas.height

  pCtx.clearRect(0, 0, previewSize, previewSize)
  pCtx.drawImage(
    cropImage,
    boxX * scaleX, boxY * scaleY,
    boxSize * scaleX, boxSize * scaleY,
    0, 0, previewSize, previewSize
  )
}

const onCropStart = (e) => {
  const rect = cropCanvasRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const { boxX, boxY, boxSize } = cropState

  if (x >= boxX && x <= boxX + boxSize && y >= boxY && y <= boxY + boxSize) {
    cropState.dragging = true
    cropState.dragStartX = x
    cropState.dragStartY = y
    cropState.boxStartX = boxX
    cropState.boxStartY = boxY
  }
}

const onCropMove = (e) => {
  if (!cropState.dragging) return
  const rect = cropCanvasRef.value.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const dx = x - cropState.dragStartX
  const dy = y - cropState.dragStartY

  cropState.boxX = Math.max(0, Math.min(cropState.imgW - cropState.boxSize, cropState.boxStartX + dx))
  cropState.boxY = Math.max(0, Math.min(cropState.imgH - cropState.boxSize, cropState.boxStartY + dy))

  drawCropCanvas()
  drawPreview()
}

const onCropEnd = () => {
  cropState.dragging = false
}

const confirmCrop = async () => {
  if (!cropCanvasRef.value || !cropImage) return

  const { boxX, boxY, boxSize } = cropState
  const scaleX = cropImage.width / cropCanvasRef.value.width
  const scaleY = cropImage.height / cropCanvasRef.value.height

  const outputSize = 256
  const tempCanvas = document.createElement('canvas')
  tempCanvas.width = outputSize
  tempCanvas.height = outputSize
  const tempCtx = tempCanvas.getContext('2d')

  tempCtx.drawImage(
    cropImage,
    boxX * scaleX, boxY * scaleY,
    boxSize * scaleX, boxSize * scaleY,
    0, 0, outputSize, outputSize
  )

  uploading.value = true
  try {
    tempCanvas.toBlob(async (blob) => {
      if (!blob) {
        ElMessage.error('裁剪失败，请重试')
        uploading.value = false
        return
      }
      const formData = new FormData()
      formData.append('file', blob, 'avatar.png')

      try {
        const data = await uploadAvatar(formData)
        userStore.updateAvatar(data.avatarUrl)
        cropDialogVisible.value = false
        ElMessage.success('头像更新成功')
      } catch (err) {
        console.error('头像上传失败', err)
      } finally {
        uploading.value = false
      }
    }, 'image/png', 0.9)
  } catch (err) {
    console.error('裁剪失败', err)
    uploading.value = false
  }
}

onMounted(async () => {
  await fetchMemberInfo()
  await fetchPointsRecords()
})
</script>

<style scoped>
.member-card {
  background: linear-gradient(135deg, #6b4f4f, #8b6f6f);
  color: white;
}
.member-card.level-0 {
  background: linear-gradient(135deg, #8b7e7e, #a39595);
}
.member-card.level-1 {
  background: linear-gradient(135deg, #7a8b9e, #9eafc2);
}
.member-card.level-2 {
  background: linear-gradient(135deg, #c9a84c, #e0c76a);
}
.member-card.level-3 {
  background: linear-gradient(135deg, #3a3a3a, #5a5a5a);
}
.level-badge {
  background: rgba(255, 255, 255, 0.2);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
}
.avatar-wrapper {
  position: relative;
  cursor: pointer;
  border-radius: 50%;
  overflow: hidden;
}
.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}
.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 50%;
}
.hidden-upload {
  display: none;
}
.crop-container {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  justify-content: center;
}
.crop-canvas {
  cursor: move;
  border: 1px solid #ddd;
  border-radius: 4px;
}
.crop-preview-box {
  text-align: center;
}
.crop-preview-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.crop-preview-canvas {
  border-radius: 50%;
  border: 2px solid #e4e7ed;
}
</style>
