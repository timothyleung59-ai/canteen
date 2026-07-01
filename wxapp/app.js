//app.js
App({
    globalData: {
        // 后端 HTTPS 域名(结尾不带斜杠)，须加入小程序后台 request 合法域名
        web_path: "https://canteen.zsess.net",
        header: {
            Token: "",
            Cookie: "",
            'content-type': 'application/x-www-form-urlencoded'
        },
        // 小程序 appid(与后端 WX_APPID / ADMIN_APPID 一致)
        "appId": "wx3c1d1894db61d5de",
    },
    onLaunch: function () {
        // 展示本地存储能力
        var logs = wx.getStorageSync('logs') || []
        logs.unshift(Date.now())
        wx.setStorageSync('logs', logs)

        // 登录
        wx.login({
                success: res => {
                // 发送 res.code 到后台换取 openId, sessionKey, unionId
            }
    })
// 获取用户信息
wx.getSetting({
        success: res => {
        if (res.authSetting['scope.userInfo']) {
    // 已经授权，可以直接调用 getUserInfo 获取头像昵称，不会弹框
    wx.getUserInfo({
        success: res => {
        // 可以将 res 发送给后台解码出 unionId
        this.globalData.userInfo = res.userInfo

    // 由于 getUserInfo 是网络请求，可能会在 Page.onLoad 之后才返回
    // 所以此处加入 callback 以防止这种情况
    if (this.userInfoReadyCallback) {
        this.userInfoReadyCallback(res)
    }
}
})
}
}
})
}
})