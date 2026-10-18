<template>
  <div class="page-container">
    <!-- 索引状态条 -->
    <el-card class="status-card" shadow="never" v-loading="statusLoading">
      <div class="status-inner">
        <div class="status-item">
          <span class="status-label">索引名称</span>
          <el-tag type="primary">{{ indexStatus.indexName || '-' }}</el-tag>
        </div>
        <div class="status-item">
          <span class="status-label">索引状态</span>
          <el-tag :type="indexStatus.exists ? 'success' : 'danger'">
            {{ indexStatus.exists ? '已创建（可用）' : '未创建（请重建索引）' }}
          </el-tag>
        </div>
        <div class="status-item">
          <span class="status-label">文档数量</span>
          <b class="doc-count">{{ indexStatus.documentCount ?? 0 }}</b>
        </div>
        <div class="status-item">
          <span class="status-label">分词器</span>
          <el-tag type="info">{{ indexStatus.analyzer || '-' }}</el-tag>
        </div>
        <div class="status-actions">
          <el-button :icon="Refresh" @click="refreshStatus">刷新状态</el-button>
          <el-button
            v-permission="'search:rebuild'"
            type="warning"
            :icon="RefreshRight"
            :loading="rebuildLoading"
            @click="handleRebuild"
          >
            重建索引
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 搜索条件 -->
    <el-card class="filter-card" shadow="never">
      <el-form :model="form" label-width="80px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="关键词">
              <el-input
                v-model="form.keyword"
                placeholder="商品名称 / 品牌 / 描述，如：手机"
                clearable
                @keyup.enter="handleSearch"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="所属分类">
              <el-select v-model="form.categoryId" placeholder="全部分类" clearable style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="品牌">
              <el-select v-model="form.brand" placeholder="全部品牌" clearable filterable style="width: 100%">
                <el-option v-for="b in brands" :key="b" :label="b" :value="b" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="状态">
              <el-select v-model="form.status" placeholder="全部状态" clearable style="width: 100%">
                <el-option label="上架" :value="1" />
                <el-option label="下架" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="价格区间">
              <div class="price-range">
                <el-input-number v-model="form.minPrice" :min="0" :controls="false" placeholder="最低" />
                <span class="range-sep">-</span>
                <el-input-number v-model="form.maxPrice" :min="0" :controls="false" placeholder="最高" />
              </div>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :md="8">
            <el-form-item label="排序">
              <div class="sort-box">
                <el-select v-model="form.sortField" style="width: 130px">
                  <el-option label="相关度" value="_score" />
                  <el-option label="价格" value="price" />
                  <el-option label="销量" value="sales" />
                  <el-option label="库存" value="stock" />
                  <el-option label="时间" value="createdAt" />
                </el-select>
                <el-radio-group v-model="form.sortOrder" class="sort-order">
                  <el-radio-button value="desc">降序</el-radio-button>
                  <el-radio-button value="asc">升序</el-radio-button>
                </el-radio-group>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label=" ">
              <el-button type="primary" :icon="Search" :loading="loading" @click="handleSearch">搜 索</el-button>
              <el-button :icon="RefreshLeft" @click="handleReset">重置条件</el-button>
              <span class="text-muted" style="margin-left: 12px">
                高亮标签由后端 Elasticsearch 返回，前端用 v-html 渲染
              </span>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 结果概要 -->
    <el-card class="result-card" shadow="never">
      <div class="result-head">
        <div class="result-count">
          共 <b>{{ total }}</b> 条结果
          <span v-if="keywordText" class="text-muted">（关键词：{{ keywordText }}）</span>
        </div>
        <div class="result-meta">
          <el-tag v-if="took !== null" type="success" effect="plain" size="small">耗时 {{ took }} ms</el-tag>
          <el-tag type="info" effect="plain" size="small">第 {{ form.page }} / {{ pages }} 页</el-tag>
          <el-tag type="warning" effect="plain" size="small">排序：{{ sortFieldText }} {{ form.sortOrder === 'desc' ? '降序' : '升序' }}</el-tag>
        </div>
      </div>

      <div v-loading="loading">
        <el-empty v-if="!loading && results.length === 0" description="没有匹配的商品，试试其它关键词或放宽筛选条件" />

        <div v-else class="result-list">
          <div v-for="item in results" :key="item.id" class="result-item">
            <div class="item-main">
              <div class="item-title">
                <span class="item-name" v-html="safeHighlight(item.nameHighlight, item.name)"></span>
                <el-tag v-if="item.status === 1" type="success" size="small" effect="plain">上架</el-tag>
                <el-tag v-else type="info" size="small" effect="plain">下架</el-tag>
                <el-tag v-if="item.score !== undefined && item.score !== null" type="warning" size="small" effect="plain">
                  score {{ Number(item.score).toFixed(2) }}
                </el-tag>
              </div>

              <div class="item-sub">
                <span class="sub-label">品牌：</span>
                <span v-html="safeHighlight(item.brandHighlight, item.brand || '-')"></span>
                <el-divider direction="vertical" />
                <span class="sub-label">分类：</span>
                <span>{{ item.categoryName || '-' }}</span>
                <el-divider direction="vertical" />
                <span class="sub-label">编码：</span>
                <span>{{ item.code }}</span>
              </div>

              <div class="item-desc search-desc" v-html="safeHighlight(item.descriptionHighlight, item.description)"></div>

              <!-- 后端 highlights 字段补充展示 -->
              <div v-if="extraHighlights(item).length" class="item-fragments">
                <span v-for="(frag, idx) in extraHighlights(item)" :key="idx" class="fragment" v-html="frag.fragment"></span>
              </div>
            </div>

            <div class="item-side">
              <div class="side-price">¥{{ formatMoney(item.price) }}</div>
              <div class="side-row">库存：<b>{{ item.stock ?? 0 }}</b></div>
              <div class="side-row">销量：<b>{{ item.sales ?? 0 }}</b></div>
            </div>
          </div>
        </div>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="form.page"
          v-model:page-size="form.size"
          :total="total"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 索引重建任务 -->
    <el-card shadow="never" class="task-card">
      <template #header>
        <div class="flex-between">
          <span class="panel-title">
            <el-icon><RefreshRight /></el-icon> 索引重建任务（RabbitMQ 异步执行）
          </span>
          <div>
            <el-tag v-if="polling" type="warning" size="small" effect="plain">轮询中…</el-tag>
            <el-button size="small" :icon="Refresh" :loading="taskLoading" @click="loadTasks">刷新任务</el-button>
          </div>
        </div>
      </template>

      <el-alert
        v-if="lastTaskNo"
        :title="`最近提交任务号：${lastTaskNo}（已投递到 RabbitMQ 队列，后台消费者异步执行）`"
        type="success"
        :closable="false"
        class="mb-12"
      />

      <el-table :data="tasks" size="small" border v-loading="taskLoading" empty-text="暂无索引任务">
        <el-table-column prop="taskNo" label="任务号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="100" align="center" />
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="taskStatusType(row.status)" size="small" effect="dark">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="indexName" label="索引" min-width="130" show-overflow-tooltip />
        <el-table-column prop="totalCount" label="总数" width="80" align="center" />
        <el-table-column prop="successCount" label="成功" width="80" align="center" />
        <el-table-column prop="failCount" label="失败" width="80" align="center" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" align="right" />
        <el-table-column prop="createdBy" label="创建人" width="100" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" min-width="165" show-overflow-tooltip />
        <el-table-column prop="message" label="消息" min-width="160" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Refresh, RefreshRight } from '@element-plus/icons-vue'
import { searchProducts, getSearchStatus } from '@/api/search'
import { rebuildIndex, pageIndexTasks } from '@/api/indexTask'
import { listAllCategories } from '@/api/category'
import { listBrands } from '@/api/product'

const loading = ref(false)
const statusLoading = ref(false)
const taskLoading = ref(false)
const rebuildLoading = ref(false)
const polling = ref(false)

const results = ref([])
const total = ref(0)
const pages = ref(1)
const took = ref(null)
const keywordText = ref('')
const categories = ref([])
const brands = ref([])
const indexStatus = ref({ indexName: '-', exists: false, documentCount: 0, analyzer: '-' })
const tasks = ref([])
const lastTaskNo = ref('')

let pollTimer = null

const form = reactive({
  keyword: '',
  categoryId: null,
  brand: null,
  status: null,
  minPrice: null,
  maxPrice: null,
  page: 1,
  size: 10,
  sortField: '_score',
  sortOrder: 'desc'
})

const sortFieldTextMap = { _score: '相关度', price: '价格', sales: '销量', stock: '库存', createdAt: '时间' }
const sortFieldText = computed(() => sortFieldTextMap[form.sortField] || form.sortField)

function formatMoney(v) {
  if (v === null || v === undefined) return '0.00'
  return Number(v).toFixed(2)
}

/**
 * 高亮字段（nameHighlight / brandHighlight / descriptionHighlight）已由后端
 * 注入 <em class="hl">关键词</em> 标签，这里统一做兜底：若后端未返回高亮字段
 * 则退回原文，避免 v-html 渲染出 undefined / 空字符串。
 * 注意：内容来自本系统后端（Elasticsearch 高亮片段），故使用 v-html 渲染。
 */
function safeHighlight(highlight, fallback) {
  const value = highlight ?? fallback ?? ''
  return value === '' ? '-' : String(value)
}

function extraHighlights(item) {
  const list = item?.highlights || []
  // name / brand / description 已在主区域渲染，这里只展示其它字段的片段
  return list.filter((h) => h && h.fragment && !['name', 'brand', 'description'].includes(h.field))
}

function taskStatusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'primary'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

function buildBody() {
  const body = { page: form.page, size: form.size, sortField: form.sortField, sortOrder: form.sortOrder }
  if (form.keyword) body.keyword = form.keyword
  if (form.categoryId !== null && form.categoryId !== '') body.categoryId = form.categoryId
  if (form.brand) body.brand = form.brand
  if (form.status !== null && form.status !== '') body.status = form.status
  if (form.minPrice !== null && form.minPrice !== undefined && form.minPrice !== '') body.minPrice = form.minPrice
  if (form.maxPrice !== null && form.maxPrice !== undefined && form.maxPrice !== '') body.maxPrice = form.maxPrice
  return body
}

async function handleSearch() {
  loading.value = true
  const start = performance.now()
  try {
    const data = await searchProducts(buildBody())
    results.value = data?.records || []
    total.value = data?.total || 0
    pages.value = data?.pages || 1
    keywordText.value = form.keyword || ''
    // 后端若未返回耗时，则用前端计时兜底
    took.value = data?.took ?? Math.round(performance.now() - start)
  } catch (e) {
    results.value = []
    total.value = 0
    pages.value = 1
    took.value = null
  } finally {
    loading.value = false
  }
}

function handleReset() {
  Object.assign(form, {
    keyword: '',
    categoryId: null,
    brand: null,
    status: null,
    minPrice: null,
    maxPrice: null,
    page: 1,
    sortField: '_score',
    sortOrder: 'desc'
  })
  handleSearch()
}

function handleSizeChange() {
  form.page = 1
  handleSearch()
}

async function refreshStatus() {
  statusLoading.value = true
  try {
    const data = await getSearchStatus()
    indexStatus.value = data?.indexStatus || { indexName: '-', exists: false, documentCount: 0, analyzer: '-' }
  } catch (e) {
    /* 已提示 */
  } finally {
    statusLoading.value = false
  }
}

async function loadTasks() {
  taskLoading.value = true
  try {
    const data = await pageIndexTasks({ page: 1, size: 5 })
    tasks.value = data?.records || []
    return tasks.value
  } catch (e) {
    tasks.value = []
    return []
  } finally {
    taskLoading.value = false
  }
}

/** 提交重建任务并开始轮询任务状态 */
async function handleRebuild() {
  try {
    await ElMessageBox.confirm(
      '将向 RabbitMQ 投递全量重建索引任务，由后台消费者异步执行。是否继续？',
      '重建索引确认',
      { confirmButtonText: '提交任务', cancelButtonText: '取消', type: 'warning' }
    )
  } catch (e) {
    return
  }
  rebuildLoading.value = true
  try {
    const data = await rebuildIndex()
    lastTaskNo.value = data?.taskNo || ''
    ElMessage.success('任务已提交，正在后台通过 RabbitMQ 执行')
    await loadTasks()
    startPolling()
  } finally {
    rebuildLoading.value = false
  }
}

function startPolling() {
  stopPolling()
  polling.value = true
  let times = 0
  pollTimer = setInterval(async () => {
    times += 1
    const list = await loadTasks()
    // 任务全部结束或已轮询 40 次（约 2 分钟）后停止
    const running = list.some((t) => t.status === 'PENDING' || t.status === 'RUNNING')
    if (!running || times >= 40) {
      stopPolling()
      if (!running) {
        await refreshStatus()
        ElMessage.success('索引重建任务已结束，索引状态已刷新')
      }
    }
  }, 3000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
  polling.value = false
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

onMounted(() => {
  loadOptions()
  refreshStatus()
  loadTasks()
  handleSearch()
})

onUnmounted(stopPolling)
</script>

<style scoped>
.status-card {
  margin-bottom: 16px;
  border-radius: 8px;
  background: linear-gradient(135deg, #f6f9ff 0%, #eef4ff 100%);
}

.status-inner {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 26px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  color: #909399;
  font-size: 13px;
}

.doc-count {
  font-size: 20px;
  color: #409eff;
}

.status-actions {
  margin-left: auto;
}

.price-range {
  display: flex;
  align-items: center;
  width: 100%;
}

.price-range :deep(.el-input-number) {
  flex: 1;
}

.range-sep {
  margin: 0 6px;
  color: #909399;
}

.sort-box {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.result-card {
  border-radius: 8px;
  margin-bottom: 16px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
  margin-bottom: 12px;
  border-bottom: 1px dashed #ebeef5;
  flex-wrap: wrap;
  gap: 8px;
}

.result-count {
  font-size: 15px;
  color: #303133;
}

.result-count b {
  color: #f56c6c;
  font-size: 18px;
}

.result-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  display: flex;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  transition: all 0.2s;
  background: #fff;
}

.result-item:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.16);
  transform: translateY(-1px);
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  font-size: 16px;
}

.item-name {
  font-weight: 600;
  color: #303133;
}

.item-sub {
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.sub-label {
  color: #909399;
}

.item-desc {
  margin-top: 8px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
  max-height: 66px;
  overflow: hidden;
}

.item-fragments {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.fragment {
  font-size: 12px;
  color: #909399;
}

.item-side {
  width: 130px;
  text-align: right;
  flex-shrink: 0;
  border-left: 1px dashed #ebeef5;
  padding-left: 12px;
}

.side-price {
  font-size: 20px;
  font-weight: 700;
  color: #f56c6c;
  margin-bottom: 8px;
}

.side-row {
  font-size: 13px;
  color: #909399;
  line-height: 1.8;
}

.side-row b {
  color: #303133;
}

.task-card {
  border-radius: 8px;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}
</style>
