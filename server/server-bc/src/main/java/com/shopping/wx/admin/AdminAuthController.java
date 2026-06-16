package com.shopping.wx.admin;

import com.shopping.base.foundation.result.ActionResult;
import com.shopping.wx.token.authorization.manager.JwtTokenUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 后台管理员登录 / 鉴权入口。
 * 登录成功后签发 JWT, 前端后续请求在请求头 Admin-Token 中携带。
 */
@Log4j2
@RestController
@RequestMapping("/admin")
public class AdminAuthController {

    @Autowired
    private AdminProperties adminProperties;

    @PostMapping("/login")
    public ActionResult login(String username, String password) {
        try {
            if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
                return ActionResult.error("请输入账号和密码");
            }
            if (!username.equals(adminProperties.getUsername())
                    || !password.equals(adminProperties.getPassword())) {
                return ActionResult.error("账号或密码错误");
            }
            String token = JwtTokenUtils.createJWT(username, "admin", adminProperties.getTokenTtl());
            Map<String, Object> map = new HashMap<>(4);
            map.put("token", token);
            map.put("username", username);
            map.put("appid", adminProperties.getAppid());
            map.put("unitName", adminProperties.getUnitName());
            return ActionResult.ok(map);
        } catch (Exception e) {
            log.error("管理员登录异常", e);
        }
        return ActionResult.error("服务器异常");
    }

    @GetMapping("/info")
    public ActionResult info() {
        Map<String, Object> map = new HashMap<>(4);
        map.put("username", adminProperties.getUsername());
        map.put("appid", adminProperties.getAppid());
        map.put("unitName", adminProperties.getUnitName());
        return ActionResult.ok(map);
    }

    @PostMapping("/logout")
    public ActionResult logout() {
        return ActionResult.ok();
    }
}
