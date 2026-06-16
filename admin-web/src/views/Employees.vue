<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q.name" placeholder="姓名" clearable style="width: 140px" @keyup.enter="search" />
      <el-input v-model="q.mobile" placeholder="手机号" clearable style="width: 150px" @keyup.enter="search" />
      <el-select v-model="q.departmentId" placeholder="部门" clearable style="width: 150px">
        <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="grow"></div>
      <el-button type="success" :icon="Download" :loading="exporting" @click="onExport">导出 Excel</el-button>
    </div>

    <el-table :data="rows" stripe v-loading="loading" border>
      <el-table-column prop="name" label="姓名" min-width="110" />
      <el-table-column prop="mobile" label="手机号" min-width="140" />
      <el-table-column prop="department" label="部门" min-width="130">
        <template #default="{ row }">{{ row.department || '未分配' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" active-text="已激活" inactive-text="待审核"
            inline-prompt @change="(v) => toggleStatus(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :icon="Switch" @click="openDept(row)">调部门</el-button>
          <el-button size="small" type="danger" :icon="Delete" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无员工" />

    <div class="pager">
      <el-pagination layout="total, sizes, prev, pager, next" :total="total" :current-page="page"
        :page-size="size" :page-sizes="[10, 20, 50]" @current-change="onPage" @size-change="onSize" />
    </div>

    <el-dialog v-model="deptDlg" title="调整部门" width="360px">
      <el-form label-width="70px">
        <el-form-item label="员工">{{ current && current.name }}</el-form-item>
        <el-form-item label="部门">
          <el-select v-model="targetDept" placeholder="选择部门" style="width: 100%">
            <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deptDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveDept">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, Delete, Switch } from '@element-plus/icons-vue'
import {
  getUserPageList,
  updateUserStatus,
  deleteUser,
  editUserDepartment,
  exportUsers,
  getDepartmentList
} from '../api/bc'
import { saveBlob } from '../utils/download'

const loading = ref(false)
const exporting = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const depts = ref([])
const q = reactive({ name: '', mobile: '', departmentId: null })

const deptDlg = ref(false)
const current = ref(null)
const targetDept = ref(null)
const saving = ref(false)

const params = computed(() => ({
  currentPage: page.value,
  pageSize: size.value,
  name: q.name || undefined,
  mobile: q.mobile || undefined,
  departmentId: q.departmentId || undefined
}))

async function load() {
  loading.value = true
  try {
    const res = await getUserPageList(params.value)
    rows.value = (res && res.data) || []
    total.value = Number(res && res.total) || 0
  } finally {
    loading.value = false
  }
}
function search() {
  page.value = 1
  load()
}
function reset() {
  q.name = ''
  q.mobile = ''
  q.departmentId = null
  search()
}
function onPage(p) {
  page.value = p
  load()
}
function onSize(s) {
  size.value = s
  page.value = 1
  load()
}

async function toggleStatus(row, val) {
  try {
    await updateUserStatus(row.id, val)
    row.status = val ? 1 : 0
    ElMessage.success(val ? '已激活' : '已设为待审核')
  } catch (e) {
    /* 已提示 */
  }
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除员工「${row.name}」？`, '提示', { type: 'warning' }).catch(
    () => Promise.reject()
  )
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

function openDept(row) {
  current.value = row
  targetDept.value = row.user_department_id || null
  deptDlg.value = true
}
async function saveDept() {
  if (!targetDept.value) {
    ElMessage.warning('请选择部门')
    return
  }
  saving.value = true
  try {
    await editUserDepartment(targetDept.value, current.value.id)
    ElMessage.success('调整成功')
    deptDlg.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function onExport() {
  exporting.value = true
  try {
    const resp = await exportUsers({
      name: q.name || undefined,
      mobile: q.mobile || undefined,
      departmentId: q.departmentId || undefined
    })
    saveBlob(resp, '员工列表.xls')
    ElMessage.success('导出成功')
  } catch (e) {
    /* 已提示 */
  } finally {
    exporting.value = false
  }
}

onMounted(async () => {
  depts.value = (await getDepartmentList()) || []
  load()
})
</script>
