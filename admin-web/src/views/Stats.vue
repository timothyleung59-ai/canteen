<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q.name" placeholder="姓名" clearable style="width: 140px" @keyup.enter="search" />
      <el-input v-model="q.mobile" placeholder="手机号" clearable style="width: 150px" @keyup.enter="search" />
      <el-select v-model="q.departmentId" placeholder="部门" clearable style="width: 150px">
        <el-option v-for="d in depts" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
      <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期"
        end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width: 240px" />
      <el-button type="primary" :icon="Search" @click="search">查询</el-button>
      <el-button :icon="Refresh" @click="reset">重置</el-button>
      <div class="grow"></div>
      <el-button type="success" :icon="Download" :loading="exporting" @click="onExport">导出 Excel</el-button>
    </div>

    <el-alert v-if="total" :closable="false" type="info" show-icon style="margin-bottom: 12px"
      :title="`共 ${total} 名员工有报餐记录`" />

    <el-table :data="rows" stripe v-loading="loading" border>
      <el-table-column type="index" label="#" width="60" />
      <el-table-column prop="deptName" label="部门" min-width="140" />
      <el-table-column prop="name" label="姓名" min-width="120" />
      <el-table-column prop="mobile" label="手机号" min-width="140" />
      <el-table-column label="报餐次数" min-width="120">
        <template #default="{ row }">
          <el-tag type="primary" effect="dark">{{ row.num }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && !rows.length" description="暂无统计数据" />

    <div class="pager">
      <el-pagination layout="total, sizes, prev, pager, next" :total="total" :current-page="page"
        :page-size="size" :page-sizes="[10, 20, 50]" @current-change="onPage" @size-change="onSize" />
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download } from '@element-plus/icons-vue'
import { countRecordList, exportRecordCount, getDepartmentList } from '../api/bc'
import { saveBlob } from '../utils/download'

const loading = ref(false)
const exporting = ref(false)
const rows = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const depts = ref([])
const dateRange = ref([])
const q = reactive({ name: '', mobile: '', departmentId: null })

const params = computed(() => {
  const p = {
    currentPage: page.value,
    pageSize: size.value,
    name: q.name || undefined,
    mobile: q.mobile || undefined,
    departmentId: q.departmentId || undefined
  }
  if (dateRange.value && dateRange.value.length === 2) {
    p.startTime = dateRange.value[0] + ' 00:00:00'
    p.endTime = dateRange.value[1] + ' 23:59:59'
  }
  return p
})

async function load() {
  loading.value = true
  try {
    const res = await countRecordList(params.value)
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
  dateRange.value = []
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
async function onExport() {
  exporting.value = true
  try {
    const resp = await exportRecordCount({
      name: q.name || undefined,
      mobile: q.mobile || undefined,
      departmentId: q.departmentId || undefined,
      startTime: params.value.startTime,
      endTime: params.value.endTime
    })
    saveBlob(resp, '报餐统计.xls')
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
