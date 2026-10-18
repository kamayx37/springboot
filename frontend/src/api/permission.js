import request from '@/utils/request'

/** 权限树（角色分配权限用） */
export function getPermissionTree() {
  return request({ url: '/permissions/tree', method: 'get' })
}

export function pagePermissions(params) {
  return request({ url: '/permissions', method: 'get', params })
}

export function listAllPermissions() {
  return request({ url: '/permissions/all', method: 'get' })
}

export function getPermission(id) {
  return request({ url: `/permissions/${id}`, method: 'get' })
}

export function createPermission(data) {
  return request({ url: '/permissions', method: 'post', data })
}

export function updatePermission(id, data) {
  return request({ url: `/permissions/${id}`, method: 'put', data })
}

export function deletePermission(id) {
  return request({ url: `/permissions/${id}`, method: 'delete' })
}
