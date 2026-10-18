<template>
  <div class="page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline label-width="72px">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="商品名称 / 编码" clearable style="width: 190px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="query.categoryId" placeholder="全部分类" clearable style="width: 170px">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌">
          <el-select v-model="query.brand" placeholder="全部品牌" clearable filterable style="width: 150px">
            <el-option v-for="b in brands" :key="b" :label="b" :value="b" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 130px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="价格区间">
          <el-input-number v-model="query.minPrice" :min="0" :controls="false" placeholder="最低价" style="width: 100px" />
          <span class="range-sep">-</span>
          <el-input-number v-model="query.maxPrice" :min="0" :controls="false" placeholder="最高价" style="width: 100px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="RefreshLeft" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格区 -->
    <el-card shadow="never">
      <div class="table-toolbar">
        <div class="toolbar-title">商品列表（共 {{ total }} 条）</div>
        <div>
          <el-button v-permission="'product:create'" type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
          <el-button
            v-permission="'product:delete'"
            type="danger"
            plain
            :icon="Delete"
            :disabled="selection.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        border
        stripe
        row-key="id"
        @selection-change="(rows) => (selection = rows)"
      >
        <el-table-column v-if="hasPermission('product:delete')" type="selection" width="46" />
        <el-table-column prop="name" label="商品名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="code" label="商品编码" min-width="130" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="所属分类" min-width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.categoryName || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="brand" label="品牌" min-width="100" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="110" align="right">
          <template #default="{ row }">
            <span class="text-price">¥{{ formatMoney(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.stock > 0 ? 'success' : 'danger'" size="small" effect="plain">{{ row.stock ?? 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sales" label="销量" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'product:update'" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'product:delete'" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无商品数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[5, 10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="700px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入商品名称" maxlength="64" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品编码" prop="code">
              <el-input v-model="form.code" placeholder="仅字母 / 数字 / - / _" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="form.brand" placeholder="请输入品牌" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="价格" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :precision="0" :step="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="件 / 台 / 箱" maxlength="16" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销量" prop="sales">
              <el-input-number v-model="form.sales" :min="0" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">上架</el-radio>
                <el-radio :value="0">下架</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="封面图" prop="coverUrl">
              <el-input v-model="form.coverUrl" placeholder="图片 URL（可选）" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="商品描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  pageProducts,
  listBrands,
  createProduct,
  updateProduct,
  deleteProduct,
  batchDeleteProducts
} from '@/api/product'
import { listAllCategories } from '@/api/category'
import { hasPermission } from '@/utils/permission'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)
const selection = ref([])
const categories = ref([])
const brands = ref([])

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: null,
  status: null,
  brand: null,
  minPrice: null,
  maxPrice: null
})

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive(defaultForm())

function defaultForm() {
  return {
    id: null,
    name: '',
    code: '',
    categoryId: null,
    brand: '',
    price: 0,
    stock: 0,
    unit: '件',
    status: 1,
    sales: 0,
    description: '',
    coverUrl: ''
  }
}

const formRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入商品编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '编码只能包含字母、数字、- 和 _', trigger: 'blur' },
    { min: 2, max: 64, message: '编码长度为 2 - 64 个字符', trigger: 'blur' }
  ],
  categoryId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  price: [
    { required: true, message: '请输入价格', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value < 0) callback(new Error('价格必须大于等于 0'))
        else callback()
      },
      trigger: 'change'
    }
  ],
  stock: [
    { required: true, message: '请输入库存', trigger: 'change' },
    {
      validator: (rule, value, callback) => {
        if (value === null || value === undefined || value < 0) callback(new Error('库存必须大于等于 0'))
        else callback()
      },
      trigger: 'change'
    }
  ]
}

function formatMoney(v) {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.keyword) params.keyword = query.keyword
  if (query.categoryId !== null && query.categoryId !== '') params.categoryId = query.categoryId
  if (query.status !== null && query.status !== '') params.status = query.status
  if (query.brand) params.brand = query.brand
  if (query.minPrice !== null && query.minPrice !== undefined && query.minPrice !== '') params.minPrice = query.minPrice
  if (query.maxPrice !== null && query.maxPrice !== undefined && query.maxPrice !== '') params.maxPrice = query.maxPrice
  return params
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageProducts(buildParams())
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  try {
    const [cats, bs] = await Promise.all([listAllCategories(), listBrands()])
    categories.value = cats || []
    brands.value = bs || []
  } catch (e) {
    /* 已提示 */
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  Object.assign(query, {
    page: 1,
    size: query.size,
    keyword: '',
    categoryId: null,
    status: null,
    brand: null,
    minPrice: null,
    maxPrice: null
  })
  loadList()
}

function handleSizeChange() {
  query.page = 1
  loadList()
}

function openCreate() {
  isEdit.value = false
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

async function openEdit(row) {
  isEdit.value = true
  Object.assign(form, defaultForm(), {
    id: row.id,
    name: row.name,
    code: row.code,
    categoryId: row.categoryId,
    brand: row.brand,
    price: Number(row.price ?? 0),
    stock: Number(row.stock ?? 0),
    unit: row.unit || '件',
    status: row.status ?? 1,
    sales: Number(row.sales ?? 0),
    description: row.description || '',
    coverUrl: row.coverUrl || ''
  })
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { ...form }
      delete payload.id
      if (isEdit.value) {
        await updateProduct(form.id, payload)
        ElMessage.success('商品修改成功')
      } else {
        await createProduct(payload)
        ElMessage.success('商品新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除商品「${row.name}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteProduct(row.id)
  ElMessage.success('删除成功')
  loadList()
}

async function handleBatchDelete() {
  if (selection.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selection.value.length} 个商品吗？`, '批量删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await batchDeleteProducts({ ids: selection.value.map((r) => r.id) })
  ElMessage.success('批量删除成功')
  loadList()
}

onMounted(() => {
  loadOptions()
  loadList()
})
</script>

<style scoped>
.range-sep {
  margin: 0 6px;
  color: #909399;
}
</style>
