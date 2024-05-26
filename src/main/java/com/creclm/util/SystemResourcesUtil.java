package com.creclm.util;

/**
 *      系统资源工具
 */
public class SystemResourcesUtil {

    // 获取类加载器 AppClassLoader
    public static ClassLoader getClassLoader(Class<?> clazz){
        return clazz.getClassLoader();
    }


}
