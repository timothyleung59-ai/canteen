package com.shopping.base.domain.bc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

/**
 * 报餐配置表
 * @author user on 2019/7/1.
 */
@Data
@Table(name = "bc_config")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class BcConfig {
    /** id;id */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    public Long id;

    /** 微信AppId */
    private  String appId;
    /** 用户是否需要审核 */
    private boolean userNeedApprove ;
    /** 周六是否可报餐 */
    private boolean saturdayCanDiner ;
    /** 周日是否可报餐 */
    private boolean sundayCanDiner ;
    /** 午餐报餐时间*/
    private String lunchOrderTime;
    /** 晚餐报餐时间 */
    private String dinnerOrderTime;
    /**  午餐是否可报餐*/
    private boolean lunchCanMeal;
    /** 晚餐是否可报餐 */
    private boolean dinnerCanMeal;
    /** 停餐日期(节假日等不开餐), 多个用逗号/换行分隔, 格式 yyyy-MM-dd */
    @Column(length = 2048)
    private String closedDates;
    /** 补班开餐日(调休上班照常开餐), 同上格式; 优先级高于周末规则和停餐日 */
    @Column(length = 2048)
    private String openDates;
}
