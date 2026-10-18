import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

export const TOKEN_KEY = 'pm_token'
export const USER_KEY = 'pm_user'

const service = axios.create({
  baseURL: '/api',
  timeout: 20000
})

// 请求拦截器：统一携带 Authorization
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

let redirecting = false

function toLogin() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  if (redirecting) return
  redirecting = true
  const current = router.currentRoute.value
  const redirect = current && current.path !== '/login' ? current.fullPath : undefined
  router
    .replace({ path: '/login', query: redirect ? { redirect } : {} })
    .catch(() => {})
    .finally(() => {
      setTimeout(() => {
        redirecting = false
      }, 800)
    })
}

// 响应拦截器：HTTP 恒为 200，业务状态在 code 里
service.interceptors.response.use(
  (response) => {
    const res = response.data
    // 非标准结构（例如文件流）直接返回
    if (res === null || res === undefined || typeof res !== 'object' || !('code' in res)) {
      return res
    }
    if (res.code === 200) {
      return res.data
    }
    if (res.code === 401) {
      ElMessage.error(res.message || '登录已失效，请重新登录')
      toLogin()
      return Promise.reject(new Error(res.message || '登录已失效'))
    }
    if (res.code === 403) {
      ElMessage.error(res.message || '权限不足，无法执行该操作')
      return Promise.reject(new Error(res.message || '权限不足'))
    }
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      ElMessage.error('登录已失效，请重新登录')
      toLogin()
    } else if (status === 403) {
      ElMessage.error('权限不足，无法执行该操作')
    } else {
      ElMessage.error(error?.message ? `网络请求失败：${error.message}` : '网络请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
