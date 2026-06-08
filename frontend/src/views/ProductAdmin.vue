<template>
  <div class="max-w-7xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h2 class="text-2xl font-bold text-gray-800">商品管理</h2>
      <el-button type="primary" @click="showCreateDialog">
        <el-icon><Plus /></el-icon>
        添加商品
      </el-button>
    </div>

    <div class="glass-card p-4 mb-6">
      <el-form inline>
        <el-form-item label="分类">
          <el-select v-model="filterCategoryId" placeholder="全部" clearable style="width: 140px" @change="handleFilter">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable style="width: 120px" @change="handleFilter">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="搜索">
          <el-input v-model="keyword" placeholder="商品名称" clearable style="width: 180px" @keyup.enter="handleFilter" @clear="handleFilter" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilter">搜索</el-button>
        </el-form-item>
      </el-form>
    </div>

    <el-table :data="products" stripe class="glass-card">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="图片" width="80">
        <template #default="{ row }">
          <el-image v-if="row.image" :src="row.image" style="width: 50px; height: 50px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="商品名称" min-width="120">
        <template #default="{ row }">
          <span :class="{ 'text-red-500 font-bold': row.lowStock }">{{ row.name }}</span>
          <el-icon v-if="row.lowStock" class="ml-1 text-red-500" :size="16"><WarningFilled /></el-icon>
        </template>
      </el-table-column>
      <el-table-column label="价格" width="90">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="库存" width="100">
        <template #default="{ row }">
          <span :class="{ 'text-red-500 font-bold': row.lowStock }">{{ row.stock }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预警阈值" width="90">
        <template #default="{ row }">{{ row.lowStockThreshold }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="showEditDialog(row)">编辑</el-button>
          <el-button
            v-if="row.status === 1"
            type="warning"
            size="small"
            link
            @click="handleToggleStatus(row.id, 0)"
          >下架</el-button>
          <el-button
            v-else
            type="success"
            size="small"
            link
            @click="handleToggleStatus(row.id, 1)"
          >上架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="total > pageSize" class="mt-6 flex justify-center">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50]"
        @size-change="fetchProducts"
        @current-change="fetchProducts"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '添加商品'" width="550px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="商品名称">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" style="width: 100%">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警阈值">
          <el-input-number v-model="form.lowStockThreshold" :min="0" style="width: 100%" />
          <div class="text-xs text-gray-400 mt-1">库存低于此值时触发库存预警，默认为 10</div>
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.image" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { adminListProducts, adminCreateProduct, adminUpdateProduct, adminUpdateProductStatus, adminGetCategories, getCategories } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { WarningFilled, Plus } from '@element-plus/icons-vue'

const products = ref([])
const categories = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterCategoryId = ref(null)
const filterStatus = ref(null)
const keyword = ref('')

const dialogVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)

const form = ref({
  name: '',
  categoryId: null,
  price: 0,
  stock: 0,
  lowStockThreshold: 10,
  image: '',
  description: '',
  status: 1
})

let editingId = null

const fetchProducts = async () => {
  const params = {
    page: currentPage.value,
    pageSize: pageSize.value
  }
  if (filterCategoryId.value !== null && filterCategoryId.value !== '') {
    params.categoryId = filterCategoryId.value
  }
  if (filterStatus.value !== null && filterStatus.value !== '') {
    params.status = filterStatus.value
  }
  if (keyword.value && keyword.value.trim()) {
    params.keyword = keyword.value.trim()
  }
  const data = await adminListProducts(params)
  products.value = data.records
  total.value = data.total
}

const fetchCategories = async () => {
  try {
    const data = await adminGetCategories()
    categories.value = data
  } catch {
    const data = await getCategories()
    categories.value = data
  }
}

const handleFilter = async () => {
  currentPage.value = 1
  await fetchProducts()
}

const showCreateDialog = () => {
  isEdit.value = false
  editingId = null
  form.value = {
    name: '',
    categoryId: null,
    price: 0,
    stock: 0,
    lowStockThreshold: 10,
    image: '',
    description: '',
    status: 1
  }
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  editingId = row.id
  form.value = {
    name: row.name,
    categoryId: row.categoryId,
    price: row.price,
    stock: row.stock,
    lowStockThreshold: row.lowStockThreshold || 10,
    image: row.image || '',
    description: row.description || '',
    status: row.status
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!form.value.name) {
    ElMessage.warning('请输入商品名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await adminUpdateProduct(editingId, form.value)
      ElMessage.success('更新成功')
    } else {
      await adminCreateProduct(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await fetchProducts()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

const handleToggleStatus = async (id, status) => {
  const action = status === 1 ? '上架' : '下架'
  try {
    await ElMessageBox.confirm(`确认${action}该商品？`, '提示', { type: 'warning' })
    await adminUpdateProductStatus(id, status)
    ElMessage.success(`${action}成功`)
    await fetchProducts()
  } catch (e) {}
}

onMounted(() => {
  fetchCategories()
  fetchProducts()
})
</script>
