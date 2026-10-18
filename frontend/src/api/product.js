import request from '@/utils/request'

export function pageProducts(params) {
  return request({ url: '/products', method: 'get', params })
}

export function listBrands() {
  return request({ url: '/products/brands', method: 'get' })
}

export function getProduct(id) {
  return request({ url: `/products/${id}`, method: 'get' })
}

export function createProduct(data) {
  return request({ url: '/products', method: 'post', data })
}

export function updateProduct(id, data) {
  return request({ url: `/products/${id}`, method: 'put', data })
}

export function deleteProduct(id) {
  return request({ url: `/products/${id}`, method: 'delete' })
}

export function batchDeleteProducts(data) {
  return request({ url: '/products/batch-delete', method: 'post', data })
}
