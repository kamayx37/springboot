<template>
  <div class="page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="权限名称 / 标识" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="权限类型">
          <el-select v-model="query.type" placeholder="全部类型" clearable style="width: 150px">
            <el-option label="菜单 MENU" value="MENU" />
            <el-option label="按钮 BUTTON" value="BUTTON" />
            <el-option label="接口 API" value="API" />
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
        <div class="toolbar-title">权限列表（共 {{ total }} 条）</div>
        <el-button v-permission="'permission:create'" type="primary" :icon="Plus" @click="openCreate">新增权限</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="权限名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="权限标识" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain" class="code-tag">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="上级权限" min-width="130">
          <template #default="{ row }">
            <span v-if="row.parentId && row.parentId !== 0">{{ parentName(row.parentId) }}</span>
            <el-tag v-else size="small" type="info" effect="plain">顶级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路由路径" min-width="140" show-overflow-tooltip />
        <el-table-column prop="icon" label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.icon"><component :is="row.icon" /></el-icon>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="description" label="描述" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'permission:update'" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'permission:delete'" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无权限数据" />
        </template>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑权限 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑权限' : '新增权限'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="权限标识" prop="code">
              <el-input v-model="form.code" placeholder="如：product:create" maxlength="64" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限名称" prop="name">
              <el-input v-model="form.name" placeholder="如：新增商品" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="权限类型" prop="type">
              <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
                <el-option label="菜单 MENU" value="MENU" />
                <el-option label="按钮 BUTTON" value="BUTTON" />
                <el-option label="接口 API" value="API" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="上级权限" prop="parentId">
              <el-select v-model="form.parentId" placeholder="请选择上级权限" style="width: 100%" clearable>
                <el-option label="顶级权限" :value="0" />
                <el-option v-for="p in parentOptions" :key="p.id" :label="`${p.name}（${p.code}）`" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路由路径" prop="path">
              <el-input v-model="form.path" placeholder="如：/goods/products" maxlength="128" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="Element Plus 图标名，如 Goods" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :precision="0" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete } from '@element-plus/icons-vue'
import {
  pagePermissions,
  listAllPermissions,
  createPermission,
  updatePermission,
  deletePermission
} from '@/api/permission'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const allPermissions = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 10, keyword: '', type: null })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive(defaultForm())

function defaultForm() {
  return {
    id: null,
    code: '',
    name: '',
    type: 'BUTTON',
    parentId: 0,
    path: '',
    icon: '',
    sortOrder: 0,
    description: ''
  }
}

/** 上级权限候选：排除自身 */
const parentOptions = computed(() => allPermissions.value.filter((p) => p.id !== form.id))

const formRules = {
  code: [
    { required: true, message: '请输入权限标识', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_:.-]+$/, message: '标识只能包含字母、数字、: _ . -', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入权限名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择权限类型', trigger: 'change' }]
}

function typeTag(type) {
  if (type === 'MENU') return 'primary'
  if (type === 'BUTTON') return 'success'
  return 'warning'
}

function parentName(parentId) {
  const found = allPermissions.value.find((p) => p.id === parentId)
  return found ? found.name : `#${parentId}`
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.type) params.type = query.type
    const data = await pagePermissions(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadAll() {
  try {
    allPermissions.value = (await listAllPermissions()) || []
  } catch (e) {
    allPermissions.value = []
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { page: 1, size: query.size, keyword: '', type: null })
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
  Object.assign(form, defaultForm(), {
    id: row.id,
    code: row.code,
    name: row.name,
    type: row.type || 'BUTTON',
    parentId: row.parentId ?? 0,
    path: row.path || '',
    icon: row.icon || '',
    sortOrder: row.sortOrder ?? 0,
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
        await updatePermission(form.id, payload)
        ElMessage.success('权限修改成功')
      } else {
        await createPermission(payload)
        ElMessage.success('权限新增成功')
      }
      dialogVisible.value = false
      loadList()
      loadAll()
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除权限「${row.name}（${row.code}）」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deletePermission(row.id)
  ElMessage.success('删除成功')
  loadList()
  loadAll()
}

onMounted(() => {
  loadAll()
  loadList()
})
</script>

<style scoped>
.code-tag {
  font-family: Consolas, Monaco, monospace;
}
</style>
