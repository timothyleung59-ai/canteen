const formatTime = date => {
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hour = date.getHours()
  const minute = date.getMinutes()
  const second = date.getSeconds()

  return [year, month, day].map(formatNumber).join('/') + ' ' + [hour, minute, second].map(formatNumber).join(':')
}

const formatNumber = n => {
  n = n.toString()
  return n[1] ? n : '0' + n
}
/**
 * 判断日期为星期几
 * @param date
 * @returns {string}
 */
let getMyDay = (date) =>{
    let week;
    if (date.getDay() === 0) {
        week = "周日"
    } else if (date.getDay() === 1) {
        week = "周一"
    } else if (date.getDay() === 2) {
        week = "周二"
    } else if (date.getDay() === 3) {
        week = "周三"
    } else if (date.getDay() === 4) {
        week = "周四"
    } else if (date.getDay() === 5) {
        week = "周五"
    } else {
        week = "周六"
    }
    return week;
}

/**
 * 打印日志信息
 * @param name
 * @param log
 */
let log = (name,log) =>{
   console.log(name,log);
}

/**
 * 弹出提示框
 * @param content
 * @param showCancel
 */
let showModel = (content) =>{
    return wx.showModal({
        content: content,
        showCancel: false
    });
}
/**
 * 长按弹出提示框
 * @param title
 * @param content
 * @param confirmText
 * @param cancelText
 */
let showConfirm = (title,content,confirmText = '撤销',cancelText = "取消") =>{
    return new Promise((resolve)=> {
        wx.showModal({
            title: title,
            content: content,
            confirmText, cancelText,
            success(res) {
                if (res.confirm) {
                    resolve();
                }
            }
        })
    })
}
/** 补零 */
let pad2 = (n) => (n < 10 ? '0' + n : '' + n);
/** Date -> 'yyyy-MM-dd' */
let fmtYmd = (date) => date.getFullYear() + '-' + pad2(date.getMonth() + 1) + '-' + pad2(date.getDate());
/** 把 "2026-10-01\n2026-10-02,..." 解析成 {日期:true} 集合 */
let parseDateSet = (str) => {
    let set = {};
    if (str) {
        String(str).split(/[\s,，;；、]+/).forEach((d) => {
            d = (d || '').trim();
            if (d) set[d] = true;
        });
    }
    return set;
};
/**
 * 判断某天是否"不开餐"(停餐)。优先级: 补班开餐日 > 停餐日 > 周末规则
 * @param date Date对象
 * @param cfg  {closedDates, openDates, saturdayCanDiner, sundayCanDiner}
 */
let isClosedDay = (date, cfg) => {
    cfg = cfg || {};
    let ymd = fmtYmd(date);
    if (parseDateSet(cfg.openDates)[ymd]) return false;   // 补班开餐日 -> 开
    if (parseDateSet(cfg.closedDates)[ymd]) return true;  // 停餐日 -> 关
    let dow = date.getDay();
    if (dow === 6 && !cfg.saturdayCanDiner) return true;  // 周六不开
    if (dow === 0 && !cfg.sundayCanDiner) return true;    // 周日不开
    return false;
};

module.exports = {
  formatTime: formatTime,
    getMyDay: getMyDay,
    log: log,
    showModel: showModel,
    showConfirm: showConfirm,
    pad2: pad2,
    fmtYmd: fmtYmd,
    parseDateSet: parseDateSet,
    isClosedDay: isClosedDay,
}
