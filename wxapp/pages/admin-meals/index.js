const app = getApp();
const WEEK_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

Page({
    data: {
        dateList: [],
        curIndex: 0,
        list: [],
        loading: false,
        emptyText: '暂无员工'
    },
    onLoad: function () {
        this.buildDateList();
        this.fetchList(0);
    },
    /**
     * 生成"今天+未来7天"日期条(共8个)
     */
    buildDateList: function () {
        const today = new Date();
        const dateList = [];
        for (let i = 0; i < 8; i++) {
            const d = new Date(today.getFullYear(), today.getMonth(), today.getDate() + i);
            const mm = (d.getMonth() + 1 < 10 ? '0' : '') + (d.getMonth() + 1);
            const dd = (d.getDate() < 10 ? '0' : '') + d.getDate();
            dateList.push({
                curIndex: i,
                day: dd,
                month: mm,
                week: i === 0 ? '今天' : (i === 1 ? '明天' : WEEK_NAMES[d.getDay()])
            });
        }
        this.setData({ dateList: dateList });
    },
    selectDate: function (e) {
        const curIndex = e.currentTarget.dataset.index;
        if (curIndex === this.data.curIndex) {
            return;
        }
        this.setData({ curIndex: curIndex });
        this.fetchList(curIndex);
    },
    fetchList: function (curIndex) {
        const that = this;
        const Token = wx.getStorageSync('Token');
        if (!Token) {
            wx.showToast({ title: '请先在"我的"完成注册', icon: 'none' });
            return;
        }
        app.globalData.header.Token = Token;
        that.setData({ loading: true, list: [] });
        wx.request({
            url: app.globalData.web_path + '/bc/' + app.globalData.appId + '/BcRecord/getBcRecordListByDinTime',
            data: {
                curIndex: curIndex,
                currentPage: 1,
                pageSize: 200
            },
            header: app.globalData.header,
            success: function (res) {
                if (res.data && res.data.code === 0) {
                    that.setData({ list: res.data.data || [] });
                } else {
                    that.setData({ list: [] });
                    wx.showToast({
                        title: (res.data && res.data.msg) || '查询失败',
                        icon: 'none'
                    });
                }
            },
            fail: function () {
                wx.showToast({ title: '网络异常', icon: 'none' });
            },
            complete: function () {
                that.setData({ loading: false });
            }
        });
    },
    /**
     * 标记已就餐(未报餐者后端会自动补一条报餐记录)
     */
    markEaten: function (e) {
        const that = this;
        const index = e.currentTarget.dataset.index;
        const item = that.data.list[index];
        if (item.hadEat == 1) {
            return;
        }
        const Token = wx.getStorageSync('Token');
        if (!Token) {
            wx.showToast({ title: '请先在"我的"完成注册', icon: 'none' });
            return;
        }
        app.globalData.header.Token = Token;
        const tip = item.reported == 1 ? ('确认 ' + item.name + ' 已就餐?') : (item.name + ' 未报餐, 确认其已就餐并补报?');
        wx.showModal({
            title: '确认就餐',
            content: tip,
            confirmText: '确认',
            confirmColor: '#ff7e00',
            success(res) {
                if (res.confirm) {
                    wx.request({
                        url: app.globalData.web_path + '/bc/' + app.globalData.appId + '/BcRecord/confirmEatByUser',
                        data: { userId: item.userId, curIndex: that.data.curIndex },
                        header: app.globalData.header,
                        success: function (res) {
                            if (res.data && res.data.code === 0 && res.data.data === 1) {
                                const list = that.data.list;
                                list[index].hadEat = 1;
                                list[index].reported = 1;
                                that.setData({ list: list });
                            } else {
                                wx.showToast({
                                    title: (res.data && res.data.msg) || '操作失败',
                                    icon: 'none'
                                });
                            }
                        },
                        fail: function () {
                            wx.showToast({ title: '网络异常', icon: 'none' });
                        }
                    });
                }
            }
        });
    }
});
