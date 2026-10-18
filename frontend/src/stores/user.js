import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getMe } from '@/api/auth'
import { TOKEN_KEY, USER_KEY } from '@/utils/request'

function readUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch (e) {
    return null
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    userInfo: readUser(),
    permissions: readUser()?.permissions || [],
    roles: readUser()?.roles || []
  }),

  getters: {
    isLogin: (state) => !!state.token,
    nickname: (state) => state.userInfo?.nickname || state.userInfo?.username || '未登录',
    username: (state) => state.userInfo?.username || '',
    email: (state) => state.userInfo?.email || '',
    roleNames: (state) => state.userInfo?.roles || [],
    permissionCount: (state) => (state.permissions || []).length,
    /** 超级管理员角色码 */
    isSuperAdmin: (state) => (state.userInfo?.roles || []).includes('SUPER_ADMIN')
  },

  actions: {
    setToken(token) {
      this.token = token || ''
      if (token) {
        localStorage.setItem(TOKEN_KEY, token)
      } else {
        localStorage.removeItem(TOKEN_KEY)
      }
    },

    setUserInfo(userInfo) {
      this.userInfo = userInfo || null
      this.permissions = userInfo?.permissions || []
      this.roles = userInfo?.roles || []
      if (userInfo) {
        localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
      } else {
        localStorage.removeItem(USER_KEY)
      }
    },

    /** 登录：保存 token + 用户信息 */
    async login(form) {
      const data = await loginApi(form)
      this.setToken(data?.token)
      this.setUserInfo(data?.userInfo)
      return data
    },

    /** 重新拉取当前用户信息（刷新页面后校验 token 是否仍有效） */
    async fetchUserInfo() {
      const data = await getMe()
      this.setUserInfo(data)
      return data
    },

    hasPermission(code) {
      if (!code) return true
      if (this.isSuperAdmin) return true
      const codes = Array.isArray(code) ? code : [code]
      return codes.some((item) => (this.permissions || []).includes(item))
    },

    clearLocal() {
      this.token = ''
      this.userInfo = null
      this.permissions = []
      this.roles = []
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },

    /** 退出登录：必须调用后端接口使 Redis 中的登录态失效 */
    async logout() {
      try {
        await logoutApi()
      } catch (e) {
        // 即使后端调用失败，也要清理本地状态
        console.warn('[logout] 后端退出接口调用失败：', e?.message)
      } finally {
        this.clearLocal()
      }
    }
  }
})
