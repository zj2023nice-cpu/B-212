import { defineStore } from 'pinia'
import { getAddresses, addAddress, updateAddress, deleteAddress, setDefaultAddress } from '@/api'

export const useAddressStore = defineStore('address', {
  state: () => ({
    addresses: []
  }),
  getters: {
    defaultAddress: (state) => state.addresses.find(a => a.isDefault === 1),
    sortedAddresses: (state) => [...state.addresses].sort((a, b) => {
      if (a.isDefault === 1 && b.isDefault !== 1) return -1
      if (a.isDefault !== 1 && b.isDefault === 1) return 1
      return 0
    })
  },
  actions: {
    async fetchAddresses() {
      this.addresses = await getAddresses()
    },
    async add(data) {
      await addAddress(data)
      await this.fetchAddresses()
    },
    async update(id, data) {
      await updateAddress(id, data)
      await this.fetchAddresses()
    },
    async remove(id) {
      await deleteAddress(id)
      await this.fetchAddresses()
    },
    async setDefault(id) {
      await setDefaultAddress(id)
      await this.fetchAddresses()
    }
  }
})
