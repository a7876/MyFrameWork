package edu.gdut.MF.annotation;

import edu.gdut.MF.Enum.InjectionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.METHOD,ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    // 指示字段属性和方法注入，构造方法构造，指示工厂方法按名还是按类类别注入参数
    InjectionType value() default InjectionType.INJECTBYNAME; // 注入的方式，不同的注入方式并不兼容
    //方法中按名注入是指类名变换之后注入，不是指参数名（根据编译器选项,参数名可能不存在局部变量表里）
    String name() default ""; // 只有在通过名字注入的时候会使用该属性来指定bean（注解在方法上时无效）
}
