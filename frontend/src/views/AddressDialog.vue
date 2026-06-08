<template>
  <el-dialog v-model="visible" title="选择收货地址" width="600px" @open="handleOpen">
    <div class="mb-4">
      <el-button type="primary" @click="openForm(null)">新增地址</el-button>
    </div>

    <div v-if="addressStore.addresses.length === 0" class="text-center py-10 text-gray-400">
      暂无收货地址，请先新增
    </div>

    <div v-else class="space-y-3">
      <div
        v-for="addr in addressStore.sortedAddresses"
        :key="addr.id"
        class="border rounded-lg p-4 cursor-pointer transition-all hover:border-primary"
        :class="{ 'border-primary bg-primary/5': selectedId === addr.id }"
        @click="handleSelect(addr)"
      >
        <div class="flex items-start justify-between">
          <div class="flex-1">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-bold text-gray-800">{{ addr.contactName }}</span>
              <span class="text-gray-500">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault === 1" size="small" type="danger">默认</el-tag>
            </div>
            <div class="text-sm text-gray-600">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detailAddress }}</div>
          </div>
          <div class="flex items-center gap-2 ml-4 shrink-0">
            <el-button link type="primary" size="small" @click.stop="handleSetDefault(addr)" v-if="addr.isDefault !== 1">设为默认</el-button>
            <el-button link type="primary" size="small" @click.stop="openForm(addr)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="handleDelete(addr.id)">删除</el-button>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="!selectedId" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="formVisible" :title="formIsEdit ? '编辑地址' : '新增地址'" width="500px" append-to-body>
    <el-form :model="form" label-width="80px" :rules="formRules" ref="formRef">
      <el-form-item label="联系人" prop="contactName">
        <el-input v-model="form.contactName" placeholder="请输入联系人姓名" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
      <el-form-item label="省" prop="province">
        <el-input v-model="form.province" placeholder="请输入省份" />
      </el-form-item>
      <el-form-item label="市" prop="city">
        <el-input v-model="form.city" placeholder="请输入城市" />
      </el-form-item>
      <el-form-item label="区" prop="district">
        <el-input v-model="form.district" placeholder="请输入区/县" />
      </el-form-item>
      <el-form-item label="详细地址" prop="detailAddress">
        <el-input v-model="form.detailAddress" type="textarea" placeholder="请输入详细地址" />
      </el-form-item>
      <el-form-item label="默认地址">
        <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmitForm" :loading="formSubmitting">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useAddressStore } from '@/store/address'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  selectedAddressId: { type: Number, default: null }
})

const emit = defineEmits(['update:modelValue', 'select'])

const addressStore = useAddressStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const selectedId = ref(null)

const formVisible = ref(false)
const formIsEdit = ref(false)
const formRef = ref(null)
const formSubmitting = ref(false)

const form = ref({
  id: null,
  contactName: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: 0
})

const formRules = {
  contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

const handleOpen = async () => {
  selectedId.value = props.selectedAddressId
  await addressStore.fetchAddresses()
}

const handleSelect = (addr) => {
  selectedId.value = addr.id
}

const handleConfirm = () => {
  const addr = addressStore.addresses.find(a => a.id === selectedId.value)
  if (addr) {
    emit('select', addr)
    visible.value = false
  }
}

const handleSetDefault = async (addr) => {
  await addressStore.setDefault(addr.id)
  ElMessage.success('已设为默认地址')
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该地址吗？', '提示', { type: 'warning' })
    await addressStore.remove(id)
    if (selectedId.value === id) {
      selectedId.value = null
    }
    ElMessage.success('删除成功')
  } catch {}
}

const openForm = (addr) => {
  if (addr) {
    formIsEdit.value = true
    form.value = { ...addr }
  } else {
    formIsEdit.value = false
    form.value = {
      id: null,
      contactName: '',
      phone: '',
      province: '',
      city: '',
      district: '',
      detailAddress: '',
      isDefault: addressStore.addresses.length === 0 ? 1 : 0
    }
  }
  formVisible.value = true
}

const handleSubmitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate()
  formSubmitting.value = true
  try {
    if (formIsEdit.value) {
      await addressStore.update(form.value.id, form.value)
      ElMessage.success('修改成功')
    } else {
      await addressStore.add(form.value)
      ElMessage.success('添加成功')
    }
    formVisible.value = false
  } catch {} finally {
    formSubmitting.value = false
  }
}
</script>
