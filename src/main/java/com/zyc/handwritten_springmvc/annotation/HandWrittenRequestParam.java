package com.zyc.handwritten_springmvc.annotation;

import java.lang.annotation.*;

/**
 * @author zyc
 * @date 2018/12/21
 * @description requestParam annotation
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWrittenRequestParam {

    String value() default "";
}
