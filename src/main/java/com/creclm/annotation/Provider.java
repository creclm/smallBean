package com.creclm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Provider {
    /**
     *  注入Bean的顺序：
     *      1、通过在provider上标记这个注解，同时标注 value值不空
     *      这样依赖注入的别名优先以这个缓存（存在同名依赖Bean则报错）
     *
     *      2、若provider不标记这个注解，则使用配置文件的别名体系来保存Bean的别名
     *      （存在同名依赖Bean则报错）
     */
    String value() default "";
}
