<template>
  <div class="page-container">
    <!-- 筛选区 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="query" inline>
        <el-form-item label="操作人">
          <el-input v-model="query.username" placeholder="用户名" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="query.module" placeholder="全部模块" clearable filterable style="width: 160px">
            <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作">
          <el-input v-model="query.operation" placeholder="操作名称" clearable style="width: 150px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="query.success" placeholder="全部结果" clearable style="width: 130px">
            <el-option label="成功" :value="true" />
            <el-option label="失败" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
          />
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
        <div class="toolbar-title">操作日志（共 {{ total }} 条）</div>
        <el-button v-permission="'log:clear'" type="danger" plain :icon="Delete" @click="handleClear">清空日志</el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="expand-box">
              <div class="expand-item">
                <div class="expand-label">请求参数 params</div>
                <pre class="expand-pre">{{ prettyParams(row.params) }}</pre>
              </div>
              <div class="expand-item">
                <div class="expand-label">异常信息 errorMessage</div>
                <pre class="expand-pre error-pre">{{ row.errorMessage || '无' }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="110" show-overflow-tooltip />
        <el-table-column prop="module" label="模块" width="110" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.module || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="operation" label="操作" min-width="130" show-overflow-tooltip />
        <el-table-column prop="httpMethod" label="请求方式" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="methodTag(row.httpMethod)" size="small" effect="dark">{{ row.httpMethod }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUri" label="请求路径" min-width="220" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" show-overflow-tooltip />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="right">
          <template #default="{ row }">
            <span :class="row.durationMs > 1000 ? 'slow' : ''">{{ row.durationMs ?? '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="success" label="结果" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" min-width="165" show-overflow-tooltip />
        <template #empty>
          <el-empty description="暂无日志数据" />
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Delete } from '@element-plus/icons-vue'
import { pageLogs, listLogModules, clearLogs } from '@/api/log'

const loading = ref(false)
const list = ref([])
const modules = ref([])
const total = ref(0)
const dateRange = ref([])

const query = reactive({
  page: 1,
  size: 10,
  username: '',
  module: null,
  operation: '',
  success: null
})

function methodTag(method) {
  if (method === 'GET') return 'primary'
  if (method === 'POST') return 'success'
  if (method === 'PUT') return 'warning'
  if (method === 'DELETE') return 'danger'
  return 'info'
}

function prettyParams(params) {
  if (!params) return '无'
  try {
    const obj = typeof params === 'string' ? JSON.parse(params) : params
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return String(params)
  }
}

function buildParams() {
  const params = { page: query.page, size: query.size }
  if (query.username) params.username = query.username
  if (query.module) params.module = query.module
  if (query.operation) params.operation = query.operation
  if (query.success !== null && query.success !== '') params.success = query.success
  // el-date-picker value-format="YYYY-MM-DD" → 直接是 yyyy-MM-dd
  if (dateRange.value && dateRange.value.length === 2) {
    params.startDate = dateRange.value[0]
    params.endDate = dateRange.value[1]
  }
  return params
}

async function loadList() {
  loading.value = true
  try {
    const data = await pageLogs(buildParams())
    list.value = data?.records || []
    total.value = data?.total || 0
  } catch (e) {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadModules() {
  try {
    modules.value = (await listLogModules()) || []
  } catch (e) {
    modules.value = []
  }
}

function handleSearch() {
  query.page = 1
  loadList()
}

function handleReset() {
  Object.assign(query, { page: 1, size: query.size, username: '', module: null, operation: '', success: null })
  dateRange.value = []
  loadList()
}

function handleSizeChange() {
  query.page = 1
  loadList()
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确定要清空全部操作日志吗？该操作不可恢复，且同样会被记录。', '危险操作确认', {
      confirmButtonText: '确定清空',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await clearLogs()
  ElMessage.success('日志已清空')
  query.page = 1
  loadList()
}

onMounted(() => {
  loadModules()
  loadList()
})
</script>

<style scoped>
.expand-box {
  padding: 8px 20px 12px 48px;
}

.expand-item {
  margin-bottom: 10px;
}

.expand-label {
  font-weight: 600;
  color: #606266;
  margin-bottom: 4px;
}

.expand-pre {
  margin: 0;
  background: #f5f7fa;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 8px 10px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 220px;
  overflow: auto;
}

.error-pre {
  color: #f56c6c;
}

.slow {
  color: #e6a23c;
  font-weight: 600;
}
</style>
