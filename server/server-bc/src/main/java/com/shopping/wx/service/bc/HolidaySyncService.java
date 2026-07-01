package com.shopping.wx.service.bc;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.shopping.base.domain.bc.BcHoliday;
import com.shopping.base.repository.bc.BcHolidayRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Calendar;
import java.util.Date;

/**
 * 中国法定节假日自动同步。
 * <p>
 * 数据源: https://github.com/NateScarlet/holiday-cn (社区在国务院公布节假日安排后 1-2 天内更新)。
 * 每季度定时(1/1、4/1、7/1、10/1) + 启动时同步"今年"和"明年"两个年份, 写入 bc_holiday 表
 * (全局共享, 不分 appId)。节假日安排本身极少临时变动, 无需每天同步; 管理员在后台有
 * "立即同步"按钮可随时手动触发, 不必等到下个季度。
 * 网络失败只记日志, 不影响系统启动/运行——旧缓存数据 + 管理员手动覆盖仍然可用。
 */
@Log4j2
@Service
public class HolidaySyncService {

    /** jsdelivr 是主源(国内可访问更稳定); raw.githubusercontent 作为兜底 */
    private static final String[] SOURCES = new String[]{
            "https://cdn.jsdelivr.net/gh/NateScarlet/holiday-cn@master/%d.json",
            "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json"
    };

    @Autowired
    private BcHolidayRepository bcHolidayRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 每季度第一天(1/1、4/1、7/1、10/1)凌晨 2:30 自动同步一次
     */
    @Scheduled(cron = "0 30 2 1 1,4,7,10 ?")
    public void scheduledSync() {
        syncCurrentAndNextYear();
    }

    /**
     * 同步"今年"和"明年"两个年份的节假日数据
     */
    public void syncCurrentAndNextYear() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        syncYear(year);
        syncYear(year + 1);
    }

    /**
     * 同步指定年份。任一数据源成功即返回; 全部失败则记日志、跳过(不影响系统运行)。
     */
    public int syncYear(int year) {
        for (String pattern : SOURCES) {
            String url = String.format(pattern, year);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (canteen-holiday-sync)");
                ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
                if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                    int count = savePayload(year, resp.getBody());
                    log.info("节假日同步成功: year=" + year + " source=" + url + " days=" + count);
                    return count;
                }
            } catch (Exception e) {
                log.warn("节假日同步失败, 尝试下一数据源: year=" + year + " source=" + url + " err=" + e.getMessage());
            }
        }
        log.error("节假日同步失败(全部数据源均不可用): year=" + year);
        return 0;
    }

    private int savePayload(int year, String body) {
        JSONObject json = JSONObject.parseObject(body);
        JSONArray days = json.getJSONArray("days");
        if (days == null) {
            return 0;
        }
        Date now = new Date();
        int count = 0;
        for (int i = 0; i < days.size(); i++) {
            JSONObject day = days.getJSONObject(i);
            String date = day.getString("date");
            String name = day.getString("name");
            boolean isOffDay = day.getBooleanValue("isOffDay");
            if (date == null) {
                continue;
            }
            BcHoliday holiday = bcHolidayRepository.findByHolidayDate(date);
            if (holiday == null) {
                holiday = new BcHoliday();
                holiday.setHolidayDate(date);
            }
            holiday.setYear(year);
            holiday.setName(name);
            holiday.setOffDay(isOffDay);
            holiday.setUpdateTime(now);
            bcHolidayRepository.save(holiday);
            count++;
        }
        return count;
    }
}
