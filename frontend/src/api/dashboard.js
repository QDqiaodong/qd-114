import request from '@/utils/request'

export const getDashboardStats = () => {
  return request.get('/dashboard/stats')
}

export const getGerminationProgress = () => {
  return request.get('/dashboard/germination-progress')
}
