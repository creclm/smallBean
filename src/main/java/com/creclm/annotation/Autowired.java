package com.creclm.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Autowired {
    /**
     *      Spring 的 IOC属性注入的方案，在类变量中注入这个注解，可以从Bean对象中获取Bean对象，而不用手动创造对象
     *
     *      value默认的话，获取的对象依赖类名别名：首字母小写来注入属性，否则依赖value的别名来获取，
     *      依赖指定别名注意保持类型一致，否则将报错，不存在也报错，不会注入为null
     * @return
     */
    String value() default "";
}
