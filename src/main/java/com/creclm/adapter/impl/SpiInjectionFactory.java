package com.creclm.adapter.impl;

import com.creclm.extension.cache.BeanCacheFactory;
import com.creclm.service.InjectionServiceFactory;

/**
 *      spi 依赖注入适配器的管理工厂
 *
 */
public class SpiInjectionFactory implements InjectionServiceFactory {

    public <T> T getBeanInstance(Class<T> attributes, String name) {
        System.out.println("这是Spi提供的属性注入方法");
        // 这里实现通过Autowired实现注入
        return (T) BeanCacheFactory.getLoadedInstance(name, attributes);
    }
}
