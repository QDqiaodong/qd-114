import request from '@/utils/request'

export const getSeedlingPlanList = () => {
  return request.get('/seedling-plans')
}

export const getSeedlingPlanById = (id) => {
  return request.get(`/seedling-plans/${id}`)
}

export const getSeedlingPlansBySeed = (seedId) => {
  return request.get(`/seedling-plans/seed/${seedId}`)
}

export const getSeedlingPlansByVariety = (varietyId) => {
  return request.get(`/seedling-plans/variety/${varietyId}`)
}

export const getSeedlingPlansByStatus = (status) => {
  return request.get(`/seedling-plans/status/${status}`)
}

export const createSeedlingPlan = (data) => {
  return request.post('/seedling-plans', data)
}

export const updateSeedlingPlan = (id, data) => {
  return request.put(`/seedling-plans/${id}`, data)
}

export const updateSeedlingPlanStatus = (id, status, actualDate) => {
  const params = { status }
  if (actualDate) {
    params.actualDate = actualDate
  }
  return request.put(`/seedling-plans/${id}/status`, null, { params })
}

export const deleteSeedlingPlan = (id) => {
  return request.delete(`/seedling-plans/${id}`)
}
