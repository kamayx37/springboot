import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

import Layout from '@/layout/index.vue'

/**
 * 路由表
 * meta.title      菜单/页签标题
 * meta.icon       Element Plus 图标组件名
 * meta.permission 需要的权限码（用于菜单显示与访问控制）
 * meta.hidden     是否在左侧菜单中隐藏
 */
export const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页仪表盘', icon: 'Odometer', public: true }
      }
    ]
  },
  {
    path: '/goods',
    component: Layout,
    redirect: '/goods/products',
    meta: { title: '商品中心', icon: 'Goods' },
    children: [
      {
        path: 'products',
        name: 'Products',
        component: () => import('@/views/goods/products.vue'),
        meta: { title: '商品管理', icon: 'ShoppingCart', permission: 'product:view' }
      },
      {
        path: 'categories',
        name: 'Categories',
        component: () => import('@/views/goods/categories.vue'),
        meta: { title: '分类管理', icon: 'Menu', permission: 'category:view' }
      }
    ]
  },
  {
    path: '/search',
    name: 'Search',
    component: Layout,
    redirect: '/search/index',
    meta: { title: '全文检索', icon: 'Search', permission: 'search:view' },
    children: [
      {
        path: 'index',
        name: 'SearchIndex',
        component: () => import('@/views/search/index.vue'),
        meta: { title: '商品全文检索', icon: 'Search', permission: 'search:view' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/users',
    meta: { title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/system/users.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'user:view' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/system/roles.vue'),
        meta: { title: '角色管理', icon: 'Avatar', permission: 'role:view' }
      },
      {
        path: 'permissions',
        name: 'Permissions',
        component: () => import('@/views/system/permissions.vue'),
        meta: { title: '权限管理', icon: 'Key', permission: 'permission:view' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/system/logs.vue'),
        meta: { title: '操作日志', icon: 'Document', permission: 'log:view' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在', hidden: true }
  }
]

export function firstAccessiblePath(hasPermission) {
  const found = flatRoutes().find((r) => r.meta?.public || (r.meta?.permission && hasPermission(r.meta.permission)))
  return found ? found.path : '/dashboard'
}

export function flatRoutes() {
  const result = []
  const walk = (list, prefix) => {
    list.forEach((route) => {
      const full = route.path.startsWith('/') ? route.path : `${prefix}/${route.path}`.replace(/\/+/g, '/')
      if (route.component && route.meta?.title && !route.meta?.hidden) {
        result.push({ ...route, path: full })
      }
      if (route.children) walk(route.children, full === '/' ? '' : full)
    })
  }
  walk(routes, '')
  return result
}

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

const WHITE_LIST = ['/login']

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const token = userStore.token

  // 未登录：白名单直接放行，其余跳登录页
  if (!token) {
    if (WHITE_LIST.includes(to.path)) {
      next()
    } else {
      next({ path: '/login', query: to.fullPath !== '/' ? { redirect: to.fullPath } : {} })
    }
    return
  }

  // 已登录访问登录页 → 首页
  if (to.path === '/login') {
    next({ path: '/dashboard' })
    return
  }

  // 页面权限校验
  const permission = to.meta?.permission
  if (permission && !userStore.hasPermission(permission)) {
    ElMessage.error(`无权访问「${to.meta?.title || to.path}」，请联系管理员分配权限`)
    next(from.path && from.path !== to.path ? false : '/dashboard')
    return
  }

  next()
})

router.afterEach((to) => {
  const base = '商品管理系统'
  document.title = to.meta?.title ? `${to.meta.title} - ${base}` : base
})

export default router
