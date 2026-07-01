package com.shopping.base.foundation.view.bc;

import com.shopping.base.domain.bc.BcConfig;
import com.shopping.base.foundation.view.BeanView;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
public class BcConfigView  extends BeanView<BcConfig>  implements Serializable {

    public Long id;

    private String appId;
    /** 用户是否需要审核 */
    private boolean userNeedApprove ;
    /** 周六是否可报餐 */
    private boolean saturdayCanDiner ;
    /** 周日是否可报餐 */
    private boolean sundayCanDiner ;
    /** 午餐报餐时间 */
    private String lunchOrderTime;
    /** 晚餐报餐时间 */
    private String dinnerOrderTime;
    /**  午餐是否可报餐*/
    private boolean lunchCanMeal;
    /** 晚餐是否可报餐 */
    private boolean dinnerCanMeal;
    /** 停餐日期(节假日等不开餐, 手动覆盖), 逗号/换行分隔, yyyy-MM-dd */
    private String closedDates;
    /** 补班开餐日(调休上班, 手动覆盖), 同上 */
    private String openDates;
    /** 未来60天内最终不开餐的日期(手动覆盖+自动节假日+周末规则合并计算结果), 由 Controller 填充, 非数据库字段 */
    private List<String> resolvedClosedDates;
}
