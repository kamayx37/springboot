import request from '@/utils/request'

/** 登录 */
export function login(data) {
  return request({ url: '/auth/login', method: 'post', data })
}

/** 退出登录（后端会删除 Redis 中的登录态） */
export function logout() {
  return request({ url: '/auth/logout', method: 'post' })
}

/** 当前登录用户信息 */
export function getMe() {
  return request({ url: '/auth/me', method: 'get' })
}

/** 在线会话数 */
export function getOnline() {
  return request({ url: '/auth/online', method: 'get' })
}

/** 修改自己的密码 */
export function changePassword(data) {
  return request({ url: '/auth/change-password', method: 'post', data })
}
