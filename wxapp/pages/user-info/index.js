// pages/user-info/index
import WxValidate from '../../utils/WxValidate.js'
const common = require("../../utils/util")
const app = getApp();

Page({

  /**
   * 页面的初始数据
   */
  data: {
    array: [],
    id: null,
    mobile: null,
    imgUrl: '/images/default-avatar.png'
  },
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    var that = this;
    that.getDepartment();
    that.initValidate();  //验证规则函数
    var url = wx.getStorageSync('imgUrl');
    if(url){
      that.setData({
        imgUrl:url
      })
    }
    // // 页面传值
    // that.setData({
    //   imgUrl:options.imgUrl
    // })
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },
  //验证表单输入打印报错信息
  showModal(error) {
    wx.showModal({
        content: error.msg,
        showCancel: false
    })
  },
  //验证函数
  initValidate(){
    const rules = {
      name: {
        required: true
      },
      mobile: {
        required: true,
        tel:true
      }
    }
    const messages = {
      name: {
        required: '请填写姓名'
      },
      mobile: {
        required:'请填写手机号',
        tel:'请填写正确的手机号'
      }
    }
    this.WxValidate = new WxValidate(rules,messages)
  },
  //得到部门信息
  getDepartment: function () {
    var that = this;
    wx.request({
      url: app.globalData.web_path + '/bc/'+app.globalData.appId+'/BcUserDepartment/getBcUserDepartmentList',
      success: function (res) {
        var list = res.data.data;
        that.setData({
          array: list
        })
      }
    })
  },
  //得到输入的手机号
  getPhone: function (e) {
    this.setData({
      mobile: e.detail.value
    })
  },
  //选择部门
  bindPickerChange: function (e) {
    var that = this;
    //console.log('picker发送选择改变，携带值为', e.detail.value)
    that.setData({
      id: e.detail.value
    })
  },
  //提交表单
  formSubmit: function (e) {
    var that = this
    var userDepartmentName = e.detail.value.userDepartmentName
    //console.log('form发生了submit事件，携带的数据为：',e.detail.value)
    const params = e.detail.value
      //校验表单
      if(!that.WxValidate.checkForm(params)){
        const error = that.WxValidate.errorList[0]
          that.showModal(error)
          return false
      }
      if (!userDepartmentName){
        wx.showModal({
          content: '请选择部门信息',
          showCancel: false  //去掉取消按钮
        });
      }else{
          // 单位内部饭堂无需短信验证, 直接注册
          wx.showLoading({
              title: '注册中',
              mask:true
          });
          wx.setStorageSync('userDepartmentName', userDepartmentName);
          wx.request({
              url: app.globalData.web_path + '/bc/'+app.globalData.appId+'/BcUser/save',
              data: {
                  name: e.detail.value.name,
                  userDepartmentId: e.detail.value.userDepartmentId,
                  mobile: e.detail.value.mobile,
                  openid: wx.getStorageSync('openid')
              },
              header: app.globalData.header,
              success: function (res) {
                  wx.setStorageSync('nickName', e.detail.value.name);
                  //将Token 放入缓存中
                  if(res.data.data && res.data.data.Token){
                      wx.setStorageSync('Token', res.data.data.Token);
                  }
                  wx.hideLoading();
                  wx.navigateBack({
                      url: '/pages/index/index'
                  })
              },
              fail: function (res) {
                  wx.hideLoading();
              }
          })
      }
  }
})