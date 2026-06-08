import axios from 'axios'
import { handleError } from '@/utils/errorCode'

function getCsrfToken() {
  const name = 'XSRF-TOKEN='
  const decodedCookie = decodeURIComponent(document.cookie)
  const cookieArray = decodedCookie.split(';')
  for (let i = 0; i < cookieArray.length; i++) {
    let cookie = cookieArray[i]
    while (cookie.charAt(0) === ' ') {
      cookie = cookie.substring(1)
    }
    if (cookie.indexOf(name) === 0) {
      return cookie.substring(name.length, cookie.length)
    }
  }
  return null
}

const service = axios.create({
  baseURL: '/api',
  timeout: 10000,
  withCredentials: true
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }

    const csrfToken = getCsrfToken()
    if (csrfToken) {
      config.headers['X-XSRF-TOKEN'] = csrfToken
    }

    return config
  },
  error => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data

    if (res.errorCode) {
      handleError(res.errorCode, res.errorMessage)
      const error = new Error(res.errorMessage || '操作失败')
      error.errorCode = res.errorCode
      error.errorResponse = res
      return Promise.reject(error)
    }

    if (res.code !== undefined && res.code !== null) {
      if (res.code === 200) {
        return res.data
      }
      handleError(null, res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }

    return res
  },
  error => {
    if (error.response) {
      const data = error.response.data
      if (data && data.errorCode) {
        handleError(data.errorCode, data.errorMessage)
        const err = new Error(data.errorMessage || '操作失败')
        err.errorCode = data.errorCode
        err.errorResponse = data
        return Promise.reject(err)
      }

      const status = error.response.status
      const statusMessages = {
        400: '请求参数错误',
        401: '未授权，请登录',
        403: '没有权限访问此资源',
        404: '请求的资源不存在',
        408: '请求超时',
        500: '服务器内部错误',
        502: '网关错误',
        503: '服务不可用',
        504: '网关超时',
      }
      const message = data?.errorMessage || statusMessages[status] || `请求失败 (${status})`
      handleError(null, message)
    } else if (error.code === 'ECONNABORTED') {
      handleError(null, '请求超时，请稍后重试')
    } else if (error.message && error.message.includes('Network Error')) {
      handleError(null, '网络错误，请检查网络连接')
    } else {
      handleError(null, error.message || '操作失败')
    }
    return Promise.reject(error)
  }
)

export default service
