package com.shopping.wx.service.bc;

import com.shopping.base.domain.bc.BcConfig;
import com.shopping.base.domain.bc.BcHoliday;
import com.shopping.base.repository.bc.BcHolidayRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一判定某天是否开餐(是否可报餐/可预订)。
 * <p>
 * 优先级(从高到低):
 * 1. 报餐设置里手动填的"补班开餐日" openDates  -> 强制开餐
 * 2. 报餐设置里手动填的"停餐日期"   closedDates -> 强制不开餐
 * 3. 自动同步的法定节假日 bc_holiday: isOffDay=true(法定放假)->不开餐; isOffDay=false(调休上班)->开餐
 * 4. 周末规则 saturdayCanDiner / sundayCanDiner
 * 5. 默认开餐(普通工作日)
 * <p>
 * 小程序端报餐/预订(含服务端强校验)与 Web 后台展示都复用这一份逻辑, 避免前后端判定口径不一致。
 */
@Service
public class HolidayJudgeService {

    /** 给前端计算"未来可预订窗口"内的停餐日期列表, 覆盖报本周/报本月场景即可 */
    private static final int RESOLVE_RANGE_DAYS = 60;

    @Autowired
    private BcHolidayRepository bcHolidayRepository;

    private static final SimpleDateFormat YMD = new SimpleDateFormat("yyyy-MM-dd");

    public boolean isOpenDay(Date date, BcConfig config) {
        return isOpenDay(YMD.format(date), config);
    }

    public boolean isOpenDay(String dateStr, BcConfig config) {
        Set<String> manualOpen = parseDates(config == null ? null : config.getOpenDates());
        if (manualOpen.contains(dateStr)) {
            return true;
        }
        Set<String> manualClosed = parseDates(config == null ? null : config.getClosedDates());
        if (manualClosed.contains(dateStr)) {
            return false;
        }
        BcHoliday holiday = bcHolidayRepository.findByHolidayDate(dateStr);
        if (holiday != null) {
            return !holiday.isOffDay();
        }
        int dow = dayOfWeek(dateStr);
        boolean saturdayCanDiner = config != null && config.isSaturdayCanDiner();
        boolean sundayCanDiner = config != null && config.isSundayCanDiner();
        if (dow == Calendar.SATURDAY && !saturdayCanDiner) {
            return false;
        }
        if (dow == Calendar.SUNDAY && !sundayCanDiner) {
            return false;
        }
        return true;
    }

    /**
     * 计算从今天起未来 RESOLVE_RANGE_DAYS 天内, 最终"不开餐"的日期列表, 供小程序/后台直接使用。
     */
    public List<String> resolveClosedDates(BcConfig config) {
        List<String> closed = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        for (int i = 0; i <= RESOLVE_RANGE_DAYS; i++) {
            String d = YMD.format(cal.getTime());
            if (!isOpenDay(d, config)) {
                closed.add(d);
            }
            cal.add(Calendar.DATE, 1);
        }
        return closed;
    }

    private int dayOfWeek(String dateStr) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(YMD.parse(dateStr));
            return cal.get(Calendar.DAY_OF_WEEK);
        } catch (Exception e) {
            return -1;
        }
    }

    private Set<String> parseDates(String raw) {
        Set<String> set = new HashSet<>();
        if (raw == null || raw.trim().isEmpty()) {
            return set;
        }
        for (String d : raw.split("[\\s,，;；、]+")) {
            d = d.trim();
            if (!d.isEmpty()) {
                set.add(d);
            }
        }
        return set;
    }
}
