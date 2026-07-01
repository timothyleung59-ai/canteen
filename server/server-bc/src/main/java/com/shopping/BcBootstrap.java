package com.shopping;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.servlet.MultipartConfigElement;
import java.io.File;

/**
 *
 * EnableAspectJAutoProxy 表示开启AOP代理自动配置
 */
@SpringBootApplication
@EnableConfigurationProperties
@ComponentScan(basePackages={"com.shopping"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableScheduling
public class BcBootstrap {

    public static void main(String[] args) throws Exception {
        // MQ日志配置
        //System.setProperty("rocketmq.client.log.loadconfig","false");
        System.setProperty("ons.client.logFileMaxIndex","3");
        System.setProperty("ons.client.logLevel","Info");
        // MQ异常之springboot环境下相同类进行转换出现ClassCastException异常 https://www.jianshu.com/p/e6d5a3969343
        System.setProperty("spring.devtools.restart.enabled","false");
        // 禁用nacos默认的日志配置,防止覆盖log4j配置
        System.setProperty("nacos.logging.default.config.enabled","false");
        SpringApplication.run(BcBootstrap.class, args);
    }

    /**
     * 上传文件新增临时存放路径
     报错信息： java.io.IOException: java.io.FileNotFoundException:
     /tmp/tomcat.273391201583741210.8080/work/Tomcat/localhost/ROOT/tmp/source/IMG_20160129_132623.jpg (No such file or directory)
     * @return
     */
    @Bean
    MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        String location = System.getProperty("user.dir") + "/data/tmp";
        File file = new File(location);
        if(!file.exists()){
            file.mkdirs();
        }
        factory.setLocation(location);
        return factory.createMultipartConfig();
    }

    /**
     * 启动时同步一次法定节假日数据, 不用等到每天定时任务的凌晨时刻才第一次有数据。
     * 网络失败只记日志, 不阻塞启动(HolidaySyncService 内部已吞异常)。
     */
    @Bean
    public ApplicationRunner syncHolidayOnStartup(com.shopping.wx.service.bc.HolidaySyncService holidaySyncService) {
        return (ApplicationArguments args) -> holidaySyncService.syncCurrentAndNextYear();
    }

    // 说明: 原先这里有一个自定义的 PropertySourcesPlaceholderConfigurer 用来加载 wx.yml,
    // 但它作为 BeanFactoryPostProcessor 实例化过早, 拿不到 Environment, 导致 ${ENV:默认值}
    // 占位符只能用 wx.yml + 默认值解析, 永远读不到环境变量(DB_PASSWORD / WX_APPID 等全部失效)。
    // wx.yml 实际上已由 WxMaProperties 的 @PropertySource("classpath:wx.yml") 正常加载,
    // 故移除该 Bean, 改用 Spring Boot 默认占位符解析器, 环境变量注入即可正常生效。
}
