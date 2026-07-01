import { ref, onMounted, onUnmounted } from 'vue'

// 窄屏(手机)判定; 各列表页用它在"表格"和"卡片列表"两种展示之间切换
export function useIsMobile(breakpoint = 768) {
  const isMobile = ref(window.innerWidth <= breakpoint)
  function onResize() {
    isMobile.value = window.innerWidth <= breakpoint
  }
  onMounted(() => window.addEventListener('resize', onResize))
  onUnmounted(() => window.removeEventListener('resize', onResize))
  return { isMobile }
}
