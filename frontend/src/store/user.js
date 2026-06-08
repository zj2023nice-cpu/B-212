import { defineStore } from 'pinia'
import { getMe } from '@/api'

export const useUserStore = defineStore('user', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null,
    token: localStorage.getItem('token') || ''
  }),
  actions: {
    setToken(token) {
      this.token = token
      localStorage.setItem('token', token)
    },
    async fetchUser() {
      const data = await getMe()
      this.user = data
      localStorage.setItem('user', JSON.stringify(data))
    },
    updateAvatar(avatarUrl) {
      if (this.user) {
        this.user = { ...this.user, avatarUrl: avatarUrl }
        localStorage.setItem('user', JSON.stringify(this.user))
      }
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
