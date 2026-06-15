import request from '@/utils/request'

export const getTransplantList = () => {
  return request.get('/transplants')
}

export const getTransplantById = (id) => {
  return request.get(`/transplants/${id}`)
}

export const getTransplantDetail = (id) => {
  return request.get(`/transplants/${id}/detail`)
}

export const getTransplantsBySowing = (sowingId) => {
  return request.get(`/transplants/sowing/${sowingId}`)
}

export const createTransplant = (data) => {
  return request.post('/transplants', data)
}

export const updateTransplant = (id, data) => {
  return request.put(`/transplants/${id}`, data)
}

export const deleteTransplant = (id) => {
  return request.delete(`/transplants/${id}`)
}
