package com.creclm.extension.cache;


import com.creclm.extension.support.Holder;

import java.security.AlgorithmParameterGenerator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能：
 * 1、静态管理所有SPI服务工厂实例化对象 Bean的 的全局缓存
 * 2、以及所有的实例化对象Bean的缓存
 * 3、对外提供查询接口
 * <p>
 * PS: 待实现属性依赖注入功能 ！！！
 */
public class BeanCacheFactory {

    // 全局加载到的Class对象缓存           服务别名#服务提供者别名  -->  包装的Class对象
    private static Map<String, Class<?>> globalCachedNames = new ConcurrentHashMap<String, Class<?>>();
    // 全局加载到的Class对象缓存           Class对象  -->  服务别名#服务提供者别名
    private static Map<Class<?>, String> globalCachedClasses = new ConcurrentHashMap<Class<?>, String>();

    // 全局Bean实例化对象缓存              服务别名#服务提供者别名  --> 实例化对象
    private static Map<String, Object> globalCachedNameInstance = new ConcurrentHashMap<String, Object>();
    // 全局Bean实例化对象缓存              Class对象    -->     实例化对象
    // 注意： 这里一个Class对象只会存在最后一个实例化对象，要查询所有的实例化对象还是用别名查最好
    // 因为ServiceLoaderFactory会覆盖
    private static Map<Class<?>, Object> globalCachedClassInstance = new ConcurrentHashMap<Class<?>, Object>();

    // 缓存依赖注入适配器的 Class包装对象
    private static volatile Holder<Class<?>> cacheInjectionAdapterClass = new Holder<Class<?>>();

    // 缓存依赖注入适配器的实例包装对象
    private static volatile Holder<Object> cacheInjectionAdapterInstance = new Holder<Object>();

    // 获取依赖注入适配器的 Class包装对象
    public static Holder<Class<?>> getCacheInjectionAdapterClass() {
        return cacheInjectionAdapterClass;
    }

    // 获取依赖注入适配器的 实例包装对象
    public static Holder<Object> getCacheInjectionAdapterInstance() {
        return cacheInjectionAdapterInstance;
    }

    /**
     *      不存在就将加载的类Class对象缓存到Bean工厂的全局静态缓存
     * @param name
     * @param clazz
     */
    public static void addClassToCacheIfAbsent(String name, Class<?> clazz) {
        if(!globalCachedNames.containsKey(name) && !globalCachedNames.containsValue(clazz)){
            globalCachedNames.put(name, clazz);
            globalCachedClasses.put(clazz, name);
        }
    }

    /**
     *      globalCachedNameInstance: 将加载的类实例对象缓存到 Bean工厂的全局静态缓存
     *      globalCachedClassInstance: 缓存之前没有加载过实例对象的Class对象和 实例对象
     * @param name
     * @param bean
     */
    public static void addInstanceToCacheIfAbsent(String name, Object bean) {
        if(!globalCachedNameInstance.containsKey(name) && !globalCachedNameInstance.containsValue(bean)){
            globalCachedNameInstance.put(name, bean);
        }
        if(!globalCachedClassInstance.containsKey(bean.getClass())){
            globalCachedClassInstance.put(bean.getClass(), bean);
        }
    }


    // 对外提供通过别名name以及Class对象查询 Bean Class对象是否存在缓存的接口 (不是实例化)
    public static Class<?> getLoadedClass(String name, Class<?> clazz) {
        if (name != null && globalCachedNames.containsKey(name)) {
            return globalCachedNames.get(name);
        } else if (clazz != null && globalCachedClasses.containsKey(clazz)) {
            return clazz;
        } else {
            return null;
        }
    }

    public static Class<?> getLoadedClass(Class<?> clazz) {
        return getLoadedClass(null, clazz);
    }

    public static Class<?> getLoadedClass(String name) {
        return getLoadedClass(name, null);
    }

    // 对外提供通过别名name查询Bean实例化对象
    // 通过clazz查询，只能查询到最新的Class对象的实例化对象，最好使用name查询
    public static Object getLoadedInstance(String name, Class<?> clazz) {
        if (name != null && globalCachedNameInstance.containsKey(name)) {
            return globalCachedNameInstance.get(name);
        } else if (clazz != null && globalCachedClassInstance.containsKey(clazz)) {
            return globalCachedClassInstance.get(clazz);
        } else {
            return null;
        }
    }

    // 获取一个最新的Bean对象（通过clazz）,多次加载不同的这个对象会导致只返回最新的（覆盖问题）
    public static Object getLoadedInstance(Class<?> clazz) {
        return getLoadedInstance(null, clazz);
    }

    public static Object getLoadedInstance(String name) {
        return getLoadedInstance(name, null);
    }

}

