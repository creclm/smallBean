package com.creclm.service;


import com.creclm.annotation.SPI;

/**
 *      依赖注入 的抽象扩展工厂接口（提供Bean依赖注入的服务Service）
 */
@SPI
public interface InjectionServiceFactory {

    /**
     *      获取指定Class和别名的 这个属性 的实例对象，用于依赖注入
     * @param attributes：需要注入的Class对象
     * @param name：目标注入的别名
     * @param <T>： 返回一个需要注入的Class对象的类型
     * @return： 对应实例对象
     */
    <T> T getBeanInstance(Class<T> attributes, String name);

}
