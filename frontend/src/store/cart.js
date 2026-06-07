import { defineStore } from 'pinia'
import { getCart, addToCart, updateCartItem, removeCartItem, clearCart } from '@/api'

export const useCartStore = defineStore('cart', {
  state: () => ({
    items: []
  }),
  getters: {
    totalCount: (state) => state.items.reduce((sum, item) => sum + item.quantity, 0),
    totalPrice: (state) => state.items.reduce((sum, item) => sum + (item.quantity * 10), 0) // 这里需要关联商品价格，简化处理
  },
  actions: {
    async fetchCart() {
      const data = await getCart()
      this.items = data
    },
    async add(product, specs) {
      await addToCart({ productId: product.id, quantity: 1, specs: JSON.stringify(specs) })
      await this.fetchCart()
    }
  }
})
