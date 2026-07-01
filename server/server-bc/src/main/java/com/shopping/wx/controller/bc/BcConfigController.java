package com.shopping.wx.controller.bc;

import com.shopping.base.domain.bc.BcConfig;
import com.shopping.base.domain.bc.BcHoliday;
import com.shopping.base.foundation.result.ActionResult;
import com.shopping.base.foundation.view.BeanViewUtils;
import com.shopping.base.foundation.view.bc.BcConfigView;
import com.shopping.base.repository.bc.BcHolidayRepository;
import com.shopping.wx.service.bc.BcConfigService;
import com.shopping.wx.service.bc.HolidayJudgeService;
import com.shopping.wx.service.bc.HolidaySyncService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Calendar;
import java.util.List;

@Log4j2
@RestController
@RequestMapping("/bc/{appid}/config")
public class BcConfigController  {

    @Autowired
    private BcConfigService bcConfigService;
    @Autowired
    private HolidayJudgeService holidayJudgeService;
    @Autowired
    private HolidaySyncService holidaySyncService;
    @Autowired
    private BcHolidayRepository bcHolidayRepository;


    /**
     *获取配置
     * @return
     */
    @GetMapping("/getConfig")
    public ActionResult getConfig(@PathVariable String appid){
        try{
            BcConfig cfg= this.bcConfigService.getConfigByAppId(appid);
            BcConfigView bcConfigView = BeanViewUtils.getView(cfg,BcConfigView.class);
            bcConfigView.setResolvedClosedDates(this.holidayJudgeService.resolveClosedDates(cfg));
            return  ActionResult.ok(bcConfigView);
        }catch (Exception e){
            log.error("获取截止时间异常",e);
        }
        return ActionResult.error("服务器异常");
    }

    /**
     * 保存或者更新配置
     * @param appid
     * @param config
     * @return
     */
    @PostMapping("/saveOrUpdate")
    public ActionResult saveOrUpdate(@PathVariable String appid,BcConfig config,String imgUrlList){
        try{
            return this.bcConfigService.saveOrUpdate(appid,config,imgUrlList);
        }catch (Exception e){
            log.error("保存或者更新配置失败!",e);
        }
        return ActionResult.error("服务器异常");
    }

    /**
     * 查看自动同步的法定节假日列表(只读, 后台展示用)
     */
    @GetMapping("/holidays")
    public ActionResult holidays(@PathVariable String appid, Integer year){
        try{
            int y = year != null ? year : Calendar.getInstance().get(Calendar.YEAR);
            List<BcHoliday> list = this.bcHolidayRepository.findByYear(y);
            return ActionResult.ok(list);
        }catch (Exception e){
            log.error("查询节假日列表异常",e);
        }
        return ActionResult.error("服务器异常");
    }

    /**
     * 手动触发一次节假日同步(节假日全局共享, 与 appid 无关, 挂在此路径下仅为保持前端调用一致)
     */
    @PostMapping("/syncHolidays")
    public ActionResult syncHolidays(@PathVariable String appid){
        try{
            this.holidaySyncService.syncCurrentAndNextYear();
            return ActionResult.ok();
        }catch (Exception e){
            log.error("手动同步节假日异常",e);
        }
        return ActionResult.error("同步失败, 请稍后重试");
    }

}
