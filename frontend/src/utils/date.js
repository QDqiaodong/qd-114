import dayjs from 'dayjs'

export const getCurrentLocalDateTime = () => {
  const now = dayjs()
  return now.format('YYYY-MM-DD HH:mm:ss')
}

export const formatDate = (date) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD')
}

export const formatDateTime = (date) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm')
}

export const formatDateTimeFull = (date) => {
  if (!date) return ''
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}
