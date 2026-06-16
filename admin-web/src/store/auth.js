import { defineStore } from 'pinia'

const LS_KEY = 'canteen_admin_auth'

function load() {
  try {
    return JSON.parse(localStorage.getItem(LS_KEY)) || {}
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const saved = load()
    return {
      token: saved.token || '',
      appid: saved.appid || '',
      username: saved.username || '',
      unitName: saved.unitName || '饭堂报餐管理后台'
    }
  },
  getters: {
    isLogged: (s) => !!s.token
  },
  actions: {
    setSession({ token, appid, username, unitName }) {
      this.token = token
      this.appid = appid
      this.username = username
      this.unitName = unitName || this.unitName
      localStorage.setItem(
        LS_KEY,
        JSON.stringify({ token, appid, username, unitName: this.unitName })
      )
    },
    clear() {
      this.token = ''
      this.appid = ''
      this.username = ''
      localStorage.removeItem(LS_KEY)
    }
  }
})
