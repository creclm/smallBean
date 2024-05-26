package com.creclm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DisableInject {
    /**
     *      在set方法上配置这个注解，可以跳过IOC属性注入
     *
     *      value：用途未定义
     */
    String value() default "";
}
