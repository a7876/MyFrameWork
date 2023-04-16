package edu.gdut.MF.core;

public interface BeanProcessor {
    enum ProcessorOption{
        DEFAULT,ALLOWTOINJECT;
        // DEFAULT 不支持对处理器bean构建的任何注入
        // ALLOWTOINJECT 允许对处理器bean构建进行注入 但是注入依赖链上的所有bean不能被本处理器处理
    }
    // 处理器类， 处理器类和配置类一样，不能注入（字段，构造器，注入方法，工厂方法）
    Object operateOnBeanAfterInitialization(Object bean, String beanName);
    // 每次执行应该返回一个新的合适对象替代源对象（如代理对象），不该在方法里修改被系统注入的对象的属性。（如果需要按情况注入应该合理设定注入）
    // 返回非空即认为是已经进行修改，返回null证明不修改（跳过）
    // 可以搭配Order注解对每个处理器类进行排序
    // 任何情况下处理器不处理（不作用于）处理器
    // 支持注入（被处理器依赖的bean不接受本处理器的处理）这样会导致这些被依赖的bean（包括依赖链上的）都不会本处理器处理
    
    
    // NEED TO CONSIDER 能否实现弱依赖中在依赖注入之后去处理
}
