import request from '@/utils/request'

export const getSowingList = () => {
  return request.get('/sowings')
}

export const getSowingById = (id) => {
  return request.get(`/sowings/${id}`)
}

export const getSowingsBySeed = (seedId) => {
  return request.get(`/sowings/seed/${seedId}`)
}

export const getSowingsByVariety = (varietyId) => {
  return request.get(`/sowings/variety/${varietyId}`)
}

export const createSowing = (data) => {
  return request.post('/sowings', data)
}

export const updateSowing = (id, data) => {
  return request.put(`/sowings/${id}`, data)
}

export const deleteSowing = (id) => {
  return request.delete(`/sowings/${id}`)
}
