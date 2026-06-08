import { defineStore } from 'pinia'
import { getUnreadCount, getRecentNotifications, markNotificationRead } from '@/api'

export const useNotificationStore = defineStore('notification', {
  state: () => ({
    unreadCount: 0,
    recentList: []
  }),
  actions: {
    async fetchUnreadCount() {
      try {
        const data = await getUnreadCount()
        this.unreadCount = data || 0
      } catch (e) {
        console.error('获取未读通知数量失败', e)
      }
    },
    async fetchRecentList() {
      try {
        const data = await getRecentNotifications(5)
        this.recentList = data || []
      } catch (e) {
        console.error('获取最近通知失败', e)
      }
    },
    async markReadAndRefresh(id) {
      try {
        await markNotificationRead(id)
        await this.fetchUnreadCount()
        await this.fetchRecentList()
      } catch (e) {
        console.error('标记已读失败', e)
      }
    },
    decrementUnread() {
      if (this.unreadCount > 0) {
        this.unreadCount--
      }
    },
    resetUnread() {
      this.unreadCount = 0
    }
  }
})
