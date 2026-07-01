package com.shopping.base.domain.bc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 法定节假日缓存(全局共享, 不分 appId), 由定时任务从开源节假日库自动同步。
 */
@Data
@Table(name = "bc_holiday")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class BcHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    public Long id;

    /** 日期 yyyy-MM-dd */
    @Column(name = "holiday_date", unique = true, nullable = false, length = 10)
    private String holidayDate;

    /** 年份 */
    private Integer year;

    /** 节假日/调休名称, 如"国庆节"、"国庆节前调休" */
    private String name;

    /** true=法定节假日(停餐)  false=调休上班(开餐) */
    @Column(name = "is_off_day")
    private boolean offDay;

    /** 本条最后同步时间 */
    @Column(name = "update_time")
    private java.util.Date updateTime;
}
