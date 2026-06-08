<template>
  <div class="max-w-7xl mx-auto">
    <h2 class="text-2xl font-bold text-gray-800 mb-6">管理仪表盘</h2>

    <div v-if="loading" class="flex justify-center py-20">
      <el-icon class="is-loading" :size="40" color="#409EFF"><Loading /></el-icon>
    </div>

    <template v-else-if="dashboard">
      <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5 mb-8">
        <div class="bg-white rounded-xl shadow-sm border p-5 flex items-center space-x-4">
          <div class="w-12 h-12 bg-blue-50 rounded-lg flex items-center justify-center">
            <el-icon :size="24" color="#409EFF"><Document /></el-icon>
          </div>
          <div>
            <p class="text-sm text-gray-500">今日订单</p>
            <p class="text-2xl font-bold text-gray-800">{{ dashboard.todayOrderCount }}</p>
          </div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border p-5 flex items-center space-x-4">
          <div class="w-12 h-12 bg-green-50 rounded-lg flex items-center justify-center">
            <el-icon :size="24" color="#67C23A"><Coin /></el-icon>
          </div>
          <div>
            <p class="text-sm text-gray-500">今日销售额</p>
            <p class="text-2xl font-bold text-gray-800">¥{{ formatAmount(dashboard.todaySalesAmount) }}</p>
          </div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border p-5 flex items-center space-x-4">
          <div class="w-12 h-12 bg-orange-50 rounded-lg flex items-center justify-center">
            <el-icon :size="24" color="#E6A23C"><Clock /></el-icon>
          </div>
          <div>
            <p class="text-sm text-gray-500">待处理订单</p>
            <p class="text-2xl font-bold text-gray-800">{{ dashboard.pendingOrderCount }}</p>
          </div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border p-5 flex items-center space-x-4">
          <div class="w-12 h-12 bg-purple-50 rounded-lg flex items-center justify-center">
            <el-icon :size="24" color="#9B59B6"><User /></el-icon>
          </div>
          <div>
            <p class="text-sm text-gray-500">注册用户</p>
            <p class="text-2xl font-bold text-gray-800">{{ dashboard.registeredUserCount }}</p>
          </div>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div class="lg:col-span-2 bg-white rounded-xl shadow-sm border p-5">
          <h3 class="text-lg font-semibold text-gray-800 mb-4">近7天销售趋势</h3>
          <div ref="trendChartRef" class="w-full" style="height: 360px;"></div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border p-5">
          <h3 class="text-lg font-semibold text-gray-800 mb-4">近7天热销TOP5</h3>
          <div v-if="dashboard.topProducts && dashboard.topProducts.length > 0">
            <div
              v-for="(product, index) in dashboard.topProducts"
              :key="product.productId"
              class="flex items-center py-3 border-b last:border-b-0"
            >
              <span
                class="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold mr-3 flex-shrink-0"
                :class="index < 3 ? 'bg-red-400' : 'bg-gray-300'"
              >{{ index + 1 }}</span>
              <div class="flex-1 min-w-0">
                <p class="text-sm font-medium truncate">{{ product.productName }}</p>
                <p class="text-xs text-gray-400">销量 {{ product.totalSales }} · 营收 ¥{{ formatAmount(product.totalRevenue) }}</p>
              </div>
            </div>
          </div>
          <div v-else class="text-center text-gray-400 py-10">暂无数据</div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { getDashboardData } from '@/api'
import * as echarts from 'echarts'

const dashboard = ref(null)
const loading = ref(true)
const trendChartRef = ref(null)
let chartInstance = null

const formatAmount = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toFixed(2)
}

const initChart = () => {
  if (!trendChartRef.value || !dashboard.value?.weeklyTrend) return

  chartInstance = echarts.init(trendChartRef.value)

  const trend = dashboard.value.weeklyTrend
  const dates = trend.map(d => d.date)
  const orderCounts = trend.map(d => d.orderCount)
  const salesAmounts = trend.map(d => Number(d.salesAmount))

  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['订单数', '销售额'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLabel: { color: '#666' }
    },
    yAxis: [
      {
        type: 'value',
        name: '订单数',
        position: 'left',
        axisLabel: { color: '#666' },
        splitLine: { lineStyle: { type: 'dashed' } }
      },
      {
        type: 'value',
        name: '销售额(元)',
        position: 'right',
        axisLabel: { color: '#666', formatter: '¥{value}' },
        splitLine: { show: false }
      }
    ],
    series: [
      {
        name: '订单数',
        type: 'bar',
        data: orderCounts,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#79bbff' }
          ]),
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '35%'
      },
      {
        name: '销售额',
        type: 'line',
        yAxisIndex: 1,
        data: salesAmounts,
        smooth: true,
        itemStyle: { color: '#67C23A' },
        lineStyle: { width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103,194,58,0.25)' },
            { offset: 1, color: 'rgba(103,194,58,0.02)' }
          ])
        }
      }
    ]
  })
}

const handleResize = () => {
  chartInstance?.resize()
}

onMounted(async () => {
  try {
    dashboard.value = await getDashboardData()
  } catch (e) {
    console.error('获取仪表盘数据失败', e)
  } finally {
    loading.value = false
  }

  await nextTick()
  initChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
  chartInstance = null
})
</script>
