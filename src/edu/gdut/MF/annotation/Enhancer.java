package edu.gdut.MF.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Bean
public @interface Enhancer {
    // aop增强定义类  aop只支持JDK基于接口的增强
    // 标记注解En
}
