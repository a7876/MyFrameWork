package edu.gdut.MF.core;

public interface BeanProcessor {
    // 处理器类， 处理器类和配置类一样，不能注入（字段，构造器，注入方法，工厂方法）
    Object operateOnBeanAfterInitialization(Object bean, String beanName);
    // 每次执行应该返回一个新的合适对象替代源对象（如代理对象），不该在方法里修改被系统注入的对象的属性。（可能会被覆盖）
    // 返回非空即认为是已经进行修改，返回null证明不修改（跳过）
    // 可以搭配Order注解对每个处理器类进行排序
}
