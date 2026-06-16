package com.shopping.wx.token.config;

/**
 * 常量
 * @author ScienJus
 */
public class Constants {

    /**
     * JWT_SECERT : 密匙
     * 优先读环境变量 JWT_SECRET；未配置时回退到内置开发密钥（仅供本机开发，生产务必注入 JWT_SECRET）。
     */
    public static final String JWT_SECERT = resolveJwtSecret();

    private static String resolveJwtSecret() {
        String env = System.getenv("JWT_SECRET");
        if (env != null && env.trim().length() >= 16) {
            return env.trim();
        }
        System.err.println("[WARN] 未配置 JWT_SECRET 环境变量(或长度<16)，回退到内置开发密钥；"
                + "生产环境务必设置 JWT_SECRET(建议>=32字符随机串)，否则任何拿到源码的人都可伪造管理员 token！");
        return "8677df7fc3a34e26a61c034d5ec8245d";
    }

    /**
     * JWT_TTL : token有效时间
     */
    public static final long JWT_TTL = -1;

    /**
     * 签发者
     */
    public static final String JWT_ISSUER = "江西红人网科技有限公司";

}
