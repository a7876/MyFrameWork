package edu.gdut.MF.annotation;

import edu.gdut.MF.Enum.EnhanceType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnhanceDefinition {
    // 用于声明aop的目标 只支持方法级别增强
    // 该bean不支持依赖注入，只支持无参构造注入，但是可以在增强方法上使用注入
    EnhanceType type();
    String targetName();
    String targetMethod();
    Class<?>[] parameters();
}
