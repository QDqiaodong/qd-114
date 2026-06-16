import request from '@/utils/request'

export const getSeedList = () => {
  return request.get('/seeds')
}

export const getSeedById = (id) => {
  return request.get(`/seeds/${id}`)
}

export const getSeedsByVariety = (varietyId) => {
  return request.get(`/seeds/variety/${varietyId}`)
}

export const createSeed = (data) => {
  return request.post('/seeds', data)
}

export const updateSeed = (id, data) => {
  return request.put(`/seeds/${id}`, data)
}

export const deleteSeed = (id) => {
  return request.delete(`/seeds/${id}`)
}

export const getSeedShelfLifeRisk = () => {
  return request.get('/seeds/shelf-life-risk')
}

export const getSeedDetail = (id) => {
  return request.get(`/seeds/${id}/detail`)
}
