import request from '@/utils/request'

export function pageRoles(params) {
  return request({ url: '/roles', method: 'get', params })
}

export function listAllRoles() {
  return request({ url: '/roles/all', method: 'get' })
}

export function getRole(id) {
  return request({ url: `/roles/${id}`, method: 'get' })
}

export function createRole(data) {
  return request({ url: '/roles', method: 'post', data })
}

export function updateRole(id, data) {
  return request({ url: `/roles/${id}`, method: 'put', data })
}

export function deleteRole(id) {
  return request({ url: `/roles/${id}`, method: 'delete' })
}

/** 分配权限 { id, ids: [permissionId] } */
export function assignPermissions(data) {
  return request({ url: '/roles/assign-permissions', method: 'post', data })
}
