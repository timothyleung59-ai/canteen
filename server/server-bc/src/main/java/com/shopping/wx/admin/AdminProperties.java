package com.shopping.wx.admin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 后台管理配置
 * 单位饭堂只需要一个管理员账号即可。生产部署务必通过 -D 参数覆盖默认密码。
 */
@Data
@Component
@ConfigurationProperties(prefix = "admin")
public class AdminProperties {

    /** 后台管理员登录账号 */
    private String username = "admin";

    /** 后台管理员登录密码 (公网部署务必修改) */
    private String password = "admin123";

    /** 本单位绑定的小程序 appid; 后台所有业务接口都使用这个值 */
    private String appid = "";

    /** 单位名称, 显示在后台标题栏 */
    private String unitName = "饭堂报餐管理后台";

    /** 登录态有效时长(秒), 默认 12 小时 */
    private long tokenTtl = 12 * 60 * 60;
}
