import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/Dashboard.vue'),
    meta: { title: '首页概览' }
  },
  {
    path: '/seeds',
    name: 'Seeds',
    component: () => import('@/views/SeedList.vue'),
    meta: { title: '种子管理' }
  },
  {
    path: '/sowings',
    name: 'Sowings',
    component: () => import('@/views/SowingList.vue'),
    meta: { title: '播种记录' }
  },
  {
    path: '/growth',
    name: 'Growth',
    component: () => import('@/views/GrowthTracking.vue'),
    meta: { title: '生长跟踪' }
  },
  {
    path: '/transplants',
    name: 'Transplants',
    component: () => import('@/views/TransplantList.vue'),
    meta: { title: '移栽分盆' }
  },
  {
    path: '/recovery',
    name: 'Recovery',
    component: () => import('@/views/TransplantRecovery.vue'),
    meta: { title: '移栽恢复观察板' }
  },
  {
    path: '/card-wall',
    name: 'CardWall',
    component: () => import('@/views/VarietyCardWall.vue'),
    meta: { title: '品种培育卡片墙' }
  },
  {
    path: '/germination',
    name: 'Germination',
    component: () => import('@/views/GerminationObservation.vue'),
    meta: { title: '发芽观察' }
  },
  {
    path: '/seedling-plan',
    name: 'SeedlingPlan',
    component: () => import('@/views/SeedlingPlanList.vue'),
    meta: { title: '育苗计划表' }
  },
  {
    path: '/variety-detail',
    name: 'VarietyDetail',
    component: () => import('@/views/VarietyDetail.vue'),
    meta: { title: '品种详情复盘' }
  },
  {
    path: '/transplant-readiness',
    name: 'TransplantReadiness',
    component: () => import('@/views/TransplantReadiness.vue'),
    meta: { title: '移栽就绪评估' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  }
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || '花卉培育'} - 花卉种子培育跟踪表`
  next()
})

export default router
