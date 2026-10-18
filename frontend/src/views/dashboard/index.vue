<template>
  <div class="page-container">
    <!-- 欢迎横幅 -->
    <el-card class="welcome-card" shadow="never">
      <div class="welcome-inner">
        <el-avatar :size="58" class="welcome-avatar">{{ avatarText }}</el-avatar>
        <div class="welcome-text">
          <div class="welcome-title">{{ greeting }}，{{ userStore.nickname }}！</div>
          <div class="welcome-sub">
            当前登录账号 <b>{{ userStore.username }}</b>，角色
            <el-tag v-for="r in roleNames" :key="r" size="small" type="warning" effect="dark" class="ml-tag">{{ r }}</el-tag>
            <span v-if="roleNames.length === 0">暂无角色</span>
          </div>
        </div>
        <div class="welcome-time">
          <div class="time-text">{{ nowText }}</div>
          <div class="text-muted">商品管理系统 · 课程作业演示</div>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6" v-for="card in statCards" :key="card.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-body">
            <div class="stat-icon" :style="{ background: card.color }">
              <el-icon :size="24"><component :is="card.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">
                {{ card.value }}
                <span v-if="card.suffix" class="stat-suffix">{{ card.suffix }}</span>
              </div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <!-- 索引状态 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="flex-between">
              <span class="panel-title"><el-icon><Search /></el-icon> Elasticsearch 索引状态</span>
              <el-button size="small" :icon="Refresh" circle :loading="indexLoading" @click="loadIndexOverview" />
            </div>
          </template>

          <el-descriptions :column="1" border v-loading="indexLoading">
            <el-descriptions-item label="索引名称">
              <el-tag type="primary">{{ indexStatus.indexName || '-' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="是否存在">
              <el-tag :type="indexStatus.exists ? 'success' : 'danger'">
                {{ indexStatus.exists ? '已创建' : '未创建' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="文档数量">
              <b class="doc-count">{{ indexStatus.documentCount ?? 0 }}</b> 条
            </el-descriptions-item>
            <el-descriptions-item label="分词器">{{ indexStatus.analyzer || '-' }}</el-descriptions-item>
            <el-descriptions-item label="MQ 队列">
              <el-tag type="info" size="small">{{ overview.queue || '-' }}</el-tag>
              <span class="text-muted" style="margin-left: 8px">{{ overview.exchange || '' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="路由键">{{ overview.routingKey || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 在线会话 -->
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="flex-between">
              <span class="panel-title"><el-icon><Connection /></el-icon> 系统运行概况</span>
              <el-button size="small" :icon="Refresh" circle :loading="onlineLoading" @click="loadOnline" />
            </div>
          </template>

          <div class="online-box" v-loading="onlineLoading">
            <el-icon class="online-icon"><UserFilled /></el-icon>
            <div>
              <div class="online-value">{{ onlineSessions }}</div>
              <div class="text-muted">当前在线会话数（Redis 统计）</div>
            </div>
          </div>

          <el-divider />

          <div class="recent-task">
            <div class="recent-title">最近索引任务</div>
            <el-table :data="recentTasks" size="small" empty-text="暂无任务记录" max-height="220">
              <el-table-column prop="taskNo" label="任务号" min-width="150" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="totalCount" label="总数" width="70" align="center" />
              <el-table-column prop="createdAt" label="创建时间" min-width="150" show-overflow-tooltip />
            </el-table>
            <el-button class="mt-8" size="small" type="primary" link @click="$router.push('/search')">
              前往全文检索页查看任务详情 →
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 权限概览 -->
    <el-card shadow="never" class="panel-card mt-16">
      <template #header>
        <span class="panel-title"><el-icon><Key /></el-icon> 我的权限（共 {{ userStore.permissionCount }} 个）</span>
      </template>
      <div v-if="userStore.permissionCount === 0" class="text-muted">当前账号没有任何权限码</div>
      <div v-else class="perm-tags">
        <el-tag v-for="p in sortedPermissions" :key="p" size="small" class="perm-tag" effect="plain">{{ p }}</el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getOnline } from '@/api/auth'
import { getIndexOverview } from '@/api/indexTask'

const userStore = useUserStore()

const onlineLoading = ref(false)
const indexLoading = ref(false)
const onlineSessions = ref(0)
const overview = ref({})
const indexStatus = ref({ indexName: '-', exists: false, documentCount: 0, analyzer: '-' })

const roleNames = computed(() =>
  (userStore.userInfo?.roles || []).map((r) => (typeof r === 'string' ? r : r?.name || r?.code || '')).filter(Boolean)
)
const avatarText = computed(() => (userStore.nickname || 'U').slice(0, 1).toUpperCase())
const sortedPermissions = computed(() => [...(userStore.permissions || [])].sort())
const recentTasks = computed(() => overview.value?.recentTasks || [])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '凌晨好'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const nowText = computed(() => {
  const d = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} 星期${week} ${pad(d.getHours())}:${pad(
    d.getMinutes()
  )}`
})

const statCards = computed(() => [
  { label: '在线会话数', value: onlineSessions.value, icon: 'User', color: '#409eff' },
  { label: '我的角色数', value: roleNames.value.length, icon: 'Avatar', color: '#67c23a' },
  { label: '我的权限数', value: userStore.permissionCount, icon: 'Key', color: '#e6a23c' },
  { label: '索引文档数', value: indexStatus.value.documentCount ?? 0, icon: 'Document', color: '#f56c6c' }
])

function statusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'RUNNING') return 'primary'
  if (status === 'FAILED') return 'danger'
  return 'warning'
}

async function loadOnline() {
  onlineLoading.value = true
  try {
    const data = await getOnline()
    onlineSessions.value = data?.onlineSessions ?? 0
  } catch (e) {
    /* 已提示 */
  } finally {
    onlineLoading.value = false
  }
}

async function loadIndexOverview() {
  indexLoading.value = true
  try {
    const data = await getIndexOverview()
    overview.value = data || {}
    indexStatus.value = data?.indexStatus || { indexName: '-', exists: false, documentCount: 0, analyzer: '-' }
  } catch (e) {
    /* 已提示 */
  } finally {
    indexLoading.value = false
  }
}

onMounted(() => {
  loadOnline()
  loadIndexOverview()
})
</script>

<style scoped>
.welcome-card {
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff 0%, #2b5876 100%);
  border: none;
  margin-bottom: 16px;
}

.welcome-inner {
  display: flex;
  align-items: center;
  gap: 18px;
}

.welcome-avatar {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 24px;
}

.welcome-text {
  flex: 1;
}

.welcome-title {
  font-size: 21px;
  font-weight: 700;
  color: #fff;
}

.welcome-sub {
  color: rgba(255, 255, 255, 0.88);
  margin-top: 8px;
  font-size: 13px;
}

.ml-tag {
  margin-left: 6px;
}

.welcome-time {
  text-align: right;
  color: #fff;
}

.time-text {
  font-size: 15px;
  font-weight: 600;
}

.welcome-time .text-muted {
  color: rgba(255, 255, 255, 0.7) !important;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 8px;
}

.stat-body {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-suffix {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.stat-label {
  color: #909399;
  font-size: 13px;
  margin-top: 2px;
}

.panel-card {
  border-radius: 8px;
}

.panel-title {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
}

.doc-count {
  color: #409eff;
  font-size: 16px;
}

.online-box {
  display: flex;
  align-items: center;
  gap: 18px;
  padding: 8px 4px;
}

.online-icon {
  font-size: 46px;
  color: #409eff;
}

.online-value {
  font-size: 34px;
  font-weight: 700;
  color: #303133;
  line-height: 1.1;
}

.recent-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #606266;
}

.perm-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.perm-tag {
  font-family: Consolas, Monaco, monospace;
}
</style>
