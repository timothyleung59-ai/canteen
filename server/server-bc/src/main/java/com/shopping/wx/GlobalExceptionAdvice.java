package com.shopping.wx;

import com.shopping.base.foundation.result.ActionResult;
import com.shopping.wx.token.authorization.BcUnauthorizedException;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * 目前只统一处理小程序登录态失效(@CurrentBcUser 解析失败)，返回 {code:401}，
 * 避免下游 controller 因 bcUser==null 触发 NPE 被吞成"服务器异常"。
 */
@Log4j2
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(BcUnauthorizedException.class)
    public ActionResult handleUnauthorized(BcUnauthorizedException e) {
        return ActionResult.error(401, e.getMessage());
    }
}
