import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        meta: { title: '概览', icon: 'Odometer' },
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'records',
        name: 'records',
        meta: { title: '报餐明细', icon: 'Tickets' },
        component: () => import('../views/Records.vue')
      },
      {
        path: 'stats',
        name: 'stats',
        meta: { title: '报餐统计', icon: 'DataLine' },
        component: () => import('../views/Stats.vue')
      },
      {
        path: 'employees',
        name: 'employees',
        meta: { title: '员工管理', icon: 'User' },
        component: () => import('../views/Employees.vue')
      },
      {
        path: 'departments',
        name: 'departments',
        meta: { title: '部门管理', icon: 'OfficeBuilding' },
        component: () => import('../views/Departments.vue')
      },
      {
        path: 'config',
        name: 'config',
        meta: { title: '报餐设置', icon: 'Setting' },
        component: () => import('../views/Config.vue')
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.isLogged) {
    return '/login'
  }
  if (to.path === '/login' && auth.isLogged) {
    return '/'
  }
  return true
})

export default router
