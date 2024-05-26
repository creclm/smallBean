package com.creclm.adapter.impl;

import com.creclm.service.InjectionServiceFactory;

/**
 *      spring 依赖注入适配器的管理工厂
 *
 */
public class SpringInjectionFactory implements InjectionServiceFactory {

    public <T> T getBeanInstance(Class<T> attributes, String name) {
        return null;
    }
}
