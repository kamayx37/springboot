<template>
  <div class="page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="角色名称 / 编码" clearable style="width: 220px" @keyup.enter="handleSearch" />
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
        <div class="toolbar-title">角色列表（共 {{ total }} 条）</div>
        <el-button v-permission="'role:create'" type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="name" label="角色名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="code" label="角色编码" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="110" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small" effect="plain">{{ (row.permissionIds || []).length }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'role:update'" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'role:assign'" type="warning" link :icon="Key" @click="openAssign(row)">分配权限</el-button>
            <el-button v-permission="'role:delete'" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无角色数据" />
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

    <!-- 新增 / 编辑角色 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：商品管理员" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" placeholder="如：PRODUCT_ADMIN（大写字母 / 下划线）" maxlength="32" />
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

    <!-- 分配权限 -->
    <el-dialog v-model="assignVisible" title="分配权限" width="600px" destroy-on-close @open="onAssignOpen">
      <el-alert
        :title="`为角色「${currentRole.name || ''}」分配权限（父级为半选状态时也会一并提交）`"
        type="info"
        :closable="false"
        class="mb-12"
      />
      <div class="tree-toolbar">
        <el-button size="small" @click="checkAll(true)">全选</el-button>
        <el-button size="small" @click="checkAll(false)">清空</el-button>
        <el-button size="small" @click="expandAll(true)">展开全部</el-button>
        <el-button size="small" @click="expandAll(false)">收起全部</el-button>
        <span class="text-muted">已选 {{ checkedCount }} 项</span>
      </div>
      <el-scrollbar height="380px" class="tree-scroll">
        <el-tree
          ref="treeRef"
          :data="permissionTree"
          show-checkbox
          node-key="id"
          default-expand-all
          :props="{ label: 'name', children: 'children' }"
          :default-checked-keys="defaultCheckedKeys"
          @check="onTreeCheck"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span class="tree-label">{{ data.name }}</span>
              <el-tag size="small" :type="typeTag(data.type)" effect="plain" class="tree-tag">{{ data.type }}</el-tag>
              <span class="text-muted tree-code">{{ data.code }}</span>
            </span>
          </template>
        </el-tree>
        <el-empty v-if="permissionTree.length === 0" description="暂无权限数据" />
      </el-scrollbar>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Key } from '@element-plus/icons-vue'
import { pageRoles, createRole, updateRole, deleteRole, assignPermissions } from '@/api/role'
import { getPermissionTree } from '@/api/permission'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 10, keyword: '', status: null })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive(defaultForm())

const assignVisible = ref(false)
const treeRef = ref(null)
const permissionTree = ref([])
const defaultCheckedKeys = ref([])
const currentRole = ref({})
const checkedCount = ref(0)

function defaultForm() {
  return { id: null, code: '', name: '', description: '', status: 1, sortOrder: 0, permissionIds: [] }
}

const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]+$/, message: '编码只能包含字母、数字和下划线', trigger: 'blur' }
  ]
}

function typeTag(type) {
  if (type === 'MENU') return 'primary'
  if (type === 'BUTTON') return 'success'
  return 'warning'
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await pageRoles(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
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
  Object.assign(form, defaultForm(), {
    id: row.id,
    code: row.code,
    name: row.name,
    description: row.description || '',
    status: row.status ?? 1,
    sortOrder: row.sortOrder ?? 0,
    permissionIds: row.permissionIds || []
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
        await updateRole(form.id, payload)
        ElMessage.success('角色修改成功')
      } else {
        await createRole(payload)
        ElMessage.success('角色新增成功')
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
    await ElMessageBox.confirm(`确定要删除角色「${row.name}」吗？已分配该角色的用户将失去对应权限。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadList()
}

/* ---------------- 分配权限 ---------------- */
async function openAssign(row) {
  currentRole.value = row
  defaultCheckedKeys.value = row.permissionIds || []
  assignVisible.value = true
  if (permissionTree.value.length === 0) {
    try {
      permissionTree.value = (await getPermissionTree()) || []
    } catch (e) {
      permissionTree.value = []
    }
  }
}

function onAssignOpen() {
  // 弹窗打开后再设置勾选，避免 el-tree 未渲染导致勾选失效
  nextTick(() => {
    if (treeRef.value) {
      // 只设置叶子节点，父节点由 el-tree 自行推导半选 / 全选状态
      const leafIds = collectLeafIds(permissionTree.value)
      const keys = (defaultCheckedKeys.value || []).filter((id) => leafIds.includes(id))
      const parentOnly = (defaultCheckedKeys.value || []).filter((id) => !leafIds.includes(id))
      treeRef.value.setCheckedKeys(keys)
      // 父节点单独勾选（若其子节点没有被全部勾选）也需要还原
      parentOnly.forEach((id) => {
        const node = treeRef.value.getNode(id)
        if (node) node.checked = true
      })
      updateCheckedCount()
    }
  })
}

function collectLeafIds(nodes, result = []) {
  ;(nodes || []).forEach((n) => {
    if (n.children && n.children.length) collectLeafIds(n.children, result)
    else result.push(n.id)
  })
  return result
}

function collectAllIds(nodes, result = []) {
  ;(nodes || []).forEach((n) => {
    result.push(n.id)
    if (n.children && n.children.length) collectAllIds(n.children, result)
  })
  return result
}

function updateCheckedCount() {
  if (!treeRef.value) return
  // 必须同时统计半选父节点，否则父级菜单权限会丢失
  checkedCount.value = treeRef.value.getCheckedKeys().length + treeRef.value.getHalfCheckedKeys().length
}

function onTreeCheck() {
  updateCheckedCount()
}

function checkAll(checked) {
  if (!treeRef.value) return
  treeRef.value.setCheckedKeys(checked ? collectAllIds(permissionTree.value) : [])
  updateCheckedCount()
}

function expandAll(expand) {
  const nodes = treeRef.value?.store?.nodesMap || {}
  Object.values(nodes).forEach((node) => {
    node.expanded = expand
  })
}

async function submitAssign() {
  if (!treeRef.value) return
  // 关键：checked + halfChecked，保证父级菜单权限一并提交
  const ids = treeRef.value.getCheckedKeys().concat(treeRef.value.getHalfCheckedKeys())
  submitting.value = true
  try {
    await assignPermissions({ id: currentRole.value.id, ids })
    ElMessage.success(`权限分配成功，共提交 ${ids.length} 项`)
    assignVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.tree-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.tree-scroll {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 6px 10px;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tree-label {
  font-size: 13px;
}

.tree-tag {
  transform: scale(0.9);
}

.tree-code {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
}
</style>
