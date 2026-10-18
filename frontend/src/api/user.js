import request from '@/utils/request'

export function pageUsers(params) {
  return request({ url: '/users', method: 'get', params })
}

export function listAllUsers() {
  return request({ url: '/users/all', method: 'get' })
}

export function getUser(id) {
  return request({ url: `/users/${id}`, method: 'get' })
}

export function createUser(data) {
  return request({ url: '/users', method: 'post', data })
}

export function updateUser(id, data) {
  return request({ url: `/users/${id}`, method: 'put', data })
}

export function deleteUser(id) {
  return request({ url: `/users/${id}`, method: 'delete' })
}

/** 分配角色 { id, ids: [roleId] } */
export function assignRoles(data) {
  return request({ url: '/users/assign-roles', method: 'post', data })
}

/** 重置密码 { newPassword } */
export function resetUserPassword(id, data) {
  return request({ url: `/users/${id}/reset-password`, method: 'post', data })
}
