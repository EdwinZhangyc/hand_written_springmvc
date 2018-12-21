package com.zyc.handwritten_springmvc.annotation;

import java.lang.annotation.*;

/**
 * @author zyc
 * @date 2018/12/21
 * @description requestMapping annotation
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWrittenRequestMapping {

    String value() default "";
}