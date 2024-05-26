package com.creclm.adapter;

import com.creclm.annotation.Adapter;
import com.creclm.service.InjectionServiceFactory;

import java.util.ArrayList;
import java.util.List;

/**
 *      依赖注入适配器的管理工厂
 *
 */
@Adapter
public class InjectionAdapter implements InjectionServiceFactory {

    private List<InjectionServiceFactory> impls = new ArrayList<InjectionServiceFactory>();

    public List<InjectionServiceFactory> getImpls() {
        return impls;
    }

    // 依次通过提供的 依赖注入具体实现来实现注入逻辑
    public <T> T getBeanInstance(Class<T> attributes, String name) {
        T beanInstance;
        for (InjectionServiceFactory impl : impls) {
            beanInstance = impl.getBeanInstance(attributes, name);
            if(beanInstance != null){
                return beanInstance;
            }
        }
        // 没有 查询到注入的实例化对象
        return null;
    }
}
