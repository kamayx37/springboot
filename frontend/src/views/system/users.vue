<template>
  <div class="page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="用户名 / 昵称 / 邮箱" clearable style="width: 220px" @keyup.enter="handleSearch" />
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
        <div class="toolbar-title">用户列表（共 {{ total }} 条）</div>
        <el-button v-permission="'user:create'" type="primary" :icon="Plus" @click="openCreate">新增用户</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id">
        <el-table-column prop="id" label="ID" width="70" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" show-overflow-tooltip />
        <el-table-column prop="nickname" label="昵称" min-width="110" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column label="拥有角色" min-width="200">
          <template #default="{ row }">
            <template v-if="row.roles && row.roles.length">
              <el-tag v-for="role in row.roles" :key="role.id ?? role" size="small" type="warning" class="role-tag">
                {{ role.name || role.code || role }}
              </el-tag>
            </template>
            <span v-else class="text-muted">未分配</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-permission="'user:update'" type="primary" link :icon="Edit" @click="openEdit(row)">编辑</el-button>
            <el-button v-permission="'user:assign'" type="warning" link :icon="Key" @click="openAssign(row)">分配角色</el-button>
            <el-button v-permission="'user:update'" type="info" link :icon="Refresh" @click="openReset(row)">重置密码</el-button>
            <el-button v-permission="'user:delete'" type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无用户数据" />
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

    <!-- 新增 / 编辑用户 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="620px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="isEdit" placeholder="登录账号" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password"
                type="password"
                show-password
                :placeholder="isEdit ? '留空表示不修改密码' : '请输入初始密码'"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="显示名称" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="example@mail.com" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="11 位手机号" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="1">启用</el-radio>
                <el-radio :value="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色" prop="roleIds">
              <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
                <el-option v-for="r in roleOptions" :key="r.id" :label="r.name" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="remark">
              <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色 -->
    <el-dialog v-model="assignVisible" title="分配角色" width="520px" destroy-on-close>
      <el-alert :title="`为用户「${currentUser.nickname || currentUser.username || ''}」分配角色`" type="info" :closable="false" class="mb-12" />
      <el-checkbox-group v-model="assignRoleIds">
        <el-checkbox v-for="r in roleOptions" :key="r.id" :value="r.id" class="assign-item">
          <b>{{ r.name }}</b>
          <span class="text-muted">（{{ r.code }}）</span>
        </el-checkbox>
      </el-checkbox-group>
      <el-empty v-if="roleOptions.length === 0" description="暂无可用角色" />
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitAssign">提交</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="440px" destroy-on-close>
      <el-alert
        :title="`将重置用户「${currentUser.username || ''}」的登录密码`"
        type="warning"
        :closable="false"
        class="mb-12"
      />
      <el-form ref="resetFormRef" :model="resetForm" :rules="resetRules" label-width="90px">
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="请输入新密码（至少 6 位）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitReset">确定重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Key, Refresh } from '@element-plus/icons-vue'
import { pageUsers, createUser, updateUser, deleteUser, assignRoles, resetUserPassword } from '@/api/user'
import { listAllRoles } from '@/api/role'

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const roleOptions = ref([])
const total = ref(0)

const query = reactive({ page: 1, size: 10, keyword: '', status: null })

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = reactive(defaultForm())

const assignVisible = ref(false)
const assignRoleIds = ref([])
const currentUser = ref({})

const resetVisible = ref(false)
const resetFormRef = ref(null)
const resetForm = reactive({ newPassword: '' })

function defaultForm() {
  return {
    id: null,
    username: '',
    password: '',
    nickname: '',
    email: '',
    phone: '',
    status: 1,
    remark: '',
    roleIds: []
  }
}

const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 32, message: '用户名长度为 3 - 32 个字符', trigger: 'blur' }
  ],
  password: [
    {
      validator: (rule, value, callback) => {
        if (!isEdit.value && !value) {
          callback(new Error('请输入初始密码'))
          return
        }
        if (value && value.length < 6) {
          callback(new Error('密码至少 6 位'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }]
}

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ]
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== null && query.status !== '') params.status = query.status
    const data = await pageUsers(params)
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  try {
    roleOptions.value = (await listAllRoles()) || []
  } catch (e) {
    roleOptions.value = []
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
    username: row.username,
    password: '',
    nickname: row.nickname,
    email: row.email || '',
    phone: row.phone || '',
    status: row.status ?? 1,
    remark: row.remark || '',
    roleIds: (row.roles || []).map((r) => (typeof r === 'object' ? r.id : r)).filter((v) => v !== undefined)
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
        if (!payload.password) delete payload.password
        await updateUser(form.id, payload)
        ElMessage.success('用户修改成功')
      } else {
        await createUser(payload)
        ElMessage.success('用户新增成功')
      }
      dialogVisible.value = false
      loadList()
    } finally {
      submitting.value = false
    }
  })
}

function openAssign(row) {
  currentUser.value = row
  assignRoleIds.value = (row.roles || []).map((r) => (typeof r === 'object' ? r.id : r)).filter((v) => v !== undefined)
  assignVisible.value = true
}

async function submitAssign() {
  submitting.value = true
  try {
    await assignRoles({ id: currentUser.value.id, ids: assignRoleIds.value })
    ElMessage.success('角色分配成功')
    assignVisible.value = false
    loadList()
  } finally {
    submitting.value = false
  }
}

function openReset(row) {
  currentUser.value = row
  resetForm.newPassword = ''
  resetVisible.value = true
}

async function submitReset() {
  if (!resetFormRef.value) return
  await resetFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      await resetUserPassword(currentUser.value.id, { newPassword: resetForm.newPassword })
      ElMessage.success('密码重置成功')
      resetVisible.value = false
    } finally {
      submitting.value = false
    }
  })
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(() => {
  loadRoles()
  loadList()
})
</script>

<style scoped>
.role-tag {
  margin: 2px 4px 2px 0;
}

.assign-item {
  display: block;
  margin: 6px 0;
}
</style>
