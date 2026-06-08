<template>
  <div class="receipt-page">
    <div class="receipt-actions no-print">
      <el-button @click="goBack">返回订单</el-button>
      <el-button type="primary" @click="handlePrint">
        <el-icon class="mr-1"><Printer /></el-icon>
        打印小票
      </el-button>
    </div>

    <div class="receipt-wrapper">
      <div class="receipt" id="receipt-content">
        <div class="receipt-header">
          <div class="store-name">{{ receipt.storeName }}</div>
          <div class="store-phone">TEL: {{ receipt.storePhone }}</div>
        </div>

        <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

        <div class="receipt-info">
          <div class="receipt-row">
            <span>单号:</span>
            <span class="receipt-value">{{ receipt.orderSn }}</span>
          </div>
          <div class="receipt-row">
            <span>时间:</span>
            <span class="receipt-value">{{ receipt.orderTime }}</span>
          </div>
        </div>

        <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

        <div class="receipt-section-title">商品明细</div>

        <div class="receipt-items">
          <div class="receipt-item" v-for="(item, index) in receipt.items" :key="index">
            <div class="item-name">{{ item.productName }}</div>
            <div class="item-specs" v-if="item.specs">{{ item.specs }}</div>
            <div class="item-detail">
              <span class="item-qty">x{{ item.quantity }}</span>
              <span class="item-price">{{ item.unitPrice }}</span>
              <span class="item-subtotal">{{ item.subtotal }}</span>
            </div>
          </div>
        </div>

        <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

        <div class="receipt-totals">
          <div class="receipt-row">
            <span>商品总额:</span>
            <span class="receipt-value">¥{{ receipt.totalAmount }}</span>
          </div>
          <div class="receipt-row" v-if="receipt.discountAmount > 0">
            <span>优惠减免:</span>
            <span class="receipt-value discount">-¥{{ receipt.discountAmount }}</span>
          </div>
          <div class="receipt-divider">--------------------------------</div>
          <div class="receipt-row total">
            <span>实付总计:</span>
            <span class="receipt-value">¥{{ receipt.payAmount }}</span>
          </div>
        </div>

        <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

        <div class="receipt-delivery">
          <div class="receipt-row">
            <span>配送方式:</span>
            <span class="receipt-value">{{ receipt.deliveryType }}</span>
          </div>
          <template v-if="receipt.deliveryType === '门店自提'">
            <div class="receipt-row" v-if="receipt.pickupStore">
              <span>自提门店:</span>
              <span class="receipt-value">{{ receipt.pickupStore }}</span>
            </div>
            <div class="receipt-row" v-if="receipt.pickupTime">
              <span>自提时间:</span>
              <span class="receipt-value">{{ receipt.pickupTime }}</span>
            </div>
          </template>
          <template v-else>
            <div class="receipt-row" v-if="receipt.contactName">
              <span>联系人:</span>
              <span class="receipt-value">{{ receipt.contactName }} {{ receipt.contactPhone }}</span>
            </div>
            <div class="receipt-row" v-if="receipt.address">
              <span>地址:</span>
              <span class="receipt-value address-value">{{ receipt.address }}</span>
            </div>
          </template>
        </div>

        <div class="receipt-remark" v-if="receipt.remark">
          <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>
          <div class="receipt-row">
            <span>备注:</span>
            <span class="receipt-value">{{ receipt.remark }}</span>
          </div>
        </div>

        <div class="receipt-divider">- - - - - - - - - - - - - - - - - - - -</div>

        <div class="receipt-footer">
          <div>感谢您的惠顾，欢迎再次光临!</div>
          <div class="receipt-qr-hint">--- 小票结束 ---</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderReceipt } from '@/api'
import { Printer } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const receipt = ref({
  storeName: '',
  storePhone: '',
  orderSn: '',
  orderTime: '',
  deliveryType: '',
  pickupStore: '',
  pickupTime: '',
  contactName: '',
  contactPhone: '',
  address: '',
  remark: '',
  items: [],
  totalAmount: '0.00',
  discountAmount: '0.00',
  payAmount: '0.00'
})

const fetchData = async () => {
  const id = route.params.id
  const data = await getOrderReceipt(id)
  receipt.value = data
}

const goBack = () => {
  router.push(`/order/${route.params.id}`)
}

const handlePrint = () => {
  window.print()
}

onMounted(fetchData)
</script>

<style scoped>
.receipt-page {
  min-height: 100vh;
  background: #f0f0f0;
  padding: 20px;
}

.receipt-actions {
  max-width: 600px;
  margin: 0 auto 20px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.receipt-wrapper {
  display: flex;
  justify-content: center;
}

.receipt {
  width: 232px;
  background: #fff;
  padding: 12px 10px;
  font-family: 'Courier New', 'Lucida Console', monospace;
  font-size: 12px;
  line-height: 1.5;
  color: #000;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
}

.receipt-header {
  text-align: center;
  margin-bottom: 4px;
}

.store-name {
  font-size: 16px;
  font-weight: bold;
  letter-spacing: 2px;
}

.store-phone {
  font-size: 11px;
  margin-top: 2px;
}

.receipt-divider {
  border: none;
  font-size: 10px;
  letter-spacing: -1px;
  color: #999;
  text-align: center;
  line-height: 1.2;
  margin: 6px 0;
  overflow: hidden;
  white-space: nowrap;
}

.receipt-info {
  margin: 4px 0;
}

.receipt-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 4px;
}

.receipt-value {
  text-align: right;
  word-break: break-all;
}

.address-value {
  max-width: 140px;
  text-align: right;
}

.receipt-section-title {
  font-weight: bold;
  text-align: center;
  margin: 4px 0;
}

.receipt-items {
  margin: 4px 0;
}

.receipt-item {
  margin-bottom: 6px;
}

.item-name {
  font-weight: bold;
}

.item-specs {
  font-size: 10px;
  color: #666;
  margin-top: 1px;
}

.item-detail {
  display: flex;
  justify-content: space-between;
  margin-top: 2px;
}

.item-qty {
  flex: 1;
}

.item-price {
  width: 60px;
  text-align: right;
}

.item-subtotal {
  width: 60px;
  text-align: right;
  font-weight: bold;
}

.receipt-totals {
  margin: 4px 0;
}

.receipt-totals .receipt-row {
  margin: 2px 0;
}

.receipt-totals .total {
  font-weight: bold;
  font-size: 14px;
  margin-top: 4px;
}

.discount {
  color: #e53e3e;
}

.receipt-delivery {
  margin: 4px 0;
}

.receipt-remark {
  margin: 4px 0;
}

.receipt-footer {
  text-align: center;
  margin-top: 8px;
  font-size: 11px;
}

.receipt-qr-hint {
  margin-top: 6px;
  font-size: 10px;
  color: #999;
}

@media print {
  body {
    margin: 0;
    padding: 0;
    background: #fff;
  }

  .no-print {
    display: none !important;
  }

  .receipt-page {
    background: #fff;
    padding: 0;
    min-height: auto;
  }

  .receipt-wrapper {
    justify-content: flex-start;
  }

  .receipt {
    box-shadow: none;
    margin: 0;
    padding: 8px;
    width: 58mm;
  }
}
</style>
