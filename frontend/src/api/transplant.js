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

export const checkTransplantEligibility = (sowingId) => {
  return request.get(`/transplants/eligibility/${sowingId}`)
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

export const getRecoveryBoard = () => {
  return request.get('/transplants/recovery-board')
}

export const getRecoveryRecords = (transplantId) => {
  return request.get(`/transplant-recovery-records/transplant/${transplantId}`)
}

export const getRecoveryRecordsDesc = (transplantId) => {
  return request.get(`/transplant-recovery-records/transplant/${transplantId}/desc`)
}

export const getRecoveryRecordsBySowing = (sowingId) => {
  return request.get(`/transplant-recovery-records/sowing/${sowingId}`)
}

export const getRecoveryRecordById = (id) => {
  return request.get(`/transplant-recovery-records/${id}`)
}

export const getLatestRecoveryRecord = (transplantId) => {
  return request.get(`/transplant-recovery-records/transplant/${transplantId}/latest`)
}

export const getRecoveryDetail = (transplantId) => {
  return request.get(`/transplant-recovery-records/transplant/${transplantId}/detail`)
}

export const createRecoveryRecord = (data) => {
  return request.post('/transplant-recovery-records', data)
}

export const updateRecoveryRecord = (id, data) => {
  return request.put(`/transplant-recovery-records/${id}`, data)
}

export const deleteRecoveryRecord = (id) => {
  return request.delete(`/transplant-recovery-records/${id}`)
}
