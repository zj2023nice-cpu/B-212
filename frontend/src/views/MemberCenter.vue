<template>
  <div class="max-w-4xl mx-auto">
    <h2 class="text-2xl font-bold mb-6 text-gray-800">会员中心</h2>

    <div class="glass-card p-6 mb-6 member-card" :class="'level-' + memberInfo.level">
      <div class="flex items-center justify-between">
        <div>
          <div class="flex items-center gap-3 mb-3">
            <span class="level-badge">{{ memberInfo.levelName }}</span>
            <span v-if="memberInfo.discountRate > 0" class="text-sm text-white/80">
              享受 {{ (memberInfo.discountRate * 100).toFixed(0) }}% 折扣
            </span>
          </div>
          <div class="text-white text-3xl font-bold mb-1">{{ memberInfo.currentPoints }}</div>
          <div class="text-white/70 text-sm">当前积分</div>
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
import { ref, computed, onMounted } from 'vue'
import { getMemberLevel, getPointsRecords } from '@/api'

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
</style>
