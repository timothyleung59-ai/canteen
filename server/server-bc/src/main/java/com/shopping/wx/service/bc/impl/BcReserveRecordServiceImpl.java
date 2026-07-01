package com.shopping.wx.service.bc.impl;

import com.shopping.base.domain.bc.BcConfig;
import com.shopping.base.domain.bc.BcRecord;
import com.shopping.base.domain.bc.BcReserveRecord;
import com.shopping.base.foundation.base.service.impl.BaseServiceImpl;
import com.shopping.base.foundation.dao.bc.BcReserveRecordDAO;
import com.shopping.base.foundation.result.ActionResult;
import com.shopping.base.repository.bc.BcRecordRepository;
import com.shopping.base.repository.bc.BcReserveRecordRepository;
import com.shopping.base.utils.CommUtils;
import com.shopping.wx.constant.BcRecordCons;
import com.shopping.wx.form.bc.BcReserveRecordAddForm;
import com.shopping.wx.service.bc.BcConfigService;
import com.shopping.wx.service.bc.BcRecordService;
import com.shopping.wx.service.bc.BcReserveRecordService;
import com.shopping.wx.service.bc.HolidayJudgeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @anthor bin
 * @data 2019/7/9 15:01
 * 类描述：预约报餐
 */
@Log4j2
@Service(value = "BcReserveRecord")
@Transactional(rollbackFor = Exception.class)
public class BcReserveRecordServiceImpl extends BaseServiceImpl<BcReserveRecord,Long> implements BcReserveRecordService{
    @Autowired
    BcReserveRecordDAO bcReserveRecordDAO;
    @Autowired
    BcReserveRecordRepository bcReserveRecordRepository;
    @Autowired
    BcRecordRepository bcRecordRepository;
    @Autowired
    BcConfigService bcConfigService;
    @Autowired
    HolidayJudgeService holidayJudgeService;
    @Autowired
    BcRecordService bcRecordService;

    private static final String[] WEEK_NAMES = {"周日","周一","周二","周三","周四","周五","周六"};

    @Override
    public ActionResult bcReserveRecordSave(String appid,Long id,BcReserveRecordAddForm bcReserveRecordAddForm,int status) throws Exception {
        BcConfig config =bcConfigService.getConfigByAppId(appid);
        Boolean userNeedApprove =config.isUserNeedApprove();
        Boolean lunchCanMeal = config.isLunchCanMeal();
        //如果用户不需要审核就给予状态为1：已激活
        if(!userNeedApprove){
            //中午报餐状态开启
            if(lunchCanMeal){
                return this.save(appid,id,bcReserveRecordAddForm,config);
            }else{
                return ActionResult.error(2,"中午报餐功能未开启");
            }
        }else{
            if(lunchCanMeal){
                if(status == 1){
                    return this.save(appid,id,bcReserveRecordAddForm,config);
                }else{
                    return ActionResult.error(1,"需要联系管理员给予激活");
                }
            }else{
                return ActionResult.error(2,"中午报餐功能未开启");
            }
        }
    }
    public ActionResult save(String appid,Long id,BcReserveRecordAddForm bcReserveRecordAddForm,BcConfig config){
        Date reserveDate = CommUtils.formatDate(bcReserveRecordAddForm.getReserveTime(),"yyyy-MM-dd");
        // L6: 停餐日闸门(法定节假日/手动停餐/周末规则, 服务端强校验, 与小程序端判定口径一致)
        if (!holidayJudgeService.isOpenDay(reserveDate, config)) {
            return ActionResult.error(2, "该日期不开餐(节假日/周末)");
        }
        // 预约直接就是正式报餐记录, 去重统一查 bc_record(不再有单独的预约表去重)
        String reserveDateStr = CommUtils.formatDate(reserveDate,"yyyy-MM-dd");
        if (this.bcRecordRepository.getByUserIdAndDinTime(appid, id, reserveDateStr) != null) {
            return ActionResult.error(3, "您当天已报餐，请勿重复预约");
        }
        BcRecord bcRecord = new BcRecord();
        bcRecord.setAddTime(new Date());
        bcRecord.setAppId(appid);
        bcRecord.setUserId(id);
        bcRecord.setBcType(BcRecordCons.BC_TYPE_NOON);
        bcRecord.setBcChannel(BcRecordCons.BC_CHANNEL_ORDER);
        bcRecord.setDinTime(reserveDate);
        bcRecord.setHadEat(BcRecordCons.HAD_EAT_CANNEL);
        this.bcRecordRepository.save(bcRecord);
        return ActionResult.ok(bcRecord);
    }
    @Override
    public List<Map<String,Object>> getBcReserveRecordList(Long id, String appid) throws Exception {
        List<BcRecord> list = this.bcRecordRepository.findFutureByUser(appid, id, new Date());
        return toReserveViewList(list);
    }

    @Override
    public ActionResult deleteBcReserveRecordById(String appId, Long bcUserId, Long id) throws Exception {
        // 取消预约 = 取消报餐, 复用同一套校验(已就餐/历史日期不可取消), 不重复实现
        return this.bcRecordService.deleteBcRecordById(appId, bcUserId, id);
    }

    /**
     * BcRecord 列表 -> 小程序预订页期望的字段格式(id/reserveTime/reserveTimeWeek), 保持接口路径/
     * 返回字段不变, 前端不用改代码
     */
    private List<Map<String,Object>> toReserveViewList(List<BcRecord> list) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        List<Map<String,Object>> result = new ArrayList<>();
        for (BcRecord r : list) {
            Map<String,Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("reserveTime", sdf.format(r.getDinTime()));
            Calendar cal = Calendar.getInstance();
            cal.setTime(r.getDinTime());
            m.put("reserveTimeWeek", WEEK_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1]);
            result.add(m);
        }
        return result;
    }

    @Override
    public List<BcReserveRecord> getBcReserveRecordListByReserveTime(String appId, String reserveRecordTime) throws Exception {
        //String date = CommUtils.formatDate(reserveRecordTime,"yyyy-MM-dd");
        //return  this.bcReserveRecordRepository.getBcReserveRecordListByReserveTime(appId,CommUtils.formatDate(date,"yyyy-MM-dd"));
        return  this.bcReserveRecordRepository.getBcReserveRecordListByReserveTime(appId,reserveRecordTime);
    }



    @Override
    public ActionResult bctchBcReserveRecordByReserveTime(String appId) throws Exception {
        Date date = new Date();
        String reserveRecordTime = CommUtils.formatDate(date,"yyyy-MM-dd");
        //先获取没有报餐的预约记录
        List<BcReserveRecord> reserveRecordList =  this.getBcReserveRecordListByReserveTime(appId,reserveRecordTime);
        //新增报餐的集合
        List<BcRecord> addList= new ArrayList<>();
        for(int i=0;i<reserveRecordList.size();i++){
            BcReserveRecord reserveRecord= reserveRecordList.get(i);
            BcRecord tranForRecord = new BcRecord();
            tranForRecord.setAddTime(new Date());
            tranForRecord.setAppId(appId);
            tranForRecord.setBcChannel(BcRecordCons.BC_CHANNEL_ORDER);
            tranForRecord.setBcType(BcRecordCons.BC_TYPE_NOON);
            tranForRecord.setUserId(reserveRecord.getBcUserId());
            tranForRecord.setHadEat(BcRecordCons.HAD_EAT_CANNEL);
            Date reserveTime = reserveRecord.getReserveTime();
            String time =CommUtils.formatDate(new Date(),"HH:mm:ss");
            String prefix = CommUtils.formatDate(reserveTime,"yyyy-MM-dd");
            Date dinTime = CommUtils.formatDate((prefix+" "+time),"yyyy-MM-dd HH:mm:ss");
            tranForRecord.setDinTime(dinTime);
            addList.add(tranForRecord);
        }
        //删除预约记录
        this.deleteAllByReserveTime(appId,date);
        //批量增加记录
        this.bcRecordRepository.saveAll(addList);
        return ActionResult.ok("处理成功");
    }

    @Override
    public int deleteAllByReserveTime(String appId,Date reserveTime) throws Exception {
        String date = CommUtils.formatDate(reserveTime,"yyyy-MM-dd");
        return this.bcReserveRecordRepository.deleteAllByReserveTime(CommUtils.formatDate(date,"yyyy-MM-dd"),appId);
    }

    @Override
    public List<Map<String,Object>> getBcReserveRecordByYearAndMonth(String appId,Long bcUserId,String curYearMonth) throws Exception {
        List<BcRecord> list = this.bcRecordRepository.findByUserAndYearMonth(appId, bcUserId, curYearMonth);
        return toReserveViewList(list);
    }

}
