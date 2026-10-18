import { useUserStore } from '@/stores/user'

/**
 * 判断当前登录用户是否拥有某个权限码
 * @param {string|string[]} code 权限码，数组表示「任意一个满足即可」
 * @returns {boolean}
 */
export function hasPermission(code) {
  if (!code) return true
  let store
  try {
    store = useUserStore()
  } catch (e) {
    return false
  }
  if (store.isSuperAdmin) return true
  const permissions = store.permissions || []
  const codes = Array.isArray(code) ? code : [code]
  if (codes.length === 0) return true
  return codes.some((item) => permissions.includes(item))
}

/** 全部满足 */
export function hasEveryPermission(codes) {
  if (!codes || codes.length === 0) return true
  const store = useUserStore()
  if (store.isSuperAdmin) return true
  const permissions = store.permissions || []
  return codes.every((item) => permissions.includes(item))
}

export function isSuperAdmin() {
  try {
    return useUserStore().isSuperAdmin
  } catch (e) {
    return false
  }
}
