package com.zyc.handwritten_springmvc.annotation;

import java.lang.annotation.*;

/**
 * @author zyc
 * @date 2018/12/21
 * @description autowired annotation
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWrittenAutowired {

    String value() default "";
}
