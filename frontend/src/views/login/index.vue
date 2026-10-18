<template>
  <div class="login-page">
    <div class="login-bg"></div>

    <div class="login-content">
      <!-- 左侧介绍 -->
      <div class="login-intro">
        <h1 class="intro-title">
          <el-icon class="intro-icon"><Shop /></el-icon>
          商品管理系统
        </h1>
        <p class="intro-sub">Vue 3 + Vite 5 + Element Plus + Pinia + Vue Router</p>
        <ul class="intro-list">
          <li><el-icon><Select /></el-icon> 商品 / 分类 全生命周期管理</li>
          <li><el-icon><Select /></el-icon> Elasticsearch 全文检索 + 关键词高亮</li>
          <li><el-icon><Select /></el-icon> RBAC 用户·角色·权限三级管控</li>
          <li><el-icon><Select /></el-icon> RabbitMQ 异步索引重建任务</li>
          <li><el-icon><Select /></el-icon> 操作日志审计与在线会话统计</li>
        </ul>
      </div>

      <!-- 右侧登录表单 -->
      <div class="login-panel">
        <el-card class="login-card" shadow="always">
          <div class="card-title">用户登录</div>
          <div class="card-sub">请输入账号密码进入系统</div>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" clearable :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                type="password"
                placeholder="请输入密码"
                show-password
                :prefix-icon="Lock"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
                {{ loading ? '登录中...' : '登 录' }}
              </el-button>
            </el-form-item>
          </el-form>

          <!-- 演示账号提示卡片 -->
          <el-divider content-position="center">演示账号（点击自动填充）</el-divider>
          <div class="demo-accounts">
            <div
              v-for="account in demoAccounts"
              :key="account.username"
              class="demo-item"
              :class="{ active: form.username === account.username }"
              @click="fillAccount(account)"
            >
              <div class="demo-head">
                <el-icon class="demo-avatar"><component :is="account.icon" /></el-icon>
                <span class="demo-role">{{ account.role }}</span>
              </div>
              <div class="demo-cred">{{ account.username }} / {{ account.password }}</div>
              <div class="demo-desc">{{ account.desc }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 32, message: '用户名长度为 2 - 32 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 5, message: '密码至少 5 位', trigger: 'blur' }
  ]
}

const demoAccounts = [
  {
    username: 'admin',
    password: 'admin123',
    role: '超级管理员',
    desc: '拥有全部菜单与操作权限，可管理用户 / 角色 / 权限',
    icon: 'Star'
  },
  {
    username: 'product',
    password: 'product123',
    role: '商品管理员',
    desc: '可维护商品、分类，并使用全文检索与重建索引',
    icon: 'Goods'
  },
  {
    username: 'viewer',
    password: 'viewer123',
    role: '只读访客',
    desc: '仅可查看数据，所有新增 / 编辑 / 删除按钮均隐藏',
    icon: 'View'
  }
]

function fillAccount(account) {
  form.username = account.username
  form.password = account.password
  if (formRef.value) formRef.value.clearValidate()
  ElMessage.success(`已填充演示账号：${account.username}`)
}

async function handleLogin() {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await userStore.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功，欢迎回来！')
      const redirect = route.query.redirect
      router.replace(typeof redirect === 'string' && redirect ? redirect : '/dashboard')
    } catch (e) {
      // 错误提示已在 axios 拦截器中统一处理
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #1f2d3d 0%, #2b5876 50%, #4e4376 100%);
}

.login-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(255, 255, 255, 0.14) 1px, transparent 1px);
  background-size: 22px 22px;
}

.login-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 60px;
  width: 1060px;
  max-width: 94vw;
}

.login-intro {
  flex: 1;
  color: #fff;
}

.intro-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 34px;
  margin: 0 0 12px;
  letter-spacing: 2px;
}

.intro-icon {
  font-size: 36px;
  color: #ffd04b;
}

.intro-sub {
  color: rgba(255, 255, 255, 0.78);
  margin: 0 0 24px;
  font-size: 14px;
  letter-spacing: 1px;
}

.intro-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.intro-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 0;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.intro-list .el-icon {
  color: #67c23a;
}

.login-panel {
  width: 420px;
}

.login-card {
  border-radius: 10px;
  padding: 6px 4px;
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  text-align: center;
}

.card-sub {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin: 6px 0 22px;
}

.login-btn {
  width: 100%;
  letter-spacing: 4px;
}

.demo-accounts {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.demo-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 8px 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fafafa;
}

.demo-item:hover {
  border-color: #409eff;
  background: #ecf5ff;
  transform: translateX(2px);
}

.demo-item.active {
  border-color: #409eff;
  background: #ecf5ff;
  box-shadow: 0 0 0 1px #409eff inset;
}

.demo-head {
  display: flex;
  align-items: center;
  gap: 6px;
}

.demo-avatar {
  color: #409eff;
}

.demo-role {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
}

.demo-cred {
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #d4380d;
  margin: 3px 0 2px;
}

.demo-desc {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

@media (max-width: 900px) {
  .login-intro {
    display: none;
  }
  .login-panel {
    width: 100%;
  }
}
</style>
