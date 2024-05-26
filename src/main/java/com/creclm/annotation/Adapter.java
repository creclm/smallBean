package com.creclm.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *          依赖注入适配器注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Adapter {

    /**
     *  提供依赖注入的适配器 Bean的别名 （通过不同类别的Service管理其Providers）
     *  如 SPI适配器（我们自己的适配器） Spring适配器
     *
     */
    String[] value() default "";
}
