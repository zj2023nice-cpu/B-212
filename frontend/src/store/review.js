import { defineStore } from 'pinia'
import { submitFeedback, getProductFeedbacks, getOrderFeedbacks } from '@/api'

export const useReviewStore = defineStore('review', {
  state: () => ({
    productReviews: [],
    orderReviews: [],
    loading: false
  }),
  actions: {
    async submitReviews(feedbacks) {
      this.loading = true
      try {
        await submitFeedback(feedbacks)
        return true
      } catch (e) {
        throw e
      } finally {
        this.loading = false
      }
    },
    async fetchProductReviews(productId, params = {}) {
      this.loading = true
      try {
        const data = await getProductFeedbacks(productId, params)
        this.productReviews = data
        return data
      } catch (e) {
        this.productReviews = []
        throw e
      } finally {
        this.loading = false
      }
    },
    async fetchOrderReviews(orderId) {
      this.loading = true
      try {
        const data = await getOrderFeedbacks(orderId)
        this.orderReviews = data
        return data
      } catch (e) {
        this.orderReviews = []
        throw e
      } finally {
        this.loading = false
      }
    }
  }
})
