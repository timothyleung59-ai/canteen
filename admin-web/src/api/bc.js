import request from './request'
import { useAuthStore } from '../store/auth'

// 后端业务接口都是 /bc/{appid}/... 形式；appid 登录后存在 store。
function base() {
  const auth = useAuthStore()
  return `/bc/${auth.appid}`
}

/* ---------------- 登录 / 鉴权 ---------------- */
export function login(username, password) {
  return request.post('/admin/login', null, { params: { username, password } })
}
export function adminInfo() {
  return request.get('/admin/info')
}
export function adminLogout() {
  return request.post('/admin/logout')
}

/* ---------------- 部门 ---------------- */
export function getDepartmentList() {
  return request.get(`${base()}/BcUserDepartment/getBcUserDepartmentList`)
}
export function getDepartmentPageList(currentPage, pageSize) {
  return request.get(`${base()}/BcUserDepartment/getDepartmentPageList`, {
    params: { currentPage, pageSize }
  })
}
export function saveDepartment(name) {
  return request.post(`${base()}/BcUserDepartment/save`, null, { params: { name } })
}
export function updateDepartmentName(id, name) {
  return request.post(`${base()}/BcUserDepartment/updateName`, null, {
    params: { id, name }
  })
}
export function deleteDepartment(id) {
  return request.delete(`${base()}/BcUserDepartment/deleteById/${id}`)
}
export function countDinnerByDay(curIndex) {
  return request.get(`${base()}/BcUserDepartment/countDinnerByDay`, {
    params: { curIndex }
  })
}

/* ---------------- 员工 ---------------- */
export function getUserPageList(params) {
  return request.get(`${base()}/BcUser/getUserPageList`, { params })
}
export function updateUserStatus(id, isActive) {
  return request.post(`${base()}/BcUser/updateStatusById`, null, {
    params: { id, isActive }
  })
}
export function deleteUser(id) {
  return request.delete(`${base()}/BcUser/delete`, { params: { id } })
}
export function editUserDepartment(userDepartmentId, id) {
  return request.get(`${base()}/BcUser/editUserDepartmentId`, {
    params: { userDepartmentId, id }
  })
}
export function exportUsers(params) {
  return request.post(`${base()}/BcUser/export`, null, { params, responseType: 'blob' })
}

/* ---------------- 报餐明细 / 统计 ---------------- */
export function getRecordList(params) {
  return request.get(`${base()}/BcRecord/getBcRecordList`, { params })
}
export function countRecordList(params) {
  return request.get(`${base()}/BcRecord/countBcRecordPageList`, { params })
}
export function getTotalRecordByDinTime(curIndex) {
  return request.get(`${base()}/BcRecord/getTotalRecordByDinTime`, {
    params: { curIndex }
  })
}
export function exportRecords(params) {
  return request.post(`${base()}/BcRecord/export`, null, { params, responseType: 'blob' })
}
export function exportRecordCount(params) {
  return request.post(`${base()}/BcRecord/exportCount`, null, {
    params,
    responseType: 'blob'
  })
}

/* ---------------- 报餐设置 ---------------- */
export function getConfig() {
  return request.get(`${base()}/config/getConfig`)
}
export function saveConfig(config) {
  return request.post(`${base()}/config/saveOrUpdate`, null, { params: config })
}

/* ---------------- 节假日(自动同步) ---------------- */
export function getHolidays(year) {
  return request.get(`${base()}/config/holidays`, { params: { year } })
}
export function syncHolidays() {
  return request.post(`${base()}/config/syncHolidays`)
}
