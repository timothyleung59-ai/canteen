package com.shopping.base.repository.bc;

import com.shopping.base.domain.bc.BcHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BcHolidayRepository extends JpaRepository<BcHoliday, Long> {

    BcHoliday findByHolidayDate(String holidayDate);

    List<BcHoliday> findByYear(Integer year);

    /**
     * 查询某个日期范围内的所有节假日/调休记录(闭区间, 字符串按 yyyy-MM-dd 字典序比较即可)
     */
    List<BcHoliday> findByHolidayDateBetween(String from, String to);
}
