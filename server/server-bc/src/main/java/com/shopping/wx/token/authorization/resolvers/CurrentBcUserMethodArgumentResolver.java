package com.shopping.wx.token.authorization.resolvers;


import com.shopping.base.domain.bc.BcUser;
import com.shopping.base.utils.CommUtils;
import com.shopping.wx.service.bc.BcUserService;
import com.shopping.wx.token.authorization.BcUnauthorizedException;
import com.shopping.wx.token.authorization.annotation.CurrentBcUser;
import com.shopping.wx.token.authorization.manager.JwtTokenUtils;
import com.shopping.wx.token.model.CheckResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 *
 */

@Component
public class CurrentBcUserMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Autowired
    private BcUserService bcUserService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (parameter.hasParameterAnnotation(CurrentBcUser.class)) {
            return true;
        }
        return false;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        String authHeader = webRequest.getHeader("Token");
        if (StringUtils.isNotBlank(authHeader)) {
            CheckResult checkResult = JwtTokenUtils.validateJWT(authHeader);
            if (checkResult.isSuccess()) {
                String key = checkResult.getClaims().getId();
                BcUser bcUser = this.bcUserService.getObjById(CommUtils.null2Long(key));
                if (bcUser != null) {
                    return bcUser;
                }
            }
        }
        // L3: token 缺失/失效/用户不存在时抛鉴权异常，由 GlobalExceptionAdvice 统一返回 401，
        // 避免下游 controller 拿到 null 后 bcUser.getId() 触发 NPE 被吞成"服务器异常"
        throw new BcUnauthorizedException("未登录或登录已过期");
    }
}
