<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside aside-desktop">
      <div class="logo">
        <el-icon><Bowl /></el-icon>
        <span class="logo-text">{{ auth.unitName }}</span>
      </div>
      <el-menu :default-active="active" router class="menu" background-color="#1f2d3d" text-color="#c0c4cc"
        active-text-color="#ffd04b">
        <el-menu-item v-for="r in navRoutes" :key="r.path" :index="'/' + r.path">
          <el-icon><component :is="r.meta.icon" /></el-icon>
          <span>{{ r.meta.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-drawer v-model="drawerVisible" direction="ltr" size="220px" :with-header="false" class="aside-drawer">
      <div class="aside">
        <div class="logo">
          <el-icon><Bowl /></el-icon>
          <span class="logo-text">{{ auth.unitName }}</span>
        </div>
        <el-menu :default-active="active" router class="menu" background-color="#1f2d3d" text-color="#c0c4cc"
          active-text-color="#ffd04b" @select="drawerVisible = false">
          <el-menu-item v-for="r in navRoutes" :key="r.path" :index="'/' + r.path">
            <el-icon><component :is="r.meta.icon" /></el-icon>
            <span>{{ r.meta.title }}</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="menu-toggle" @click="drawerVisible = true"><Menu /></el-icon>
          <div class="crumb">{{ currentTitle }}</div>
        </div>
        <el-dropdown @command="onCommand">
          <span class="user">
            <el-icon><Avatar /></el-icon>
            <span class="user-name">{{ auth.username }}</span>
            <el-icon><CaretBottom /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view v-slot="{ Component }">
          <keep-alive>
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'
import { adminLogout } from '../api/bc'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const drawerVisible = ref(false)

// 手机上切页面后自动收起抽屉(el-menu 的 @select 已经会关一次, 这里再兜底一次路由变化的场景)
watch(() => route.path, () => {
  drawerVisible.value = false
})

const navRoutes = computed(() => {
  const parent = router.options.routes.find((r) => r.path === '/')
  return (parent.children || []).filter((c) => c.meta && c.meta.title)
})
const active = computed(() => route.path)
const currentTitle = computed(() => (route.meta && route.meta.title) || '')

async function onCommand(cmd) {
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' }).catch(
      () => 'cancel'
    )
    try {
      await adminLogout()
    } catch (e) {
      /* 忽略 */
    }
    auth.clear()
    router.replace('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #1f2d3d;
  overflow-x: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  border-bottom: 1px solid #2a3a4d;
}
.logo-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.menu {
  border-right: none;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.menu-toggle {
  display: none;
  font-size: 20px;
  cursor: pointer;
  flex-shrink: 0;
}
.crumb {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.user {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  outline: none;
  flex-shrink: 0;
}
/* 手机端登录名太占地方, 只在宽屏显示 */
.user-name {
  display: inline;
}
.aside-drawer :deep(.el-drawer__body) {
  padding: 0;
}
.aside-drawer .aside {
  height: 100%;
}

@media (max-width: 768px) {
  .aside-desktop {
    display: none;
  }
  .menu-toggle {
    display: inline-flex;
  }
  .header {
    padding: 0 12px;
  }
  .main {
    padding: 10px;
  }
  .user-name {
    display: none;
  }
}
.main {
  padding: 18px;
}
</style>
