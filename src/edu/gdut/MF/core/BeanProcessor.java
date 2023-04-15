package edu.gdut.MF.core;

public interface BeanProcessor {
    Object operateOnBeanBeforeInitialization(Object bean, String beanName);
    // 注入前生效，显然对于工厂方法和构造方法的生成是不能做到一创建空对象就执行本方法
}
