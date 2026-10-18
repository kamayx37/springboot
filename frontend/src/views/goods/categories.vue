<template>
  <div class="page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="分类名称 / 编码" clearable style="width: 200px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
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
        <div class="toolbar-title">商品分类（共 {{ total }} 条）</div>
        <el-button v-permission="'category:create'" type="primary" :icon="Plus" @click="openCreate">新增分类</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="分类名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="code" label="分类编码" min-width="140" show-overflow-tooltip />
        <el-table-column label="上级分类" min-width="130">
          <template #default="{ row }">
            <el-tag v-if="row.parentId && row.parentId !== 0" size="small" effect="plain">
              {{ row.parentName || '未知上级' }}
            </el-tag>
            <el-tag v-else size="small" type="info" effect="plain">顶级分类</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="商品数" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.productCount > 0 ? 'warning' : 'info'" size="small" effect="plain">
              {{ row.productCount ?? 0 }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'category:update'" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'category:delete'" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无分类数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="分类编码" prop="code">
          <el-input v-model="form.code" placeholder="仅字母 / 数字 / - / _" maxlength="32" />
        </el-form-item>
        <el-form-item label="上级分类" prop="parentId">
          <el-select v-model="form.parentId" placeholder="请选择上级分类" style="width: 100%">
            <el-option label="顶级分类" :value="0" />
            <el-option v-for="c in parentOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :precision="0" style="width: 180px" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import { pageCategories, listAllCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const allCategories = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 10, keyword: '', status: null })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive(defaultForm())

function defaultForm() {
  return { id: null, name: '', code: '', parentId: 0, sortOrder: 0, status: 1, description: '' }
}

/** 上级分类候选：排除自身，避免形成自引用 */
const parentOptions = computed(() => allCategories.value.filter((c) => c.id !== form.id))

const formRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入分类编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_-]+$/, message: '编码只能包含字母、数字、- 和 _', trigger: 'blur' }
  ],
  parentId: [{ required: true, message: '请选择上级分类（顶级填 0）', trigger: 'change' }]
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await pageCategories(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadAllCategories() {
  try {
    allCategories.value = (await listAllCategories()) || []
  } catch (e) {
    allCategories.value = []
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { page: 1, size: query.size, keyword: '', status: null })
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

function openEdit(row) {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    name: row.name,
    code: row.code,
    parentId: row.parentId ?? 0,
    sortOrder: row.sortOrder ?? 0,
    status: row.status ?? 1,
    description: row.description || ''
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
        await updateCategory(form.id, payload)
        ElMessage.success('分类修改成功')
      } else {
        await createCategory(payload)
        ElMessage.success('分类新增成功')
      }
      dialogVisible.value = false
      loadList()
      loadAllCategories()
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定要删除分类「${row.name}」吗？该分类下若有 ${row.productCount ?? 0} 个商品可能受影响。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  loadList()
  loadAllCategories()
}

onMounted(() => {
  loadAllCategories()
  loadList()
})
</script>
