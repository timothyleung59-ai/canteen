package com.shopping.wx.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shopping.wx.token.authorization.manager.JwtTokenUtils;
import com.shopping.wx.token.model.CheckResult;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 后台管理鉴权过滤器。
 * <p>
 * 只拦截"纯管理端"接口(小程序不会调用的那些), 要求请求头携带有效的 Admin-Token。
 * 小程序与后台共用的只读接口(部门列表 / 读配置 / 当天报餐人数 / 确认就餐等)不在拦截范围内,
 * 保持原有开放逻辑, 避免影响小程序。
 */
public class AdminAuthFilter implements Filter {

    /** 需要管理员登录态才能访问的接口路径片段(以下接口小程序均不调用) */
    private static final List<String> PROTECTED = Arrays.asList(
            "/BcUser/getUserPageList",
            "/BcUser/updateStatusById",
            "/BcUser/delete",
            "/BcUser/export",
            "/BcUser/editUserDepartmentId",
            "/BcUser/updateAdminById",
            "/BcUserDepartment/getDepartmentPageList",
            "/BcUserDepartment/save",
            "/BcUserDepartment/updateName",
            "/BcUserDepartment/deleteById",
            "/BcRecord/getBcRecordList",
            "/BcRecord/countBcRecordPageList",
            "/BcRecord/export",
            "/BcRecord/exportCount",
            "/BcBanner/upload",
            "/BcBanner/deleteImg",
            "/bctch",
            "/config/saveOrUpdate",
            "/config/holidays",
            "/config/syncHolidays"
            // 注意: /BcRecord/getBcRecordListByDinTime、/confirmEat、/confirmEatByUser、
            // /getTotalRecordByDinTime 故意不在此列——这几个是小程序管理员查看报餐名单页面调用的
            // 接口, 用的是普通登录用户 Token 头(非 Admin-Token), 权限校验改在各自 controller 方法里
            // 用 @CurrentBcUser 判断 bcUser.isAdmin(), 不走这个基于 Admin-Token 的过滤器。
    );

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest sreq, ServletResponse sresp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) sreq;
        String uri = request.getRequestURI();

        boolean needAuth = uri.startsWith("/admin/") && !uri.equals("/admin/login");
        if (!needAuth) {
            needAuth = matchesProtected(uri);
        }

        if (!needAuth) {
            chain.doFilter(sreq, sresp);
            return;
        }

        String token = request.getHeader("Admin-Token");
        if (StringUtils.isBlank(token)) {
            writeJson(sresp, 401, "未登录或登录已过期");
            return;
        }
        CheckResult result = JwtTokenUtils.validateJWT(token);
        if (result.isSuccess() && result.getClaims() != null
                && "admin".equals(result.getClaims().getSubject())) {
            chain.doFilter(sreq, sresp);
        } else {
            writeJson(sresp, 401, "登录已过期, 请重新登录");
        }
    }

    /**
     * 判断 URI 是否命中受保护接口。
     * 用"边界匹配"而非简单 contains: 片段之后必须是串尾 / '/' / '?',
     * 避免 "/BcRecord/getBcRecordList" 误伤 "/BcRecord/getBcRecordListByDinTime"(小程序共用只读),
     * 同时仍能匹配带路径参数的 "/BcUserDepartment/deleteById/{id}"。
     */
    private boolean matchesProtected(String uri) {
        for (String frag : PROTECTED) {
            int idx = uri.indexOf(frag);
            if (idx < 0) {
                continue;
            }
            int end = idx + frag.length();
            if (end == uri.length()) {
                return true;
            }
            char c = uri.charAt(end);
            if (c == '/' || c == '?') {
                return true;
            }
        }
        return false;
    }

    private void writeJson(ServletResponse resp, int code, String msg) throws IOException {
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(JSON.toJSONString(json));
    }

    @Override
    public void destroy() {
    }
}
