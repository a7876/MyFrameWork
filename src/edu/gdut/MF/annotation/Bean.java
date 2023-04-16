package edu.gdut.MF.annotation;

import edu.gdut.MF.Enum.BeanScope;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD}) // 如果用在工厂方法上需要用户自行在参数要求所有的注入项目
public @interface Bean {
    String value() default "";
    // 定义bean名称
    boolean priority() default false;
    // 定义优先级
    BeanScope scope() default BeanScope.SINGLETON;
    // 实例化形式, 这里的prototype会保证每一个需要一个对象的时候都会是一个新的对象（也正是这样，prototype会导致新的循环注入的问题）
}
