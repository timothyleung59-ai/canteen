import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'
import router from '../router'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000
})

// 请求拦截：附带管理员登录态
service.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers['Admin-Token'] = auth.token
  }
  return config
})

// 响应拦截：统一解包 ActionResult { code, msg, data }
service.interceptors.response.use(
  (resp) => {
    // 文件下载等二进制响应直接返回
    if (resp.config.responseType === 'blob') {
      return resp
    }
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code === 0 || body.code == null) {
        return body.data
      }
      if (body.code === 401) {
        handleUnauthorized(body.msg)
        return Promise.reject(new Error(body.msg || '未登录'))
      }
      ElMessage.error(body.msg || '操作失败')
      return Promise.reject(new Error(body.msg || '操作失败'))
    }
    return body
  },
  (error) => {
    const status = error.response && error.response.status
    if (status === 401) {
      handleUnauthorized()
    } else {
      ElMessage.error(
        (error.response && error.response.data && error.response.data.msg) ||
          error.message ||
          '网络异常'
      )
    }
    return Promise.reject(error)
  }
)

let redirecting = false
function handleUnauthorized(msg) {
  const auth = useAuthStore()
  auth.clear()
  if (!redirecting) {
    redirecting = true
    ElMessage.warning(msg || '登录已过期，请重新登录')
    router.replace('/login').finally(() => {
      redirecting = false
    })
  }
}

export default service
