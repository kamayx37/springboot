<template>
  <el-container class="app-layout">
    <!-- 左侧菜单 -->
    <el-aside :width="collapsed ? '64px' : '220px'" class="app-aside">
      <div class="logo">
        <el-icon class="logo-icon"><Shop /></el-icon>
        <span v-show="!collapsed" class="logo-text">商品管理系统</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          :collapse-transition="false"
          background-color="#1f2d3d"
          text-color="#c0c4cc"
          active-text-color="#ffd04b"
          router
          unique-opened
        >
          <template v-for="item in menus" :key="item.path">
            <!-- 单子菜单：直接渲染菜单项 -->
            <el-menu-item v-if="item.children.length === 1" :index="item.children[0].path">
              <el-icon><component :is="item.children[0].icon" /></el-icon>
              <template #title>{{ item.children[0].title }}</template>
            </el-menu-item>

            <!-- 多个子菜单：渲染分组 -->
            <el-sub-menu v-else :index="item.path">
              <template #title>
                <el-icon><component :is="item.icon" /></el-icon>
                <span>{{ item.title }}</span>
              </template>
              <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
                <el-icon><component :is="child.icon" /></el-icon>
                <template #title>{{ child.title }}</template>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container>
      <!-- 顶部栏 -->
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed">
            <component :is="collapsed ? 'Expand' : 'Fold'" />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tag v-for="role in roleNames" :key="role" type="warning" effect="dark" size="small" class="role-tag">
            {{ role }}
          </el-tag>

          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-box">
              <el-avatar :size="30" class="user-avatar">{{ avatarText }}</el-avatar>
              <span class="user-name">{{ userStore.nickname }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <el-icon><User /></el-icon>&nbsp;{{ userStore.username }}
                </el-dropdown-item>
                <el-dropdown-item disabled>
                  权限数量：{{ userStore.permissionCount }}
                </el-dropdown-item>
                <el-dropdown-item divided command="changePassword">
                  <el-icon><Lock /></el-icon>&nbsp;修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>&nbsp;退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 修改密码 -->
    <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" :loading="pwdLoading" @click="submitPassword">确定</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { routes } from '@/router'
import { changePassword } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const collapsed = ref(false)

const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')

const roleNames = computed(() =>
  (userStore.userInfo?.roles || []).map((r) => (typeof r === 'string' ? r : r?.name || r?.code || '')).filter(Boolean)
)
const avatarText = computed(() => (userStore.nickname || 'U').slice(0, 1).toUpperCase())

/** 根据权限动态构建菜单 */
const menus = computed(() => {
  const list = []
  routes.forEach((top) => {
    if (!top.children || top.meta?.hidden) return
    // 顶层路由若自身带权限且不满足，直接跳过
    if (top.meta?.permission && !userStore.hasPermission(top.meta.permission)) return

    const children = []
    top.children.forEach((child) => {
      if (child.meta?.hidden) return
      if (child.meta?.permission && !userStore.hasPermission(child.meta.permission)) return
      if (!child.meta?.title) return
      children.push({
        path: top.path === '/' ? `/${child.path}` : `${top.path}/${child.path}`,
        title: child.meta.title,
        icon: child.meta.icon || top.meta?.icon || 'Menu'
      })
    })
    if (children.length === 0) return
    list.push({
      path: top.path,
      title: top.meta?.title || children[0].title,
      icon: top.meta?.icon || 'Menu',
      children
    })
  })
  return list
})

/** 刷新页面后校验登录状态是否仍然有效 */
onMounted(async () => {
  if (!userStore.token) return
  try {
    await userStore.fetchUserInfo()
  } catch (e) {
    // 拦截器已统一处理 401
  }
})

function handleCommand(command) {
  if (command === 'logout') doLogout()
  if (command === 'changePassword') pwdVisible.value = true
}

async function doLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？退出后当前 token 会立即失效。', '提示', {
      confirmButtonText: '确定退出',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await userStore.logout()
  ElMessage.success('已退出登录')
  router.replace('/login')
}

/* ---------------- 修改密码 ---------------- */
const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submitPassword() {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
      ElMessage.success('密码修改成功，请重新登录')
      pwdVisible.value = false
      await userStore.logout()
      router.replace('/login')
    } finally {
      pwdLoading.value = false
    }
  })
}
</script>

<style scoped>
.app-layout {
  height: 100vh;
}

.app-aside {
  background: #1f2d3d;
  transition: width 0.25s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  white-space: nowrap;
  background: #18232f;
}

.logo-icon {
  font-size: 22px;
  color: #ffd04b;
}

.menu-scroll {
  height: calc(100vh - 60px);
}

.app-aside :deep(.el-menu) {
  border-right: none;
}

.app-header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e8eb;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
}

.collapse-btn:hover {
  color: #409eff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-tag {
  margin-right: 2px;
}

.user-box {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  outline: none;
}

.user-box:hover {
  background: #f5f7fa;
}

.user-avatar {
  background: #409eff;
  color: #fff;
  font-size: 14px;
}

.user-name {
  font-size: 14px;
  color: #303133;
}

.app-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}

.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.2s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-12px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(12px);
}
</style>
