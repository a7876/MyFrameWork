package edu.gdut.MF.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Order {
    int value() default 0;
    // 越大越优先 只在构建类BeanProcessor上有效
}
