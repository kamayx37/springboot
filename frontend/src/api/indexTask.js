import request from '@/utils/request'

/** 提交全量重建索引任务（RabbitMQ 异步执行） */
export function rebuildIndex() {
  return request({ url: '/index-tasks/rebuild', method: 'post' })
}

export function pageIndexTasks(params) {
  return request({ url: '/index-tasks', method: 'get', params })
}

/** 索引总览 { indexStatus, queue, exchange, routingKey, recentTasks } */
export function getIndexOverview() {
  return request({ url: '/index-tasks/overview', method: 'get' })
}

export function getIndexTask(taskNo) {
  return request({ url: `/index-tasks/${taskNo}`, method: 'get' })
}
