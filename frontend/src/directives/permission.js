import { hasPermission } from '@/utils/permission'

/**
 * v-permission="'product:create'"  或  v-permission="['a','b']"
 * 无权限时直接把元素从 DOM 中移除
 */
const permission = {
  mounted(el, binding) {
    const value = binding.value
    if (value === undefined || value === null || value === '') return
    if (!hasPermission(value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  }
}

export default {
  install(app) {
    app.directive('permission', permission)
  }
}
