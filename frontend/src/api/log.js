import request from '@/utils/request'

export function pageLogs(params) {
  return request({ url: '/operation-logs', method: 'get', params })
}

/** 模块列表（筛选下拉） */
export function listLogModules() {
  return request({ url: '/operation-logs/modules', method: 'get' })
}

/** 清空日志 */
export function clearLogs() {
  return request({ url: '/operation-logs', method: 'delete' })
}
