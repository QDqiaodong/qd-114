import request from '@/utils/request'

export const getGerminationObservations = (sowingId) => {
  return request.get(`/germination-observations/sowing/${sowingId}`)
}

export const getGerminationObservationById = (id) => {
  return request.get(`/germination-observations/${id}`)
}

export const getGerminationByVariety = (varietyId) => {
  return request.get(`/germination-observations/variety/${varietyId}`)
}

export const createGerminationObservation = (data) => {
  return request.post('/germination-observations', data)
}

export const updateGerminationObservation = (id, data) => {
  return request.put(`/germination-observations/${id}`, data)
}

export const deleteGerminationObservation = (id) => {
  return request.delete(`/germination-observations/${id}`)
}
