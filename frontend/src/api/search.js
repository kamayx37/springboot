import request from '@/utils/request'

/**
 * 全文检索
 * body: { keyword, categoryId, brand, minPrice, maxPrice, status, page, size, sortField, sortOrder }
 */
export function searchProducts(data) {
  return request({ url: '/search/products', method: 'post', data })
}

/** 索引状态 { indexStatus:{indexName,exists,documentCount,analyzer}, pageSize } */
export function getSearchStatus() {
  return request({ url: '/search/status', method: 'get' })
}
