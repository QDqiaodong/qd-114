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

export const getAnomaliesBySowing = (sowingId) => {
  return request.get(`/germination-anomalies/sowing/${sowingId}`)
}

export const getAnomalyById = (id) => {
  return request.get(`/germination-anomalies/${id}`)
}

export const getAnomalyByObservation = (observationId) => {
  return request.get(`/germination-anomalies/observation/${observationId}`)
}

export const getAnomaliesByStatus = (status) => {
  return request.get(`/germination-anomalies/status/${status}`)
}

export const getAnomalyCountByStatus = (status) => {
  return request.get(`/germination-anomalies/count/${status}`)
}

export const processAnomaly = (id, data) => {
  return request.put(`/germination-anomalies/${id}/process`, data)
}

export const resolveAnomaly = (id, data) => {
  return request.put(`/germination-anomalies/${id}/resolve`, data)
}

export const closeAnomaly = (id) => {
  return request.put(`/germination-anomalies/${id}/close`)
}

export const deleteAnomaly = (id) => {
  return request.delete(`/germination-anomalies/${id}`)
}
