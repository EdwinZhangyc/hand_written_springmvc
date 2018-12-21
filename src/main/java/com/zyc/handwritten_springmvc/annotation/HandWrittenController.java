package com.zyc.handwritten_springmvc.annotation;

import java.lang.annotation.*;

/**
 * @author zyc
 * @date 2018/12/21
 * @description  controller annotation
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HandWrittenController {

    String value() default "";}
