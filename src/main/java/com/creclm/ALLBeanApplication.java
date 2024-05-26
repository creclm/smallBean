package com.creclm;

import com.creclm.annotation.Bean;
import com.creclm.extension.cache.BeanCacheFactory;
import com.creclm.util.InjectionUtil;
import com.creclm.util.PacketClassScanUtil;

import java.util.*;

/**
 *      初始化Bean工厂
 */
public class ALLBeanApplication {

    /**
     * Dubbo启动入口
     */
    public static void run(Class<?> clazz, String[] args) {
        // 初始化依赖注入
        InjectionUtil.initInjection();

        // 包扫描 以及Bean注入、依赖注入
        ClassLoader ourLoader = ALLBeanApplication.class.getClassLoader();
        ClassLoader outerLoader = clazz.getClassLoader();
        String basePacket = ALLBeanApplication.class.getName();
        basePacket = basePacket.substring(0, basePacket.lastIndexOf("."));
        List<Class<?>> ourClasses = new PacketClassScanUtil().scan(ourLoader, basePacket);
        String outerBasePacket = clazz.getName().contains(".") ?
                clazz.getName().substring(0, clazz.getName().lastIndexOf(".")) : "";
        List<Class<?>> allClasses = new PacketClassScanUtil().scan(outerLoader, outerBasePacket);
        List<Class<?>> outerClasses = new ArrayList<>();
        for (Class<?> aClass : allClasses) {
            if(!ourClasses.contains(aClass)){
                outerClasses.add(aClass);
            }
        }
        injectionBean(outerLoader, ourClasses);
        injectionBean(outerLoader, outerClasses);
    }

    private static void injectionBean(ClassLoader classLoader, List<Class<?>> classes) {
        Map<String, Class<?>> beanClasses = new HashMap<>();
        Map<String, Object> beans = new HashMap<>();
        // 查询所有目录下的java文件，存在 .class
        for (Class<?> clazz : classes) {
            // 存在 Bean注解的类
            if (!clazz.isInterface() && !clazz.isEnum()
                    && clazz.isAnnotationPresent(Bean.class)) {
                // 添加进入属性工厂 的Class对象中
                String className = clazz.getName();
                String simpleName = clazz.getName().contains(".") ?
                        clazz.getName().substring(clazz.getName().lastIndexOf(".") + 1) : className;
                try {
                    // 普通的Bean就使用类名首字母小写来实现
                    simpleName = simpleName.substring(0, 1).toLowerCase() + simpleName.substring(1);
                    if (BeanCacheFactory.getLoadedClass(simpleName) != null) {
                        throw new IllegalStateException("存在同名Bean，配置出错了");
                    }
                    BeanCacheFactory.addClassToCacheIfAbsent(simpleName, clazz);
                    beanClasses.put(simpleName, clazz);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        // 将beans都实例化存入BeanUtil工厂
        for (String simpleName : beanClasses.keySet()) {
            try {
                Object bean = beanClasses.get(simpleName).newInstance();
                BeanCacheFactory.addInstanceToCacheIfAbsent(simpleName, bean);
                beans.put(simpleName, bean);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        // 处理依赖注入问题  IOC注入
        for (String simpleName : beans.keySet()) {
            InjectionUtil.injection(beans.get(simpleName));
        }
    }




}
