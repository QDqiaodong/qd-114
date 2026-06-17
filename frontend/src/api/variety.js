import request from '@/utils/request'

export const getVarietyList = () => {
  return request.get('/varieties')
}

export const getVarietyById = (id) => {
  return request.get(`/varieties/${id}`)
}

export const createVariety = (data) => {
  return request.post('/varieties', data)
}

export const updateVariety = (id, data) => {
  return request.put(`/varieties/${id}`, data)
}

export const deleteVariety = (id) => {
  return request.delete(`/varieties/${id}`)
}

export const getVarietyCardWall = () => {
  return request.get('/varieties/card-wall')
}

export const getVarietyReview = (id) => {
  return request.get(`/varieties/${id}/review`)
}
