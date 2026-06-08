import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'
import { isAuthError, isForbidden, getMessage, ResultCode } from '@/utils/resultCode'

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

function handleAuthError(code, message) {
  const defaultMessage = '登录已过期，请重新登录'
  const errorMessage = message || getMessage(code) || defaultMessage
  
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  
  ElMessageBox.confirm(
    errorMessage,
    '提示',
    {
      confirmButtonText: '重新登录',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    router.push('/login')
  }).catch(() => {
    router.push('/login')
  })
}

function handleForbiddenError(code, message) {
  const defaultMessage = '没有权限访问此资源'
  const errorMessage = message || getMessage(code) || defaultMessage
  ElMessage.error(errorMessage)
}

function handleBusinessError(code, message) {
  const errorMessage = message || getMessage(code) || '操作失败'
  ElMessage.error(errorMessage)
}

service.interceptors.response.use(
  response => {
    if (response.config.responseType === 'blob') {
      return response.data
    }

    const res = response.data
    
    if (res.code === undefined || res.code === null) {
      return res
    }
    
    if (res.code === ResultCode.SUCCESS) {
      return res.data
    }
    
    if (isAuthError(res.code)) {
      handleAuthError(res.code, res.message)
      return Promise.reject(new Error(res.message || getMessage(res.code)))
    }
    
    if (isForbidden(res.code)) {
      handleForbiddenError(res.code, res.message)
      return Promise.reject(new Error(res.message || getMessage(res.code)))
    }
    
    handleBusinessError(res.code, res.message)
    return Promise.reject(new Error(res.message || getMessage(res.code)))
  },
  error => {
    let message = error.message
    
    if (error.response) {
      const status = error.response.status
      
      switch (status) {
        case 400:
          message = '请求参数错误'
          break
        case 401:
          handleAuthError(ResultCode.UNAUTHORIZED, error.response.data?.message)
          return Promise.reject(error)
        case 403:
          message = '没有权限访问此资源'
          break
        case 404:
          message = '请求的资源不存在'
          break
        case 408:
          message = '请求超时'
          break
        case 500:
          message = '服务器内部错误'
          break
        case 502:
          message = '网关错误'
          break
        case 503:
          message = '服务不可用'
          break
        case 504:
          message = '网关超时'
          break
        default:
          message = `请求失败 (${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (error.message.includes('Network Error')) {
      message = '网络错误，请检查网络连接'
    }
    
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

export default service
