import { defineStore } from 'pinia'
import { getCart, addToCart, updateCartItem, removeCartItem, clearCart } from '@/api'

export const useCartStore = defineStore('cart', {
  state: () => ({
    groups: []
  }),
  getters: {
    allItems: (state) => state.groups.flatMap(g => g.specs || []),
    totalCount: (state) => state.groups.reduce((sum, g) => sum + (g.specs || []).reduce((s, item) => s + item.quantity, 0), 0),
    totalPrice: (state) => state.groups.reduce((sum, g) => sum + (g.specs || []).reduce((s, item) => s + (Number(item.unitPrice) || 0) * item.quantity, 0), 0)
  },
  actions: {
    async fetchCart() {
      const data = await getCart()
      this.groups = data || []
    },
    async add(product, specs, unitPrice) {
      await addToCart({ productId: product.id, quantity: 1, specs: JSON.stringify(specs), unitPrice })
      await this.fetchCart()
    }
  }
})
