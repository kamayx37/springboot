import request from '@/utils/request'

export function pageCategories(params) {
  return request({ url: '/categories', method: 'get', params })
}

export function listAllCategories() {
  return request({ url: '/categories/all', method: 'get' })
}

export function getCategory(id) {
  return request({ url: `/categories/${id}`, method: 'get' })
}

export function createCategory(data) {
  return request({ url: '/categories', method: 'post', data })
}

export function updateCategory(id, data) {
  return request({ url: `/categories/${id}`, method: 'put', data })
}

export function deleteCategory(id) {
  return request({ url: `/categories/${id}`, method: 'delete' })
}
