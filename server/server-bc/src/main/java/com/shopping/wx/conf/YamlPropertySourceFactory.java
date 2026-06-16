package com.shopping.wx.conf;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * 让 @PropertySource 能够解析 YAML 文件 (Spring 默认只支持 .properties)。
 * 用于加载 wx.yml; 加载后由 Spring Boot 默认占位符解析器解析其中的 ${ENV:默认值},
 * 因此环境变量(WX_APPID 等)可以正常注入。
 */
public class YamlPropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(@Nullable String name, EncodedResource resource)
            throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        String sourceName = name != null ? name : resource.getResource().getFilename();
        List<PropertySource<?>> sources = loader.load(sourceName, resource.getResource());
        return sources.isEmpty() ? null : sources.get(0);
    }
}
