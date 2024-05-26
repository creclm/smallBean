package com.creclm.util;

import com.creclm.adapter.InjectionAdapter;
import com.creclm.annotation.Autowired;
import com.creclm.annotation.DisableInject;
import com.creclm.extension.cache.BeanCacheFactory;
import com.creclm.extension.loader.ServiceLoaderFactory;
import com.creclm.service.InjectionServiceFactory;

import java.lang.reflect.*;

/**
 * 依赖注入工具包
 */
public class InjectionUtil {

    /**
     * 初始化依赖注入
     */
    public static void initInjection() {
        ServiceLoaderFactory<InjectionServiceFactory> factory =
                ServiceLoaderFactory.providersLoader(InjectionServiceFactory.class);
        InjectionAdapter adapter = (InjectionAdapter)
                factory.loadInjectionInstance();
        factory.getInjectionProviderFactory().set(adapter);
    }

    /**
     * 依赖注入
     */
    public static Object injection(Object instance) {
        InjectionServiceFactory factory = (InjectionServiceFactory)
                BeanCacheFactory.getCacheInjectionAdapterInstance().get();
        // 注入工厂不用注入属性（设计如此）
        if (factory != null) {
            setterInjection(instance, factory);
            // Autowried注入
            autowriedInjection(instance, factory);
        }
        return instance;
    }

    /**
     *      通过autowired注解注入
     * @param instance
     * @param factory
     * @return
     */
    private static void autowriedInjection(Object instance, InjectionServiceFactory factory) {
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class) && !field.isAccessible()) {
                // 存在 Autowired注解和未初始化
                if (ServiceLoaderFactory.isWrapperClass(field.getType())) {
                    try {
                        // 是包装类， 就重新创建一个
                        Object object1 = field.getType().newInstance();
                        Method set = null;
                        Method[] mds = object1.getClass().getDeclaredMethods();
                        for (Method md : mds) {
                            if (md.getName().equals("set")) {
                                set = md;
                            }
                        }
                        Type fieldType = field.getGenericType();
                        ParameterizedType parameterizedType = (ParameterizedType) fieldType;
                        Type[] typeArguments = parameterizedType.getActualTypeArguments();
                        Class<?> aclass = (Class<?>) typeArguments[0];
                        String paramName = aclass.getSimpleName().substring(0, 1).toLowerCase() + aclass.getSimpleName().substring(1);
                        Object object2 = factory.getBeanInstance(aclass, paramName);
                        if (object2 != null && set != null) {
                            // 先注入包装类，再注入内部
                            set.invoke(object1, object2);
                            field.setAccessible(true);
                            field.set(instance, object1);
                            continue;
                        }
                    } catch (Throwable e) {
                    }
                }else {
                    Class<?> type = field.getType();
                    String paramName;
                    if (field.getAnnotation(Autowired.class).value().equals("")) {
                        paramName = type.getName().substring(0, 1).toLowerCase()
                                + type.getName().substring(1);
                    } else {
                        paramName = field.getAnnotation(Autowired.class).value();
                    }

                    Object object = factory.getBeanInstance(type, paramName);
                    if (object != null) {
                        field.setAccessible(true);
                        try {
                            field.set(instance, object);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     *      通过setter方法进行注入
     * @param instance
     * @param factory
     * @return
     */
    private static void setterInjection(Object instance, InjectionServiceFactory factory) {
        // setter注入
        Method[] methods = instance.getClass().getMethods();
        for (Method method : methods) {
            // 方法以set开头、长度大于3、修饰符是公共的、不存在 DisableInject注解
            if (method.getName().startsWith("set") && method.getName().length() > 3
                    && method.getParameterTypes().length == 1
                    && Modifier.isPublic(method.getModifiers())
                    && !method.isAnnotationPresent(DisableInject.class)) {
                String paramName = null;
                try {
                    // 提取方法对应参数的属性名和类型
                    paramName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
                    Class<?> type = method.getParameterTypes()[0];
                    if (ServiceLoaderFactory.isWrapperClass(type)) {
                        Object object1 = type.newInstance();  // new 一个包装类对象
                        Method set = null;
                        Method[] mds = type.getDeclaredMethods();
                        for (Method md : mds) {
                            if (md.getName().equals("set")) {
                                set = md;
                            }
                        }
                        // 这里type虽然是包装Class对象，但是 getBeanInstance 是先通过别名获取对象的
                        Object object2 = factory.getBeanInstance(type, paramName);
                        if (object2 != null && set != null) {
                            // 先注入包装类，再注入内部
                            set.invoke(object1, object2);
                            method.invoke(instance, object1);
                            continue;
                        }
                    } else {
                        Object object = factory.getBeanInstance(type, paramName);
                        if (object != null) {
                            method.invoke(instance, object);
                        }
                    }
                } catch (Throwable e) {
                }
            }
        }
    }
}
