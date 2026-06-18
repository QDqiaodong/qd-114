import request from '@/utils/request'

export const getGrowthTrackings = (sowingId) => {
  return request.get(`/growth-trackings/sowing/${sowingId}`)
}

export const getGrowthTrackingById = (id) => {
  return request.get(`/growth-trackings/${id}`)
}

export const createGrowthTracking = (data) => {
  return request.post('/growth-trackings', data)
}

export const updateGrowthTracking = (id, data) => {
  return request.put(`/growth-trackings/${id}`, data)
}

export const deleteGrowthTracking = (id) => {
  return request.delete(`/growth-trackings/${id}`)
}

export const getHealthAggregation = () => {
  return request.get('/growth-trackings/health/aggregation')
}

export const getBatchHealth = (sowingId) => {
  return request.get(`/growth-trackings/health/batch/${sowingId}`)
}

export const getVarietyHealth = (varietyId) => {
  return request.get(`/growth-trackings/health/variety/${varietyId}`)
}

export const getBatchHealthByVariety = (varietyId) => {
  return request.get(`/growth-trackings/health/batch/by-variety/${varietyId}`)
}

export const getTransplantReadiness = (sowingId) => {
  return request.get(`/growth-trackings/transplant-readiness/${sowingId}`)
}

export const getAllTransplantReadiness = () => {
  return request.get('/growth-trackings/transplant-readiness')
}

export const getBatchTransplantReadiness = (sowingIds) => {
  return request.post('/growth-trackings/transplant-readiness/batch', sowingIds)
}
