package edu.gdut.MF.core;

import edu.gdut.MF.Enum.BeanScope;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class BeanDefinition {
    String beanName;
    Class<?> beanClass;
    Object beanInstance;
    boolean priority;
    String factoryName; // 这是Config对象的bean名
    Method factoryMethod;
    BeanScope scope;
    Constructor<?> constructor;
    Object tmpInstance; // 用于暂存被处理的对象以免影响原实例的注入
    Integer order;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    public Object getBeanInstance() {
        return beanInstance;
    }

    public void setBeanInstance(Object beanInstance) {
        this.beanInstance = beanInstance;
    }

    public boolean isPriority() {
        return priority;
    }

    public void setPriority(boolean priority) {
        this.priority = priority;
    }
}
