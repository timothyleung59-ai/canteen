package com.shopping.wx.token.authorization;

/**
 * 小程序登录态(@CurrentBcUser)无效时抛出，由 GlobalExceptionAdvice 统一转成 401 响应。
 */
public class BcUnauthorizedException extends RuntimeException {

    public BcUnauthorizedException(String message) {
        super(message);
    }
}
