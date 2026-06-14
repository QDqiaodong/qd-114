import request from '@/utils/request'

export const getStageList = () => {
  return request.get('/stages')
}

export const refreshStageCache = () => {
  return request.post('/stages/refresh')
}
