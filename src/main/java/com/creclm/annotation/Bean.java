package com.creclm.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bean {

    /**
     *      在类上添加这个Bean注解，就会被扫描添加进入Bean工厂进行Bean对象管理，实现IOC控制反转
     *
     *      value默认不写的情况下，别名规则： 小写类名的第一个字母，指定别名则通过别名注册Bean对象
     *      若指定了别名，可以存在多个相同类名的Bean对象
     *
     *      注意：这个Bean对象的别名有别于SPI服务别名，这里只依赖类名，不使用全类名管理，因此在多个不同
     *      路径下定义相同类名需要指定Bean注解的value值，否则产生冲突报错
     * @return
     */
    String value() default "";

}
