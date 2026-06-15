import request from '@/utils/request'

export const getDashboardStats = () => {
  return request.get('/dashboard/stats')
}

export const getGerminationProgress = () => {
  return request.get('/dashboard/germination-progress')
}

export const getGrowthTimeline = () => {
  return request.get('/dashboard/growth-timeline')
}

export const getSeedVitalityCalendar = () => {
  return request.get('/dashboard/seed-vitality-calendar')
}
